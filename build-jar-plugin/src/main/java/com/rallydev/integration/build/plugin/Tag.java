/**
 * 
 */
package com.rallydev.integration.build.plugin;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.rallydev.integration.build.rest.RallyRestService;

/**
 * @author smithc41
 *
 */

public class Tag
{
    private RallyRestService service;
    private String ref;
    private String id;
    private String name;
    private boolean created;
    
    
    //If we know it exists let's use this constructor and mark it as created
    public Tag(RallyRestService service, String ref, String name){
        this.service = service;
        this.ref = ref;
        this.created = true;
    }
    
    //If we don't know if it exists, then let's use this constructor
    public Tag(RallyRestService service, String name) throws Exception{
        this.service = service;
        this.name = name;
        //Try to import ref from the existing tag, and if it doesn't exist... create it
        try
        {
            checkExistence(name);
            if(ref == ""){
                throw new Exception("Ref was not populated");
            }
        }
        catch (Exception e){
           throw new Exception("The tag could not be located");
        }
    }
    
    public String extractRef(String response){
        return null;
    }
    
    public int countOccurence(String substring, String string){
        int count = 0;
        int index = 0;
        while(index != -1){
            index = string.indexOf(substring,index);
            if (index!=-1){
                count++;
            }
        }
        return count;
    }
    
    public String toXml(){
        StringBuffer xml = new StringBuffer();
        xml.append("<Tag ref=\"");
        xml.append(getRef());
        xml.append("\">");
        xml.append("</Tag>");
        return xml.toString();
    }
    
    public void tagArtifact(String artifact) throws Exception{
        String response = "";
        if(!created){
            try{
                response = create();
            }catch(Exception e){
                throw new Exception("Unable to create the tag due to the following error:\n"+e.toString());
            }
        }
        if(response.equals("") || response.contains("</Errors>")){
            throw new Exception("The tag does not exist and could not be created");
        }else{
            getService().update(artifact, toXml());
        }
    }
    
    /**This method needs to be evaluated before it is used*/
    public String create() throws IOException{
     // call post
        String response = getService().create("tag", toXml());
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

        }
        setCreated(true);
        return response;
    }
    
    protected String checkExistence(String query) throws IOException, ParserConfigurationException, SAXException{
        String response = getService().query("tag","(Name contains \""+query+"\")");
        
        //Isn't XML parsing fun.....?
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = factory.newDocumentBuilder();
        ByteArrayInputStream in = new ByteArrayInputStream(response.toString().getBytes("UTF-8"));
        Document doc = db.parse(in);
        NodeList resultList = doc.getElementsByTagName("Object");
        int resultListLength  = resultList.getLength();
        if(resultListLength > 0){
            boolean loop = true;
            int index = 0;
            String refObjectName = "";
            String ref = "";
            while(loop){
                Node n = resultList.item(index);
                NamedNodeMap attributes = n.getAttributes();
                refObjectName = attributes.getNamedItem("refObjectName").getNodeValue();
                ref = attributes.getNamedItem("ref").getNodeValue();
                if(refObjectName.equals(query) || (index==(resultList.getLength()-1))){
                    loop = false;
                }
            }
            setName(refObjectName);
            setRef(ref);
        }
        if(ref!=null){
            if(!ref.equals("")){
                setCreated(true);
            }
        }
        return response;
    }

    public RallyRestService getService()
    {
        return service;
    }

    public void setService(RallyRestService service)
    {
        this.service = service;
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

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
    
    
}
