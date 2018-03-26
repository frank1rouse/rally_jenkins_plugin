/*
 * Copyright 2014 - Rally Software Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rallydev.integration.build.plugin;

import com.rallydev.integration.build.rest.RallyRestService;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Defect {
    private static final int MESSAGE_SIZE = 4000;
    private RallyRestService service;
    private String description;
    private String name;
    private String uri;
    private String projectRef;
    private String severity;
    private String priority;
    private boolean created =  false;
    private String foundInBuild;
    private String id;
    private String ref;

    private static final SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public Defect(RallyRestService service) {
        this.service = service;
    }

    private String truncate(String string) {
        if (string.length() <= MESSAGE_SIZE) {
            return string;
        } else {
            return string.substring(string.length() - MESSAGE_SIZE,
                string.length());
        }
    }
    
    public String create() throws IOException {
        // call post
        String response = getService().create("defect", toXml());
        try{
            //Some weird stuff happens when I try to parse this using a document builder
            //Had to parse it using String methods, should be rewritten for quality at some point
            String refText = response.substring(response.indexOf("ref="));
            refText = refText.substring(5);
            refText = refText.split("\"")[0];
            setRef(refText);
            String [] tokens = refText.split("/");
            setId(tokens[tokens.length-1]);
        }catch(Exception e){
            //Don't really need to do anything here
            //If it fails, it doesn't mean the defect shouldn't be filed
        }
        setCreated(true);
        return response;
    }
    
    public String tag(Tag tag) throws Exception{
        String response = "";
        if(!tag.isCreated()){
            throw new Exception("Failed to create tag");
            //If the defect ref is not null, if the ref does not equal "", and the response from creating the defect does not contain Errors
        }else if(getRef()!=null && !getRef().equals("") && !response.contains("</Errors>")){
            StringBuffer xml = new StringBuffer();
            xml.append("<Defect>");
            xml.append("<Tags>");
            xml.append(tag.toXml());
            xml.append("</Tags>");
            xml.append("</Defect>");
            response = getService().update(getRef(), xml.toString());
        }else{
            throw new Exception("Failed to tag defect");
        }
        return response;
    }

    protected String toXml() throws IOException {
        // create Build XML
        StringBuffer xml = new StringBuffer();
        xml.append("<Defect>");
        xml.append("<Project ref=\"");
        xml.append(getProjectRef());
        xml.append("\" type=\"Project\"/>");
        
        xml.append("<Name>");
        xml.append(name);
        xml.append("</Name>");

        if (description != null) {
            xml.append("<Description>");
            xml.append("<![CDATA[");
            xml.append(truncate(description));
            xml.append("]]>");
            xml.append("</Description>");
        }
        
        if (severity != null) {
            xml.append("<Severity>");
            xml.append(severity);
            xml.append("</Severity>");
        }
        
        if (priority != null) {
            xml.append("<Priority>");
            xml.append(priority);
            xml.append("</Priority>");
        }
        
        if (foundInBuild != null){
            xml.append("<FoundInBuild>");
            xml.append(foundInBuild);
            xml.append("</FoundInBuild>");
        }

        xml.append("</Defect>");

        return xml.toString();
    }

    public RallyRestService getService() {
        return service;
    }

    public void setService(RallyRestService service) {
        this.service = service;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setProjectRef(String projectRef) {
		this.projectRef = projectRef;
	}

	public String getProjectRef() {
		return projectRef;
	}
	
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getUri() {
		return uri;
	}

    public String getPriority()
    {
        return priority;
    }

    public void setPriority(String priority)
    {
        this.priority = priority;
    }

    public String getSeverity()
    {
        return severity;
    }

    public void setSeverity(String severity)
    {
        this.severity = severity;
    }

    public String getFoundInBuild()
    {
        return foundInBuild;
    }

    public void setFoundInBuild(String foundInBuild)
    {
        this.foundInBuild = foundInBuild;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getRef()
    {
        return ref;
    }

    public void setRef(String ref)
    {
        this.ref = ref;
    }

    public boolean isCreated()
    {
        return created;
    }

    public void setCreated(boolean created)
    {
        this.created = created;
    }
    
    
}
