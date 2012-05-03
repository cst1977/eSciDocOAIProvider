package de.fiz.karlsruhe.escidoc.services.oaiprovider.saxhandler;

import org.xml.sax.Attributes;

public class IdentifyHandler extends DefaultHandler {
   
    private boolean behindElement = false;

    private boolean inElement = false;
    private boolean inEarliestDate = false;
    private boolean inName = false;
    private boolean inEmail = false;
    private boolean inBaseUrl = false;
    
    private String earliestDate;
    private String name;
    private String email;
    private String baseUrl;
    
    
    public IdentifyHandler() {
        
    }

   
    public void startElement(
        String uri, String localName, String qName, Attributes attributes) {
        
    	this.behindElement = false;
        if (qName.equals("entry")) {
            inElement = true; 
            String value = attributes.getValue("key");
            if (value.equals("escidoc-core.earliest-date")) {
                inEarliestDate = true;
            } else if (value.equals("escidoc-core.repository-name")) {
                inName = true;
            } else if (value.equals("escidoc-core.admin-email")) {
                inEmail = true;
            } else if (value.equals("escidoc-core.baseurl")) {
                inBaseUrl = true;
            }
        }
        
    }

    public void endElement(String uri, String localName, String qName) {
    	this.behindElement = true;
        if (inElement) {
            if (inEarliestDate) {
                inEarliestDate = false;   
               } else if (inName) {
                   inName = false;
               } else if (inEmail) {
                   inEmail = false;
               } else if (inBaseUrl) {
                   inBaseUrl = false;
               } 
            inElement = false;
        }                 
    }

    public void characters(char[] ch, int start, int length) {
        
    	if (behindElement) {
    		return;
    	}
        if (inElement) {
            if (inEarliestDate) {
                if (this.earliestDate == null) {
                    this.earliestDate = new String(ch, start, length);
                }  else {
                    this.earliestDate = this.earliestDate + new String(ch, start, length);
                }
            } else if (inName) {
                if (this.name == null) {
                    this.name = new String(ch, start, length);
                }  else {
                    this.name = this.name + new String(ch, start, length);
                }
            } else if (inEmail) {
                if (this.email == null) {
                    this.email = new String(ch, start, length);
                }  else {
                    this.email = this.email + new String(ch, start, length);
                }
            } else if (inBaseUrl) {
                if (this.baseUrl == null) {
                    this.baseUrl = new String(ch, start, length);
                }  else {
                    this.baseUrl = this.baseUrl + new String(ch, start, length);
                }
            }
            
        }
    }

    public String getBaseUrl() {
        return this.baseUrl;
    }

    public String getName() {
        return this.name;
    }
    public String getEmail() {
        return this.email;
    }
    public String getEarliestDate(){
        return this.earliestDate;
    }
}
