package org.hisp.dhis.importexport.analysis;

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

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public class IndicatorFormulaIdentifier
{
    public static final String NUMERATOR = "numerator";
    public static final String DENOMINATOR = "denominator";
    
    private String indicator;
    
    private String formula;
    
    private String identifier;
    
    public IndicatorFormulaIdentifier( String indicator, String formula, String identifier )
    {
        this.indicator = indicator;
        this.formula = formula;
        this.identifier = identifier;
    }

    public String getIndicator()
    {
        return indicator;
    }

    public void setIndicator( String indicator )
    {
        this.indicator = indicator;
    }

    public String getFormula()
    {
        return formula;
    }

    public void setFormula( String formula )
    {
        this.formula = formula;
    }

    public String getIdentifier()
    {
        return identifier;
    }

    public void setIdentifier( String identifier )
    {
        this.identifier = identifier;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        
        int result = 1;
        
        result = prime * result + ( ( formula == null ) ? 0 : formula.hashCode() );
        result = prime * result + ( ( identifier == null ) ? 0 : identifier.hashCode() );
        result = prime * result + ( ( indicator == null ) ? 0 : indicator.hashCode() );
        
        return result;
    }

    @Override
    public boolean equals( Object object )
    {
        if ( this == object )
        {
            return true;
        }
        
        if ( object == null )
        {
            return false;
        }
        
        if ( getClass() != object.getClass() )
        {
            return false;
        }
        
        final IndicatorFormulaIdentifier other = (IndicatorFormulaIdentifier) object;
        
        return indicator.equals( other.indicator ) && formula.equals( other.formula ) && identifier == other.identifier;
    }

    @Override
    public String toString()
    {
        return "[" + indicator + ", " + formula + ", " + identifier + "]";
    }
}
