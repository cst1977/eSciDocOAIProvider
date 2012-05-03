package de.fiz.karlsruhe.cache;

import javax.xml.XMLConstants;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class SchemaHandler extends DefaultHandler {

    private String targetNamespace = null;

    public SchemaHandler() {
    }

    public void startElement(
            String uri, String localName, String qName, Attributes attributes) {
        if (localName.equals("schema") && uri.equals(XMLConstants.W3C_XML_SCHEMA_NS_URI)) {
            int indexOfTargetNamespace = attributes.getIndex("targetNamespace");
            if (indexOfTargetNamespace != -1) {
                this.targetNamespace = attributes.getValue(indexOfTargetNamespace);
            }
        }

    }

    public String getTargetNamespace() {
        return this.targetNamespace;
    }
}
