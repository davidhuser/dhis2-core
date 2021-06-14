/*
 * Copyright (c) 2004-2021, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.merge.orgunit;

import java.util.Set;

import javax.transaction.Transactional;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.merge.orgunit.handler.AnalyticalObjectOrgUnitMergeHandler;
import org.hisp.dhis.merge.orgunit.handler.MetadataOrgUnitMergeHandler;
import org.hisp.dhis.merge.orgunit.handler.TrackerOrgUnitMergeHandler;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

/**
 * Main class for org unit merge.
 *
 * @author Lars Helge Overland
 */
@Service
public class DefaultOrgUnitMergeService
    implements OrgUnitMergeService
{
    private final MetadataOrgUnitMergeHandler metadataHandler;

    private final AnalyticalObjectOrgUnitMergeHandler analyticalObjectHandler;

    private final TrackerOrgUnitMergeHandler trackerHandler;

    private final IdentifiableObjectManager idObjectManager;

    private final ImmutableList<OrgUnitMergeHandler> handlers;

    public DefaultOrgUnitMergeService( MetadataOrgUnitMergeHandler metadataHandler,
        AnalyticalObjectOrgUnitMergeHandler analyticalObjectMergeHandler,
        TrackerOrgUnitMergeHandler trackerHandler,
        IdentifiableObjectManager idObjectManager )
    {
        this.metadataHandler = metadataHandler;
        this.analyticalObjectHandler = analyticalObjectMergeHandler;
        this.trackerHandler = trackerHandler;
        this.idObjectManager = idObjectManager;
        this.handlers = getMergeHandlers();
    }

    @Override
    @Transactional
    public void merge( Set<OrganisationUnit> sources, OrganisationUnit target )
    {
        handlers.forEach( merge -> merge.apply( sources, target ) );

        // Persistence framework inspection will update associated objects

        idObjectManager.update( target );
    }

    public ErrorMessage validateForErrorMessage( OrgUnitMergeRequest request )
    {
        ErrorMessage error = null;

        if ( request.getSources().isEmpty() )
        {
        }

        return error;
    }

    public OrgUnitMergeRequest getFromQuery( OrgUnitMergeQuery query )
    {
        Set<OrganisationUnit> sources = Sets.newHashSet(
            idObjectManager.getByUid( OrganisationUnit.class, query.getSources() ) );

        OrganisationUnit target = idObjectManager.get( OrganisationUnit.class, query.getTarget() );

        return new OrgUnitMergeRequest.Builder()
            .addSources( sources )
            .withTarget( target )
            .build();
    }

    // -------------------------------------------------------------------------
    // Private methods
    // -------------------------------------------------------------------------

    private ImmutableList<OrgUnitMergeHandler> getMergeHandlers()
    {
        return ImmutableList.<OrgUnitMergeHandler> builder()
            .add( ( s, t ) -> metadataHandler.mergeDataSets( s, t ) )
            .add( ( s, t ) -> metadataHandler.mergePrograms( s, t ) )
            .add( ( s, t ) -> metadataHandler.mergeOrgUnitGroups( s, t ) )
            .add( ( s, t ) -> metadataHandler.mergeCategoryOptions( s, t ) )
            .add( ( s, t ) -> metadataHandler.mergeUsers( s, t ) )
            .add( ( s, t ) -> metadataHandler.mergeInterpretations( s, t ) )
            .add( ( s, t ) -> metadataHandler.mergeConfiguration( s, t ) )
            .add( ( s, t ) -> analyticalObjectHandler.mergeVisualizations( s, t ) )
            .add( ( s, t ) -> analyticalObjectHandler.mergeEventReports( s, t ) )
            .add( ( s, t ) -> analyticalObjectHandler.mergeEventCharts( s, t ) )
            .add( ( s, t ) -> analyticalObjectHandler.mergeMaps( s, t ) )
            .add( ( s, t ) -> trackerHandler.mergeProgramMessages( s, t ) )
            .build();
    }
}
