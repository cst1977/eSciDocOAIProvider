package de.fiz.karlsruhe.escidoc.services.oaiprovider.saxhandler;

import java.io.PrintWriter;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;

import de.fiz.karlsruhe.escidoc.services.oaiprovider.EscidocQueryFactory;

public class AllRecordMetadataHandler extends DefaultHandler {
    private static final Logger logger =
        Logger.getLogger(AllRecordMetadataHandler.class.getName());

    private String oaiIdPrefix;

    private String elementName;

    private boolean inElement = false;

    private boolean behindElement = false;

    private String recordsNumber;

    private String recordPosition;

    private String resourceId;

    private String id;

    private String contextId;

    private String ouId;

    boolean newContextElement = true;

    boolean newOuElement = true;

    String setSpecifications;

    String lastModDate = null;

    String latestReleaseDate;

    String resourceType;

    String deleted;

    private Vector<String> organizationalUnits = null;

    private Vector<String> oldSets = null;

    private EscidocQueryFactory queryFactory = null;

    private PrintWriter out;

    public AllRecordMetadataHandler(String namespaceIdentifier,
        Vector<String> oldSetSpecs, EscidocQueryFactory queryFactory,
        PrintWriter out) {
        oaiIdPrefix = "oai:" + namespaceIdentifier + ":";
        this.oldSets = oldSetSpecs;
        this.queryFactory = queryFactory;
        this.out = out;
    }

    public void startElement(
        String uri, String localName, String qName, Attributes attributes) {
    	this.behindElement = false;
        this.elementName = localName;
        if (localName.equals("record")) {
            inElement = true;
            recordPosition = null;
            this.organizationalUnits = new Vector<String>();
            this.setSpecifications = new String();
            this.lastModDate = null;
            this.latestReleaseDate = null;
            this.deleted = null;
            this.resourceType = null;
            resourceId = null;
            id = null;
        }

    }

    public void endElement(String uri, String localName, String qName) {
    	this.behindElement = true;
        if (localName.equals("record")) {
            inElement = false;
            this.resourceId = oaiIdPrefix + this.id;
            if (this.oldSets != null) {
                for (int i = 0; i < this.oldSets.size(); i++) {
                    String setSpec = this.oldSets.get(i);
                    Vector<String> resourceIds =
                        this.queryFactory.retrieveIdsForSetQuery(setSpec);
                    if (resourceIds.contains(this.resourceId)) {
                        if (this.setSpecifications != null) {
                            this.setSpecifications =
                                this.setSpecifications + "," + setSpec;
                        }
                        else {
                            this.setSpecifications = setSpec;
                        }
                    }
                }
            }
            if (out != null) {
                out.print(this.resourceId);
                out.print(" " + this.id);
                out.print(" " + this.lastModDate);
                out.print(" " + this.latestReleaseDate);
                out.print(" " + this.deleted);
                out.print(" " + this.resourceType);
                if (this.setSpecifications != null) {
                    out.print(" " + this.setSpecifications);
                }
                out.println();
            }
        }
        else if (localName.equals("context-id")) {
            newContextElement = true;
            contextId = null;

        }
        else if (localName.equals("organizational-unit-id")) {
            newOuElement = true;
            ouId = null;
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
                    if (this.id != null) {
                        this.id = this.id + id;
                    }
                    else {
                        this.id = id;
                    }

                }
            }
            else if (this.elementName.equals("organizational-unit-id")) {
                String ou = new String(ch, start, length);
                ou = ou.replaceAll("[\r\n]", "");
                if (newOuElement) {
                    ouId = ou;
                }
                else {
                    this.organizationalUnits.remove(ouId);
                    ouId = ouId + ou;
                }
                if ((ouId.length() != 0)
                    && !this.organizationalUnits.contains(ouId)) {
                    this.organizationalUnits.add(ouId);

                    if (this.setSpecifications.length() != 0) {
                        this.setSpecifications =
                            this.setSpecifications + "," + "ou_"
                                + ouId.replaceAll(":", "_");
                    }
                    else {
                        this.setSpecifications =
                            "ou_" + ouId.replaceAll(":", "_");
                    }

                }
                if (newOuElement) {
                    newOuElement = false;
                }
            }
            else if (this.elementName.equals("context-id")) {

                String context = new String(ch, start, length);
                context = context.replaceAll("[\r\n]", "");
                if (newContextElement) {
                    contextId = context;
                }
                else {
                    contextId = contextId + context;
                }
                if (contextId.length() != 0) {
                    if (this.setSpecifications.length() != 0) {
                        this.setSpecifications =
                            this.setSpecifications + "," + "context_"
                                + contextId.replaceAll(":", "_");
                    }
                    else {
                        this.setSpecifications =
                            "context_" + contextId.replaceAll(":", "_");
                    }
                }
                if (newContextElement) {
                    newContextElement = false;
                }

            }
            else if (this.elementName.equals("last-modification-date")) {
                String lmd = new String(ch, start, length);
                lmd = lmd.replaceAll("[\r\n]", "");
                if (lmd.length() != 0) {

                    if (this.lastModDate != null) {
                        this.lastModDate = this.lastModDate + lmd;
                    }
                    else {
                        this.lastModDate = lmd;
                    }
                }

            }
            else if (this.elementName.equals("latest-release-date")) {
                String lrd = new String(ch, start, length);
                lrd = lrd.replaceAll("[\r\n]", "");
                if (lrd.length() != 0) {
                    if (this.latestReleaseDate != null) {
                        this.latestReleaseDate = this.latestReleaseDate + lrd;
                    }
                    else {
                        this.latestReleaseDate = lrd;
                    }
                }

            }
            else if (this.elementName.equals("deleted")) {
                String deleted = new String(ch, start, length);
                deleted = deleted.replaceAll("[\r\n]", "");
                if (deleted.length() != 0) {

                    if (this.deleted != null) {
                        this.deleted = this.deleted + deleted;
                    }
                    else {
                        this.deleted = deleted;
                    }

                }

            }
            else if (this.elementName.equals("resource-type")) {
                String resourceType = new String(ch, start, length);
                resourceType = resourceType.replaceAll("[\r\n]", "");
                if (resourceType.length() != 0) {

                    if (this.resourceType != null) {
                        this.resourceType = this.resourceType + resourceType;
                    }
                    else {
                        this.resourceType = resourceType;
                    }

                }
            }
            else if (this.elementName.equals("recordPosition")) {
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
                    this.recordsNumber = this.recordsNumber + rn;
                }
                else {
                    this.recordsNumber = rn;
                }
            }
        }
    }

    public boolean isFinished() {
        if (recordsNumber.equals("0")) {
            return true;
        }
        else if (Integer.parseInt(this.recordsNumber) == Integer
            .parseInt(this.recordPosition)) {
            return true;
        }
        return false;

    }

    public String getLastModificationDate() {
        return this.lastModDate;
    }

    public String nextRecord() {
        String nextRecordPosition =
            String.valueOf(Integer.parseInt(this.recordPosition) + 1);
        return nextRecordPosition;
    }

    public void resetRecordsNumber() {
        this.recordsNumber = null;

    }
}
