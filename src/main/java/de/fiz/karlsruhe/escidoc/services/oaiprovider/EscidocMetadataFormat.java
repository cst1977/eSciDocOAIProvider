package de.fiz.karlsruhe.escidoc.services.oaiprovider;

import proai.MetadataFormat;

/**
 * @author ROF
 */
public class EscidocMetadataFormat implements MetadataFormat {

    private final String m_prefix;
    private final String m_namespaceURI;
    private final String m_schemaLocation;
    private final String m_mdDissemination;

    public EscidocMetadataFormat(String prefix,
            String namespaceURI,
            String schemaLocation,
            String mdDissemination) {
        m_prefix = prefix;
        m_namespaceURI = namespaceURI;
        m_schemaLocation = schemaLocation;
        m_mdDissemination = mdDissemination;
    }

    public String getPrefix() {
        return m_prefix;
    }

    public String getNamespaceURI() {
        return m_namespaceURI;
    }

    public String getSchemaLocation() {
        return m_schemaLocation;
    }

    public String getDissemination() {
        return m_mdDissemination;
    }
}
