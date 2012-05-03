package de.fiz.karlsruhe.escidoc.services.oaiprovider.saxhandler;

import java.util.Vector;

import org.xml.sax.Attributes;

import proai.SetInfo;
import de.fiz.karlsruhe.escidoc.services.oaiprovider.EscidocSetInfo;

public class OuOrContextListHandler extends DefaultHandler {
    private String elementName;

    private boolean behindElement = false;

    private boolean inElement = false;

    private static String XLINK_NAMESPACE_URI = "http://www.w3.org/1999/xlink";
    
    private static String CONTEXT_URI_PREFIX="http://www.escidoc.de/schemas/context";
    
    private static String OU_URI_PREFIX="http://www.escidoc.de/schemas/organizationalunit";
    
    private Vector<SetInfo> sets;

    private SetInfo setInfo;

    private String resourceId;

    private String title;

    private String description;

    private int numberOfRecords = 0;

    private int numberOfOuRecords = 0;

    private int numberOfContextRecords = 0;

    private int lastContextRecordNumber = 0;
    
    private int lastOuRecordNumber = 0;
    
    private boolean inDescription = false;
    
    private boolean inNumberOfRecords = false;
    
    public OuOrContextListHandler() {
       this.sets = new Vector<SetInfo>();
    }

    public OuOrContextListHandler(final Vector<SetInfo> sets) {
        this.sets = sets;
    }

    public void startElement(
        String uri, String localName, String qName, Attributes attributes) {
    	this.behindElement = false;
        this.elementName = qName;
        if (localName.equals("numberOfRecords")) {
            inNumberOfRecords = true;
        }
        if ((localName.equals("organizational-unit") && uri.startsWith(OU_URI_PREFIX)) 
            || (localName.equals("context")) && (uri.startsWith(CONTEXT_URI_PREFIX))) {
            if (localName.equals("organizational-unit")) {
                lastOuRecordNumber++;
            }
            else if (localName.equals("context")) {
                lastContextRecordNumber++;
            }
            
            inElement = true;
            
            int indexTitle = attributes.getIndex(XLINK_NAMESPACE_URI, "title");

            if (indexTitle != -1) {
                this.title = attributes.getValue(indexTitle);
            }
            int indexHref = attributes.getIndex(XLINK_NAMESPACE_URI, "href");

            if (indexTitle != -1) {
                String href = attributes.getValue(indexHref);
                int index = href.lastIndexOf("/");
                if (index != -1) {
                    this.resourceId = href.substring(href.lastIndexOf("/") + 1);
                }
            }

        }
        if (inElement) {
            if (this.elementName.equals("prop:description")) {
                inDescription = true;   
               }  
        }
    }

    public void endElement(String uri, String localName, String qName) {
    	this.behindElement = true;
        if (inElement) {
            if (this.elementName.equals("prop:description")) {
                inDescription = false;   
               }  
        }
        if (inNumberOfRecords && localName.equals("numberOfRecords")) {
        	inNumberOfRecords = false;
        }
        if ((localName.equals("organizational-unit") && uri.startsWith(OU_URI_PREFIX)) 
            || (localName.equals("context")) && (uri.startsWith(CONTEXT_URI_PREFIX))) {
            
            inElement = false;
            String setSpecPrefix = null;
            if (localName.equals("organizational-unit")) {
                setSpecPrefix = "ou_";
                numberOfOuRecords = numberOfRecords;
            }
            else if (localName.equals("context")) {
                setSpecPrefix = "context_";
                numberOfContextRecords = numberOfRecords;
            }
            this.setInfo =
                new EscidocSetInfo(setSpecPrefix + this.resourceId.replaceAll(":", "_"), this.title,
                    this.description, null);
            if (sets == null) {
                sets = new Vector<SetInfo>();
            }
            sets.add(this.setInfo);
            setInfo = null;
            resourceId = null;
            title = null;
            description = null;
        }
        
    }

    public void characters(char[] ch, int start, int length) {
    	if (behindElement) {
    		return;
    	}
        if (inElement && inDescription) {
            if (this.description == null) {
                this.description = new String(ch, start, length);
            }  else {
                this.description = this.description + new String(ch, start, length);
            }
        }
        if (inNumberOfRecords) {
            this.numberOfRecords = Integer.parseInt(
    				new String(ch, start, length));
        }
    }

    public Vector<SetInfo> getData() {
        return this.sets;
    }

    public boolean isOuListFinished() {
        if (this.numberOfOuRecords > this.lastOuRecordNumber) {
            return false;
        }
        return true;
    }

    public boolean isContextListFinished() {
        if (this.numberOfContextRecords > this.lastContextRecordNumber) {
            return false;
        }
        return true;
    }

    public String nextOuNumber() {
        return String.valueOf(this.lastOuRecordNumber);
    }

    public String nextContextNumber() {
        return String.valueOf(this.lastContextRecordNumber);
    }
}
