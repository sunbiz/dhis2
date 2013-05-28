package org.hisp.dhis.dxf2.pdfform;

/*
 * Copyright (c) 2004-2013, University of Oslo
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

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.CMYKColor;
import com.lowagie.text.pdf.PdfAnnotation;
import com.lowagie.text.pdf.PdfAppearance;
import com.lowagie.text.pdf.PdfBorderDictionary;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfFormField;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.RadioCheckField;
import com.lowagie.text.pdf.TextField;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.option.OptionService;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.period.CalendarPeriodType;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.program.ProgramStageService;
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.*;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

public class DefaultPdfDataEntryFormService
    implements PdfDataEntryFormService
{
    private static final Color COLOR_BACKGROUDTEXTBOX = Color.LIGHT_GRAY;

    private static final String TEXT_BLANK = " ";

    private static final int TEXTBOXWIDTH_NUMBERTYPE = 20;

    private static final int TEXTBOXWIDTH = 200;

    private static final int PERIODRANGE_PREVYEARS = 1;

    private static final int PERIODRANGE_FUTUREYEARS = 2;

    private static Integer MAX_OPTIONS_DISPLAYED = 30;

    private static Integer PROGRAM_FORM_ROW_NUMBER = 10;

    private PdfFormFontSettings pdfFormFontSettings;

    private I18nFormat format;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private OptionService optionService;

    // -------------------------------------------------------------------------
    // METHODS / CLASSES
    // -------------------------------------------------------------------------

    @Override
    public void generatePDFDataEntryForm( Document document, PdfWriter writer, String inputUid, int typeId,
        Rectangle pageSize, PdfFormFontSettings pdfFormFontSettings, I18nFormat format )
    {
        try
        {
            this.pdfFormFontSettings = pdfFormFontSettings;
            this.format = format;
            
            document.setPageSize( pageSize );

            document.open();

            if ( typeId == PdfDataEntryFormUtil.DATATYPE_DATASET )
            {
                setDataSet_DocumentContent( document, writer, inputUid );
            }
            else if ( typeId == PdfDataEntryFormUtil.DATATYPE_PROGRAMSTAGE )
            {
                setProgramStage_DocumentContent( document, writer, inputUid );
            }

            document.close();

        }
        catch ( Exception ex )
        {
            ex.printStackTrace();
        }
    }

    private void setDataSet_DocumentContent( Document document, PdfWriter writer, String dataSetUid )
        throws IOException, DocumentException, ParseException, Exception
    {
        DataSet dataSet = dataSetService.getDataSet( dataSetUid );

        if ( dataSet == null )
        {
            throw new Exception( "Error - DataSet not found for UID " + dataSetUid );
        }
        else
        {
            setDataSet_DocumentTopSection( document, dataSet );

            document.add( Chunk.NEWLINE );

            List<Period> periods = getPeriods_DataSet( dataSet.getPeriodType() );

            PdfPTable mainTable = new PdfPTable( 1 ); // Table with 1 cell.
            setMainTable( mainTable );

            insertTable_OrgAndPeriod( mainTable, writer, periods );

            insertTable_DataSet( mainTable, writer, dataSet );

            document.add( mainTable );

            document.add( Chunk.NEWLINE );
            document.add( Chunk.NEWLINE );

            insertSaveAsButton( document, writer, PdfDataEntryFormUtil.LABELCODE_BUTTON_SAVEAS );
        }
    }

    private void setDataSet_DocumentTopSection( Document document, DataSet dataSet )
        throws DocumentException
    {
        document.add( new Paragraph( dataSet.getDisplayName(), pdfFormFontSettings
            .getFont( PdfFormFontSettings.FONTTYPE_TITLE ) ) );

        document.add( new Paragraph( dataSet.getDescription(), pdfFormFontSettings
            .getFont( PdfFormFontSettings.FONTTYPE_DESCRIPTION ) ) );
    }

    private List<Period> getPeriods_DataSet( PeriodType periodType )
        throws ParseException
    {
        Period period = setPeriodDateRange();

        return ((CalendarPeriodType) periodType).generatePeriods( period.getStartDate(), period.getEndDate() );
    }

    private void setMainTable( PdfPTable mainTable )
    {
        mainTable.setWidthPercentage( 90.0f );
        mainTable.setHorizontalAlignment( Element.ALIGN_LEFT );
    }

    private void insertTable_DataSet( PdfPTable mainTable, PdfWriter writer, DataSet dataSet )
        throws IOException, DocumentException
    {
        Rectangle rectangle = new Rectangle( TEXTBOXWIDTH, PdfDataEntryFormUtil.CONTENT_HEIGHT_DEFAULT );

        if ( dataSet.getSections().size() > 0 )
        {
            for ( Section section : dataSet.getSections() )
            {
                insertTable_DataSetSections( mainTable, writer, rectangle, section.getDataElements(),
                    section.getDisplayName() );
            }
        }
        else
        {
            insertTable_DataSetSections( mainTable, writer, rectangle, dataSet.getDataElements(), "" );
        }
    }

    private void insertTable_DataSetSections( PdfPTable mainTable, PdfWriter writer, Rectangle rectangle,
        Collection<DataElement> dataElements, String sectionName )
        throws IOException, DocumentException
    {
        // Add Section Name and Section Spacing
        insertTable_TextRow( writer, mainTable, rectangle, TEXT_BLANK );

        if ( sectionName != "" )
        {
            insertTable_TextRow( writer, mainTable, rectangle, sectionName,
                pdfFormFontSettings.getFont( PdfFormFontSettings.FONTTYPE_SECTIONHEADER ) );
        }

        // Create A Table To Add For Each Section
        PdfPTable table = new PdfPTable( 2 );

        for ( DataElement dataElement : dataElements )
        {

            addCell_Text( table, dataElement.getDisplayName(), Element.ALIGN_RIGHT );

            String strFieldLabel = PdfDataEntryFormUtil.LABELCODE_DATAENTRYTEXTFIELD + dataElement.getUid() + "_"
                + dataElement.getCategoryCombo().getSortedOptionCombos().get( 0 ).getUid();

            String dataElementTextType = dataElement.getType();

            // Yes Only case - render as check-box
            if ( dataElementTextType.equals( DataElement.VALUE_TYPE_TRUE_ONLY ) )
            {
                addCell_WithCheckBox( table, writer, strFieldLabel );
            }
            else if ( dataElementTextType.equals( DataElement.VALUE_TYPE_BOOL ) )
            {
                // Create Yes - true, No - false, Select..
                String[] optionList = new String[]{ "[No Value]", "Yes", "No" };
                String[] valueList = new String[]{ "", "true", "false" };

                // addCell_WithRadioButton(table, writer, strFieldLabel);
                addCell_WithDropDownListField( table, strFieldLabel, optionList, valueList, rectangle, writer );
            }
            else if ( dataElementTextType.equals( DataElement.VALUE_TYPE_NUMBER ) )
            {
                rectangle = new Rectangle( TEXTBOXWIDTH_NUMBERTYPE, PdfDataEntryFormUtil.CONTENT_HEIGHT_DEFAULT );

                addCell_WithTextField( table, rectangle, writer, strFieldLabel, PdfFieldCell.TYPE_TEXT_NUMBER );
            }
            else //DataElement.VALUE_TYPE_DATE
            {
                // NOTE: When Rendering for DataSet, DataElement's OptionSet does not get rendered.
                // Only for events, it gets rendered as drop-down list.
                addCell_WithTextField( table, rectangle, writer, strFieldLabel );
            }
        }

        PdfPCell cell_withInnerTable = new PdfPCell( table );
        cell_withInnerTable.setBorder( Rectangle.NO_BORDER );

        mainTable.addCell( cell_withInnerTable );
    }

    private void setProgramStage_DocumentContent( Document document, PdfWriter writer, String programStageUid )
        throws IOException, DocumentException, ParseException, Exception
    {
        ProgramStage programStage = programStageService.getProgramStage( programStageUid );

        if ( programStage == null )
        {
            throw new Exception( "Error - ProgramStage not found for UID " + programStageUid );
        }
        else
        {
            // 1. Get Rectangle with TextBox Width to be used
            Rectangle rectangle = new Rectangle( 0, 0, TEXTBOXWIDTH, PdfDataEntryFormUtil.CONTENT_HEIGHT_DEFAULT );

            // 2. Create Main Layout table and set the properties
            PdfPTable mainTable = getProgramStageMainTable();

            // 3. Generate Period List for ProgramStage
            List<Period> periods = getProgramStagePeriodList();

            // 4. Add Org Unit, Period, Hidden ProgramStageID Field
            insertTable_OrgAndPeriod( mainTable, writer, periods );

            insertTable_TextRow( writer, mainTable, rectangle, TEXT_BLANK );

            // Add ProgramStage Field - programStage.getId();
            insertTable_HiddenValue( mainTable, rectangle, writer,
                PdfDataEntryFormUtil.LABELCODE_PROGRAMSTAGEIDTEXTBOX, String.valueOf( programStage.getId() ) );

            // 5. Add ProgramStage Content to PDF - [The Main Section]
            insertTable_ProgramStage( mainTable, writer, programStage );

            // 6. Add the mainTable to document
            document.add( mainTable );
        }
    }

    private void insertTable_ProgramStage( PdfPTable mainTable, PdfWriter writer, ProgramStage programStage )
        throws IOException, DocumentException
    {
        Rectangle rectangle = new Rectangle( TEXTBOXWIDTH, PdfDataEntryFormUtil.CONTENT_HEIGHT_DEFAULT );

        // Add Program Stage Sections
        if ( programStage.getProgramStageSections().size() > 0 )
        {
            // Sectioned Ones
            for ( ProgramStageSection section : programStage.getProgramStageSections() )
            {
                insertTable_ProgramStageSections( mainTable, rectangle, writer, section.getProgramStageDataElements() );
            }
        }
        else
        {
            // Default one
            insertTable_ProgramStageSections( mainTable, rectangle, writer, programStage.getProgramStageDataElements() );
        }
    }

    private void insertTable_ProgramStageSections( PdfPTable mainTable, Rectangle rectangle, PdfWriter writer,
        Collection<ProgramStageDataElement> programStageDataElements )
        throws IOException, DocumentException
    {
        // Add one to column count due to date entry + one hidden height set field.
        int colCount = programStageDataElements.size() + 1 + 1;

        PdfPTable table = new PdfPTable( colCount ); // Code 1

        float totalWidth = 800f;
        float firstCellWidth_dateEntry = PdfDataEntryFormUtil.UNITSIZE_DEFAULT * 3;
        float lastCellWidth_hidden = PdfDataEntryFormUtil.UNITSIZE_DEFAULT;
        float dataElementCellWidth = (totalWidth - firstCellWidth_dateEntry - lastCellWidth_hidden)
            / programStageDataElements.size();

        // Create 2 types of Rectangles, one for Date field, one for data
        // elements - to be used when rendering them.
        Rectangle rectangleDate = new Rectangle( 0, 0, PdfDataEntryFormUtil.UNITSIZE_DEFAULT * 2,
            PdfDataEntryFormUtil.UNITSIZE_DEFAULT );
        Rectangle rectangleDataElement = new Rectangle( 0, 0, dataElementCellWidth,
            PdfDataEntryFormUtil.UNITSIZE_DEFAULT );

        // Cell Width Set
        float[] cellWidths = new float[colCount];

        // Date Field Settings
        cellWidths[0] = firstCellWidth_dateEntry;

        for ( int i = 1; i < colCount - 1; i++ )
        {
            cellWidths[i] = dataElementCellWidth;
        }

        cellWidths[colCount - 1] = lastCellWidth_hidden;

        table.setWidths( cellWidths );

        // Create Header
        table.addCell( new PdfPCell( new Phrase( "Date" ) ) );

        // Add Program Data Elements Columns
        for ( ProgramStageDataElement programStageDataElement : programStageDataElements )
        {
            DataElement dataElement = programStageDataElement.getDataElement();

            table.addCell( new PdfPCell( new Phrase( dataElement.getDisplayFormName() ) ) );
        }

        table.addCell( new PdfPCell( new Phrase( TEXT_BLANK ) ) );

        // ADD A HIDDEN INFO FOR ProgramStageID
        // Print rows, having the data elements repeating on each column.

        for ( int rowNo = 1; rowNo <= PROGRAM_FORM_ROW_NUMBER; rowNo++ )
        {
            // Add Date Column
            String strFieldDateLabel = PdfDataEntryFormUtil.LABELCODE_DATADATETEXTFIELD + Integer.toString( rowNo );

            addCell_WithTextField( table, rectangleDate, writer, strFieldDateLabel );

            // Add Program Data Elements Columns
            for ( ProgramStageDataElement programStageDataElement : programStageDataElements )
            {
                DataElement dataElement = programStageDataElement.getDataElement();

                OptionSet optionSet = dataElement.getOptionSet();
                String optionSetName = "";

                // addCell_Text(table, dataElement.getFormName());

                String strFieldLabel = PdfDataEntryFormUtil.LABELCODE_DATAENTRYTEXTFIELD
                    + Integer.toString( dataElement.getId() )
                    // + "_" + Integer.toString(programStageId) + "_" +
                    // Integer.toString(rowNo);
                    + "_" + Integer.toString( rowNo );

                if ( optionSet != null )
                {
                    optionSetName = optionSet.getName();

                    String query = ""; // Get All Option

                    // TODO: This gets repeated <- Create an array of the
                    // options. and apply only once.
                    List<String> options = optionService.getOptions( optionSet.getId(), query, MAX_OPTIONS_DISPLAYED );

                    addCell_WithDropDownListField( table, strFieldLabel, options.toArray( new String[0] ),
                        options.toArray( new String[0] ), rectangleDataElement, writer );
                }
                else
                {
                    // NOTE: When Rendering for DataSet, DataElement's OptionSet
                    // does not get rendered.
                    // Only for events, it gets rendered as dropdown list.
                    addCell_WithTextField( table, rectangleDataElement, writer, strFieldLabel );
                }
            }

            addCell_Text( table, TEXT_BLANK, Element.ALIGN_LEFT );
        }

        PdfPCell cell_withInnerTable = new PdfPCell( table );
        cell_withInnerTable.setBorder( Rectangle.NO_BORDER );

        mainTable.addCell( cell_withInnerTable );

    }

    private List<Period> getProgramStagePeriodList()
        throws ParseException
    {
        Period period = setPeriodDateRange();

        PeriodType periodType = PeriodType.getPeriodTypeByName( MonthlyPeriodType.NAME );

        return ((CalendarPeriodType) periodType).generatePeriods( period.getStartDate(), period.getEndDate() );
    }

    private PdfPTable getProgramStageMainTable()
    {
        PdfPTable mainTable = new PdfPTable( 1 ); // Code 1

        mainTable.setTotalWidth( 800f );
        mainTable.setLockedWidth( true );
        mainTable.setHorizontalAlignment( Element.ALIGN_LEFT );

        return mainTable;
    }

    private void insertTable_OrgAndPeriod( PdfPTable mainTable, PdfWriter writer, List<Period> periods )
        throws IOException, DocumentException
    {
        // Input TextBox size
        Rectangle rectangle = new Rectangle( TEXTBOXWIDTH, PdfDataEntryFormUtil.CONTENT_HEIGHT_DEFAULT );

        // Add Organization ID/Period textfield
        // Create A table to add for each group AT HERE
        PdfPTable table = new PdfPTable( 2 ); // Code 1

        addCell_Text( table, "OrganizationID", Element.ALIGN_LEFT );
        addCell_WithTextField( table, rectangle, writer, PdfDataEntryFormUtil.LABELCODE_ORGID,
            PdfFieldCell.TYPE_TEXT_ORGUNIT );

        String[] periodsTitle = getPeriodTitles( periods, format );
        String[] periodsValue = getPeriodValues( periods );

        addCell_Text( table, "PeriodID", Element.ALIGN_LEFT );
        addCell_WithDropDownListField( table, PdfDataEntryFormUtil.LABELCODE_PERIODID, periodsTitle, periodsValue,
            rectangle, writer );

        // Add to the main table
        PdfPCell cell_withInnerTable = new PdfPCell( table );
        // cell_withInnerTable.setPadding(0);
        cell_withInnerTable.setBorder( Rectangle.NO_BORDER );

        cell_withInnerTable.setHorizontalAlignment( Element.ALIGN_LEFT );

        mainTable.addCell( cell_withInnerTable );
    }

    private void insertTable_HiddenValue( PdfPTable mainTable, Rectangle rectangle, PdfWriter writer, String fieldName,
        String value )
        throws IOException, DocumentException
    {
        // Add Organization ID/Period textfield
        // Create A table to add for each group AT HERE
        PdfPTable table = new PdfPTable( 1 ); // Code 1

        addCell_WithTextField( table, rectangle, writer, fieldName, value );

        // Add to the main table
        PdfPCell cell_withInnerTable = new PdfPCell( table );
        // cell_withInnerTable.setPadding(0);
        cell_withInnerTable.setBorder( Rectangle.NO_BORDER );
        mainTable.addCell( cell_withInnerTable );
    }

    private void insertTable_TextRow( PdfWriter writer, PdfPTable mainTable, Rectangle rectangle, String text )
    {
        insertTable_TextRow( writer, mainTable, rectangle, text,
            pdfFormFontSettings.getFont( PdfFormFontSettings.FONTTYPE_BODY ) );
    }

    private void insertTable_TextRow( PdfWriter writer, PdfPTable mainTable, Rectangle rectangle, String text, Font font )
    {
        // Add Organization ID/Period textfield
        // Create A table to add for each group AT HERE
        PdfPTable table = new PdfPTable( 1 );
        table.setHorizontalAlignment( Element.ALIGN_LEFT );

        addCell_Text( table, text, Element.ALIGN_LEFT, font );

        // Add to the main table
        PdfPCell cell_withInnerTable = new PdfPCell( table );
        cell_withInnerTable.setBorder( Rectangle.NO_BORDER );
        mainTable.addCell( cell_withInnerTable );
    }

    // Insert 'Save As' button to document.
    private void insertSaveAsButton( Document document, PdfWriter writer, String name )
        throws DocumentException
    {
        // Button Table
        PdfPTable tableButton = new PdfPTable( 1 );

        tableButton.setWidthPercentage( 20.0f );
        float buttonHeight = PdfDataEntryFormUtil.UNITSIZE_DEFAULT + 5;

        tableButton.setHorizontalAlignment( Element.ALIGN_CENTER );

        String jsAction = "app.execMenuItem('SaveAs');";

        addCell_WithPushButtonField( tableButton, name, buttonHeight, jsAction, writer );

        document.add( tableButton );
    }

    private void addCell_Text( PdfPTable table, String text, int horizontalAlignment )
    {
        addCell_Text( table, text, horizontalAlignment, pdfFormFontSettings.getFont( PdfFormFontSettings.FONTTYPE_BODY ) );
    }

    private void addCell_Text( PdfPTable table, String text, int horizontalAlignment, Font font )
    {
        PdfPCell cell = PdfDataEntryFormUtil.getPdfPCell( PdfDataEntryFormUtil.CELL_MIN_HEIGHT_DEFAULT,
            PdfDataEntryFormUtil.CELL_COLUMN_TYPE_LABEL );

        cell.setHorizontalAlignment( horizontalAlignment );

        cell.setPhrase( new Phrase( text, font ) );

        table.addCell( cell );
    }

    private void addCell_WithTextField( PdfPTable table, Rectangle rect, PdfWriter writer, String strfldName )
        throws IOException, DocumentException
    {
        addCell_WithTextField( table, rect, writer, strfldName, PdfFieldCell.TYPE_DEFAULT, "" );
    }

    private void addCell_WithTextField( PdfPTable table, Rectangle rect, PdfWriter writer, String strfldName,
        int fieldCellType )
        throws IOException, DocumentException
    {
        addCell_WithTextField( table, rect, writer, strfldName, fieldCellType, "" );
    }

    private void addCell_WithTextField( PdfPTable table, Rectangle rect, PdfWriter writer, String strfldName,
        String value )
        throws IOException, DocumentException
    {
        addCell_WithTextField( table, rect, writer, strfldName, PdfFieldCell.TYPE_DEFAULT, value );
    }

    private void addCell_WithTextField( PdfPTable table, Rectangle rect, PdfWriter writer, String strfldName,
        int fieldCellType, String value )
        throws IOException, DocumentException
    {
        TextField nameField = new TextField( writer, rect, strfldName );

        nameField.setBorderWidth( 1 );
        nameField.setBorderColor( Color.BLACK );
        nameField.setBorderStyle( PdfBorderDictionary.STYLE_SOLID );
        nameField.setBackgroundColor( COLOR_BACKGROUDTEXTBOX );

        nameField.setText( value );

        nameField.setAlignment( Element.ALIGN_RIGHT );
        nameField.setFontSize( PdfDataEntryFormUtil.UNITSIZE_DEFAULT );

        PdfPCell cell = PdfDataEntryFormUtil.getPdfPCell( PdfDataEntryFormUtil.CELL_MIN_HEIGHT_DEFAULT,
            PdfDataEntryFormUtil.CELL_COLUMN_TYPE_ENTRYFIELD );
        cell.setCellEvent( new PdfFieldCell( nameField.getTextField(), (int) (rect.getWidth()), fieldCellType, writer ) );

        table.addCell( cell );
    }

    private void addCell_WithDropDownListField( PdfPTable table, String strfldName, String[] optionList,
        String[] valueList, Rectangle rect, PdfWriter writer )
    {
        // If there is option, then create name-value set in 2 dimension array
        // and set it as dropdown option name-value list.
        String[][] optionValueList = new String[optionList.length][2];

        for ( int i = 0; i < optionList.length; i++ )
        {
            optionValueList[i][1] = optionList[i];
            optionValueList[i][0] = valueList[i];
        }

        // Code 2 create DROP-DOWN LIST
        PdfFormField dropDown = PdfFormField.createCombo( writer, true, optionValueList, 0 );

        dropDown.setWidget( rect, PdfAnnotation.HIGHLIGHT_INVERT );
        dropDown.setFieldName( strfldName );

        dropDown.setMKBorderColor( CMYKColor.BLACK );

        PdfPCell cell = PdfDataEntryFormUtil.getPdfPCell( PdfDataEntryFormUtil.CELL_MIN_HEIGHT_DEFAULT,
            PdfDataEntryFormUtil.CELL_COLUMN_TYPE_ENTRYFIELD );
        cell.setCellEvent( new PdfFieldCell( dropDown, (int) (rect.getWidth()), writer ) );

        table.addCell( cell );
    }

    private void addCell_WithCheckBox( PdfPTable table, PdfWriter writer, String strfldName )
        throws IOException, DocumentException
    {
        float sizeDefault = PdfDataEntryFormUtil.UNITSIZE_DEFAULT;

        PdfContentByte canvas = writer.getDirectContent();

        PdfAppearance[] onOff = new PdfAppearance[2];
        onOff[0] = canvas.createAppearance( sizeDefault + 2, sizeDefault + 2 );
        onOff[0].rectangle( 1, 1, sizeDefault, sizeDefault );
        onOff[0].stroke();
        onOff[1] = canvas.createAppearance( sizeDefault + 2, sizeDefault + 2 );
        onOff[1].setRGBColorFill( 255, 128, 128 );
        onOff[1].rectangle( 1, 1, sizeDefault, sizeDefault );
        onOff[1].fillStroke();
        onOff[1].moveTo( 1, 1 );
        onOff[1].lineTo( sizeDefault + 1, sizeDefault + 1 );
        onOff[1].moveTo( 1, sizeDefault + 1 );
        onOff[1].lineTo( sizeDefault + 1, 1 );
        onOff[1].stroke();

        Rectangle rect = new Rectangle( sizeDefault, sizeDefault );

        RadioCheckField checkbox = new RadioCheckField( writer, rect, "Yes", "on" );
        checkbox.setBorderWidth( 1 );
        checkbox.setBorderColor( Color.BLACK );

        PdfFormField checkboxfield = checkbox.getCheckField();
        checkboxfield.setFieldName( strfldName );

        checkboxfield.setAppearance( PdfAnnotation.APPEARANCE_NORMAL, "", onOff[0] );
        checkboxfield.setAppearance( PdfAnnotation.APPEARANCE_NORMAL, "true", onOff[1] );

        PdfPCell cell = PdfDataEntryFormUtil.getPdfPCell( PdfDataEntryFormUtil.CELL_MIN_HEIGHT_DEFAULT,
            PdfDataEntryFormUtil.CELL_COLUMN_TYPE_ENTRYFIELD );

        cell.setCellEvent( new PdfFieldCell( checkboxfield, (int) (rect.getWidth()), writer ) );

        table.addCell( cell );
    }

    private void addCell_WithRadioButton( PdfPTable table, PdfWriter writer, String strfldName )
    {
        // Add to the main table
        PdfPCell cell = PdfDataEntryFormUtil.getPdfPCell( PdfDataEntryFormUtil.CELL_MIN_HEIGHT_DEFAULT,
            PdfDataEntryFormUtil.CELL_COLUMN_TYPE_ENTRYFIELD );

        // RADIO BUTTON FIELD
        PdfFormField radiogroupField = PdfFormField.createRadioButton( writer, true );
        radiogroupField.setFieldName( strfldName );

        cell.setCellEvent( new PdfFieldCell( radiogroupField, new String[]{ "Yes", "No", "null" }, new String[]{
            "true", "false", "" }, "", 30.0f, PdfFieldCell.TYPE_RADIOBUTTON, writer ) );

        table.addCell( cell );

        // Last - Add Annotation
        writer.addAnnotation( radiogroupField );
    }

    private void addCell_WithPushButtonField( PdfPTable table, String strfldName, float buttonHeight, String jsAction,
        PdfWriter writer )
    {
        PdfPCell cell = PdfDataEntryFormUtil.getPdfPCell( buttonHeight,
            PdfDataEntryFormUtil.CELL_COLUMN_TYPE_ENTRYFIELD );
        cell.setCellEvent( new PdfFieldCell( null, jsAction, "BTN_SAVEPDF", "Save PDF", PdfFieldCell.TYPE_BUTTON,
            writer ) );

        table.addCell( cell );
    }

    public String[] getPeriodValues( List<Period> periods )
    {
        String[] periodValues = new String[periods.size()];

        for ( int i = 0; i < periods.size(); i++ )
        {
            periodValues[i] = periods.get( i ).getExternalId();
        }

        return periodValues;
    }

    public String[] getPeriodTitles( List<Period> periods, I18nFormat format )
    {
        String[] periodTitles = new String[periods.size()];

        for ( int i = 0; i < periods.size(); i++ )
        {
            periodTitles[i] = format.formatPeriod( periods.get( i ) );
        }

        return periodTitles;
    }

    private Period setPeriodDateRange()
        throws ParseException
    {
        Period period = new Period();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat( Period.DEFAULT_DATE_FORMAT );

        Calendar currentDate = Calendar.getInstance();

        int currYear = currentDate.get( Calendar.YEAR );
        int startYear = currYear - PERIODRANGE_PREVYEARS;
        int endYear = currYear + PERIODRANGE_FUTUREYEARS;

        period.setStartDate( simpleDateFormat.parse( String.valueOf( startYear ) + "-01-01" ) );
        period.setEndDate( simpleDateFormat.parse( String.valueOf( endYear ) + "-01-01" ) );

        return period;
    }
}