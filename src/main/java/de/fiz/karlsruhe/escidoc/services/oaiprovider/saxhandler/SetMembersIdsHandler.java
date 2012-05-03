package de.fiz.karlsruhe.escidoc.services.oaiprovider.saxhandler;

import java.util.Vector;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;

public class SetMembersIdsHandler extends DefaultHandler {
    private static final Logger logger =
        Logger.getLogger(SetMembersIdsHandler.class.getName());
    
    private String oaiIdPrefix = "oai:escidoc.org:";
    
    private boolean behindElement = false;

    private String elementName;

    private boolean inElement = false;

    private Vector<String> resourceIds = null;

    private String resourceId;
    
    private String recordsNumber;
    
    private String recordPosition;


    public SetMembersIdsHandler(String namespaceIdentifier) {
        oaiIdPrefix = "oai:" + namespaceIdentifier + ":";
        this.resourceIds = new Vector<String>();
        this.recordsNumber = null;
    }
    public void startElement(
        String uri, String localName, String qName, Attributes attributes) {
    	this.behindElement = false;
        this.elementName = localName;
        if (localName.equals("record")) {
            inElement = true;
            recordPosition = null;
        }

    }

    public void endElement(String uri, String localName, String qName) {

    	this.behindElement = true;
        if (localName.equals("record")) {
            inElement = false;
            resourceIds.add(oaiIdPrefix
                + this.resourceId);
            resourceId = null;
            
        } 
    }

    public void characters(char[] ch, int start, int length) {
    	if (behindElement) {
    		return;
    	}
        if (inElement) {
            if (this.elementName.equals("id")) {
                String id = new String(ch, start, length);
                id = id.replaceAll("[\r\n]", "");
                if (id.length() != 0) {
                    if (this.resourceId != null) {
                        this.resourceId = this.resourceId + id;
                    }
                    else {
                        this.resourceId = id;
                    }
                }
            } else if (this.elementName.equals("recordPosition")) {
                String rp = new String(ch, start, length);
                rp = rp.replaceAll("[\r\n]", "");
                if (rp.length() != 0) {
                    if (this.recordPosition != null) {
                        this.recordPosition = this.recordPosition + rp;
                    }
                    else {
                        this.recordPosition = rp;
                    }
                }
            }
        }
        else if (this.elementName.equals("numberOfRecords")) {
            String rn = new String(ch, start, length);
            rn = rn.replaceAll("[\r\n]", "");
            if (rn.length() != 0) {
                if (this.recordsNumber != null) {
                    this.recordsNumber += rn;
                }
                else {
                    this.recordsNumber = rn;
                }
            }
        }
        
    }

    public Vector<String> getIds() {
        return this.resourceIds;
    }
    
    public boolean isFinished() {
        if (recordsNumber.equals("0")) {
            return true;
        } else if (Integer.parseInt(this.recordsNumber) == Integer
                .parseInt(this.recordPosition)) {
            return true;
        } 
        return false;
        
    }

    public String nextRecord() {
        String nextRecordPosition = String.valueOf(Integer
        .parseInt(this.recordPosition) + 1);
        return nextRecordPosition;
    }
    public void resetRecordsNumber() {
        this.recordsNumber = "0";
        
    }
}
