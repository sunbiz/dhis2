package org.hisp.dhis.reportsheet.organisationunit.action;

/*
 * Copyright (c) 2004-2012, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of the HISP project nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
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

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.ouwt.manager.OrganisationUnitSelectionManager;

import com.opensymphony.xwork2.Action;

/**
 * @author Dang Duy Hieu
 * @version $Id$
 */
public class EditDepartmentAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    private OrganisationUnitGroupService organisationUnitGroupService;

    public void setOrganisationUnitGroupService( OrganisationUnitGroupService organisationUnitGroupService )
    {
        this.organisationUnitGroupService = organisationUnitGroupService;
    }

    private OrganisationUnitSelectionManager selectionManager;

    public void setSelectionManager( OrganisationUnitSelectionManager selectionManager )
    {
        this.selectionManager = selectionManager;
    }

    // -------------------------------------------------------------------------
    // Input & Output
    // -------------------------------------------------------------------------

    private String name;

    public void setName( String name )
    {
        this.name = name;
    }

    private String shortName;

    public void setShortName( String shortName )
    {
        this.shortName = shortName;
    }

    private List<String> selectedGroups = new ArrayList<String>();

    public void setSelectedGroups( List<String> selectedGroups )
    {
        this.selectedGroups = selectedGroups;
    }

    private List<String> orgUnitGroupSets = new ArrayList<String>();

    public void setOrgUnitGroupSets( List<String> orgUnitGroupSets )
    {
        this.orgUnitGroupSets = orgUnitGroupSets;
    }

    private String id;

    public void setId( String id )
    {
        this.id = id;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    public String execute()
        throws Exception
    {
        // ---------------------------------------------------------------------
        // Get parent
        // ---------------------------------------------------------------------

        OrganisationUnit parent = selectionManager.getSelectedOrganisationUnit();

        if ( parent == null )
        {
            // -----------------------------------------------------------------
            // If no unit is selected, the parent is the parent of the roots
            // -----------------------------------------------------------------

            parent = selectionManager.getRootOrganisationUnitsParent();
        }

        // ---------------------------------------------------------------------
        // Create organization unit
        // ---------------------------------------------------------------------

        OrganisationUnit department = organisationUnitService.getOrganisationUnitByUuid( id );

        if ( department == null )
        {
            department = organisationUnitService.getOrganisationUnit( Integer.parseInt( id ) );
        }
        
        department.setName( name );
        department.setShortName( shortName );

        // ---------------------------------------------------------------------
        // Must persist org-unit before adding data sets because association are
        // updated on both sides (and this side is inverse)
        // ---------------------------------------------------------------------

        for ( int i = 0; i < orgUnitGroupSets.size(); i++ )
        {
            OrganisationUnitGroupSet groupSet = organisationUnitGroupService.getOrganisationUnitGroupSet( Integer
                .parseInt( orgUnitGroupSets.get( i ) ) );

            OrganisationUnitGroup oldGroup = groupSet.getGroup( department );

            OrganisationUnitGroup newGroup = organisationUnitGroupService.getOrganisationUnitGroup( Integer
                .parseInt( selectedGroups.get( i ) ) );

            if ( oldGroup != null )
            {
                oldGroup.removeOrganisationUnit( department );

                for ( DataSet ds : oldGroup.getDataSets() )
                {
                    department.removeDataSet( ds );
                }

                organisationUnitGroupService.updateOrganisationUnitGroup( oldGroup );
            }

            if ( newGroup != null )
            {
                newGroup.addOrganisationUnit( department );

                for ( DataSet ds : newGroup.getDataSets() )
                {
                    department.addDataSet( ds );
                }

                organisationUnitGroupService.updateOrganisationUnitGroup( newGroup );
            }
        }

        organisationUnitService.updateOrganisationUnit( department );

        return SUCCESS;
    }
}
