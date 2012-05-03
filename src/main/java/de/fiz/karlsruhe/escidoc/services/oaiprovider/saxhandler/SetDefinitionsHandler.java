package de.fiz.karlsruhe.escidoc.services.oaiprovider.saxhandler;

import java.util.HashMap;

import org.xml.sax.Attributes;

import de.fiz.karlsruhe.escidoc.services.oaiprovider.EscidocSetInfo;

public class SetDefinitionsHandler extends DefaultHandler {
   
    private boolean behindElement = false;

    private boolean inElement = false;
    private boolean inDescription = false;
    private boolean inName = false;
    private boolean inSpecification = false;
    private boolean inQuery = false;
    
    private String description;
    private String name;
    private String query;
    private String specification;
    private HashMap<String,EscidocSetInfo> sets;

    private EscidocSetInfo set;

    private int recordsNumber;
    
    public SetDefinitionsHandler() {
        this.sets = new HashMap<String,EscidocSetInfo>();
        this.recordsNumber = 0;
    }

   
    public void startElement(
        String uri, String localName, String qName, Attributes attributes) {
    	this.behindElement = false;
        if (localName.equals("set-definition")) {
            inElement = true; 
            recordsNumber++;
        }
        if (inElement) {
            if (localName.equals("description")) {
                inDescription = true;   
               } else if (localName.equals("name")) {
                   inName = true;
               } else if (localName.equals("specification")) {
                   inSpecification = true;
               } else if (localName.equals("query")) {
                   inQuery = true;
               }
        }
    }

    public void endElement(String uri, String localName, String qName) {
    	this.behindElement = true;
        if (inElement) {
            if (localName.equals("description")) {
                inDescription = false;   
               } else if (localName.equals("name")) {
                   inName = false;
               } else if (localName.equals("specification")) {
                   inSpecification = false;
               } else if (localName.equals("query")) {
                   inQuery = false;
               }
        }
        if (localName.equals("set-definition")) {
            inElement = false;
            
            set = new EscidocSetInfo(this.specification, this.name,
                this.description, this.query);
            if (sets == null) {
             sets = new HashMap<String,EscidocSetInfo>(); 
            }
            sets.put(this.specification, set);
            this.specification = null;
            this.name = null;
            this.description = null;
            this.query = null;
            this.set = null;
        } 
    }

    public void characters(char[] ch, int start, int length) {
        
    	if (behindElement) {
    		return;
    	}
        if (inElement) {
            if (inDescription) {
                if (this.description == null) {
                    this.description = new String(ch, start, length);
                }  else {
                    this.description = this.description + new String(ch, start, length);
                }
            } else if (inSpecification) {
                if (this.specification == null) {
                    this.specification = new String(ch, start, length);
                }  else {
                    this.specification = this.specification + new String(ch, start, length);
                }
            } else if (inQuery) {
                if (this.query == null) {
                    this.query = new String(ch, start, length);
                }  else {
                    this.query = this.query + new String(ch, start, length);
                }
            } else if (inName) {
                if (this.name == null) {
                    this.name = new String(ch, start, length);
                }  else {
                    this.name = this.name + new String(ch, start, length);
                }
            }
            
        }
    }

    public HashMap<String,EscidocSetInfo> getData() {
        return this.sets;
    }

    public int getRecordNumber() {
        return this.recordsNumber;
    }
    
    public void resetRecordsNumber(){
        this.recordsNumber = 0;
    }
}
