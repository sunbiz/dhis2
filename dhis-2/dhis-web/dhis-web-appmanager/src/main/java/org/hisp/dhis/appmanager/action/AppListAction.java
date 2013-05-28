package org.hisp.dhis.appmanager.action;

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

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManagerService;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.Action;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts2.ServletActionContext;
import org.hisp.dhis.util.ContextUtils;

/**
 * @author Saptarshi Purkayastha
 */
public class AppListAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private AppManagerService appManagerService;

    // -------------------------------------------------------------------------
    // Input & Output
    // -------------------------------------------------------------------------

    private List<App> appList = new ArrayList<App>();

    public List<App> getAppList()
    {
        return appManagerService.getInstalledApps();
    }

    private List<String> appFolderNames = new ArrayList<String>();

    public List<String> getAppFolderNames()
    {
        return appFolderNames;
    }

    //TODO: create settings to set for external server like Apache2/nginx
    // Should be a per-app setting
    private String appsRootUrl = new String();

    public String getAppsRootUrl()
    {
        HttpServletRequest request = ServletActionContext.getRequest();
        String realPath = ServletActionContext.getServletContext().getRealPath( "/" );
        String appFolderPath = appManagerService.getAppFolderPath();
        String baseUrl = ContextUtils.getBaseUrl( request );
        String contextPath = request.getContextPath();
        if ( !contextPath.isEmpty() )
        {
            appsRootUrl = baseUrl.substring( 0, baseUrl.length() - 1 ) + request.getContextPath() + "/"
                + ((appFolderPath.replace( "//", "/" )).replace( realPath, "" )).replace( '\\', '/' );
        }
        else
        {
            appsRootUrl = baseUrl.substring( 0, baseUrl.length() - 1 )
                + ((appFolderPath.replace( "//", "/" )).replace( realPath, "" )).replace( '\\', '/' );
        }
        return appsRootUrl;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        for ( App app : getAppList() )
        {
            appFolderNames.add( appManagerService.getAppFolderName( app ) );
        }

        return SUCCESS;
    }
}