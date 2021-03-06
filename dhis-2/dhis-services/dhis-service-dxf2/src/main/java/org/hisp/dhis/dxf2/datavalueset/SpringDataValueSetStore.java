package org.hisp.dhis.dxf2.datavalueset;

/*
 * Copyright (c) 2004-2014, University of Oslo
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

import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifiers;
import static org.hisp.dhis.system.util.DateUtils.getLongDateString;
import static org.hisp.dhis.system.util.TextUtils.getCommaDelimitedString;

import java.io.OutputStream;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.amplecode.staxwax.factory.XMLFactory;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.datavalue.DataValue;
import org.hisp.dhis.dxf2.metadata.ExportOptions;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.system.util.StreamUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import com.csvreader.CsvWriter;

/**
 * @author Lars Helge Overland
 */
public class SpringDataValueSetStore
    implements DataValueSetStore
{
    private static final char CSV_DELIM = ',';

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    //--------------------------------------------------------------------------
    // DataValueSetStore implementation
    //--------------------------------------------------------------------------

    @Override
    public void writeDataValueSetXml( Set<DataSet> dataSets, Date completeDate, Period period, OrganisationUnit orgUnit,
        Set<Period> periods, Set<OrganisationUnit> orgUnits, OutputStream out, ExportOptions exportOptions )
    {
        DataValueSet dataValueSet = new StreamingDataValueSet( XMLFactory.getXMLWriter( out ) );

        writeDataValueSet( getDataValueSql( dataSets, periods, orgUnits, exportOptions ), dataSets, completeDate, period, orgUnit, dataValueSet );

        StreamUtils.closeOutputStream( out );
    }

    @Override
    public void writeDataValueSetJson( Set<DataSet> dataSets, Date completeDate, Period period, OrganisationUnit orgUnit, 
        Set<Period> periods, Set<OrganisationUnit> orgUnits, OutputStream outputStream, ExportOptions exportOptions )
    {
        DataValueSet dataValueSet = new StreamingJsonDataValueSet( outputStream );

        writeDataValueSet( getDataValueSql( dataSets, periods, orgUnits, exportOptions ), dataSets, completeDate, period, orgUnit, dataValueSet );

        StreamUtils.closeOutputStream( outputStream );
    }

    @Override
    public void writeDataValueSetCsv( Set<DataSet> dataSets, Date completeDate, Period period, OrganisationUnit orgUnit, 
        Set<Period> periods, Set<OrganisationUnit> orgUnits, Writer writer, ExportOptions exportOptions )
    {
        DataValueSet dataValueSet = new StreamingCsvDataValueSet( new CsvWriter( writer, CSV_DELIM ) );

        writeDataValueSet( getDataValueSql( dataSets, periods, orgUnits, exportOptions ), dataSets, completeDate, period, orgUnit, dataValueSet );
        
        StreamUtils.closeWriter( writer );
    }

    @Override
    public void writeDataValueSetJson( Date lastUpdated, OutputStream outputStream, ExportOptions exportOptions )
    {
        String deScheme = exportOptions.getDataElementIdSchemeFallback().toString().toLowerCase();
        String ouScheme = exportOptions.getOrgUnitIdSchemeFallback().toString().toLowerCase();
        String ocScheme = exportOptions.getCategoryOptionComboIdSchemeFallback().toString().toLowerCase();
        
        DataValueSet dataValueSet = new StreamingJsonDataValueSet( outputStream );

        final String sql =
            "select de." + deScheme + " as deid, pe.startdate as pestart, pt.name as ptname, ou." + ouScheme + " as ouid, " +
            "coc." + ocScheme + " as cocid, aoc." + ocScheme + " as aocid, " + 
            "dv.value, dv.storedby, dv.created, dv.lastupdated, dv.comment, dv.followup " +
            "from datavalue dv " +
            "join dataelement de on (dv.dataelementid=de.dataelementid) " +
            "join period pe on (dv.periodid=pe.periodid) " +
            "join periodtype pt on (pe.periodtypeid=pt.periodtypeid) " +
            "join organisationunit ou on (dv.sourceid=ou.organisationunitid) " +
            "join categoryoptioncombo coc on (dv.categoryoptioncomboid=coc.categoryoptioncomboid) " +
            "join categoryoptioncombo aoc on (dv.attributeoptioncomboid=aoc.categoryoptioncomboid) " +
            "where dv.lastupdated >= '" + DateUtils.getLongDateString( lastUpdated ) + "'";

        writeDataValueSet( sql, null, null, null, null, dataValueSet );
    }

    private void writeDataValueSet( String sql, Set<DataSet> dataSets, Date completeDate, Period period,
        OrganisationUnit orgUnit, final DataValueSet dataValueSet )
    {
        dataValueSet.setDataSet( ( dataSets != null && dataSets.size() == 1 ) ? dataSets.iterator().next().getUid() : null );
        dataValueSet.setCompleteDate( getLongDateString( completeDate ) );
        dataValueSet.setPeriod( period != null ? period.getIsoDate() : null );
        dataValueSet.setOrgUnit( orgUnit != null ? orgUnit.getUid() : null );

        final Calendar calendar = PeriodType.getCalendar();
        
        jdbcTemplate.query( sql, new RowCallbackHandler()
        {
            @Override
            public void processRow( ResultSet rs ) throws SQLException
            {
                DataValue dataValue = dataValueSet.getDataValueInstance();
                PeriodType pt = PeriodType.getPeriodTypeByName( rs.getString( "ptname" ) );

                dataValue.setDataElement( rs.getString( "deid" ) );
                dataValue.setPeriod( pt.createPeriod( rs.getDate( "pestart" ), calendar ).getIsoDate() );
                dataValue.setOrgUnit( rs.getString( "ouid" ) );
                dataValue.setCategoryOptionCombo( rs.getString( "cocid" ) );
                dataValue.setAttributeOptionCombo( rs.getString( "aocid" ) );
                dataValue.setValue( rs.getString( "value" ) );
                dataValue.setStoredBy( rs.getString( "storedby" ) );
                dataValue.setCreated( getLongDateString( rs.getDate( "created" ) ) );
                dataValue.setLastUpdated( getLongDateString( rs.getDate( "lastupdated" ) ) );
                dataValue.setComment( rs.getString( "comment" ) );
                dataValue.setFollowup( rs.getBoolean( "followup" ) );
                dataValue.close();
            }            
        } );
        
        dataValueSet.close();
    }
        
    //--------------------------------------------------------------------------
    // DataValueSetStore implementation
    //--------------------------------------------------------------------------

    private String getDataValueSql( Set<DataSet> dataSets, Collection<Period> periods, Collection<OrganisationUnit> orgUnits, ExportOptions exportOptions )
    {
        exportOptions = exportOptions != null ? exportOptions : new ExportOptions();
        
        String deScheme = exportOptions.getDataElementIdSchemeFallback().toString().toLowerCase();
        String ouScheme = exportOptions.getOrgUnitIdSchemeFallback().toString().toLowerCase();
        String ocScheme = exportOptions.getCategoryOptionComboIdSchemeFallback().toString().toLowerCase();
        
        return
            "select de." + deScheme + " as deid, pe.startdate as pestart, pt.name as ptname, ou." + ouScheme + " as ouid, " +
            "coc." + ocScheme + " as cocid, aoc." + ocScheme + " as aocid, " +
            "dv.value, dv.storedby, dv.created, dv.lastupdated, dv.comment, dv.followup " +
            "from datavalue dv " +
            "join dataelement de on (dv.dataelementid=de.dataelementid) " +
            "join period pe on (dv.periodid=pe.periodid) " +
            "join periodtype pt on (pe.periodtypeid=pt.periodtypeid) " +
            "join organisationunit ou on (dv.sourceid=ou.organisationunitid) " +
            "join categoryoptioncombo coc on (dv.categoryoptioncomboid=coc.categoryoptioncomboid) " +
            "join categoryoptioncombo aoc on (dv.attributeoptioncomboid=aoc.categoryoptioncomboid) " +
            "where de.dataelementid in (" + getCommaDelimitedString( getIdentifiers( getDataElements( dataSets ) ) ) + ") " +
            "and dv.periodid in (" + getCommaDelimitedString( getIdentifiers( periods ) ) + ") " +
            "and dv.sourceid in (" + getCommaDelimitedString( getIdentifiers( orgUnits ) ) + ")";
    }
    
    private Set<DataElement> getDataElements( Set<DataSet> dataSets )
    {
        Set<DataElement> elements = new HashSet<>();
        
        for ( DataSet dataSet : dataSets )
        {
            elements.addAll( dataSet.getDataElements() );
        }
        
        return elements;
    }
}
