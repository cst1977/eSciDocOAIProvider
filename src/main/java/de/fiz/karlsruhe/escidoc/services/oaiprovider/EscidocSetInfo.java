package de.fiz.karlsruhe.escidoc.services.oaiprovider;

import java.io.PrintWriter;

import proai.SetInfo;
import proai.error.RepositoryException;

/**
 * SetInfo impl that includes setDescription elements for setDiss dissemination,
 * if provided + available.
 */
public class EscidocSetInfo
        implements SetInfo {

    
    private final String m_setSpec;

    private final String m_setName;

    private final String m_setDescription;
    
    private final String m_setQuery;
    

 
    public EscidocSetInfo(String setSpec,
                         String setName,
                         String setDescription,
                         String setQuery) {
       
        m_setSpec = setSpec;
        m_setName = setName;
        m_setDescription = setDescription;
        m_setQuery = setQuery;
    }

    public String getSetSpec() {
        return m_setSpec;
    }
    
    public String getSetQuery() {
        return m_setQuery;
    }

    public void write(PrintWriter out) throws RepositoryException {
        out.println("<set>");
        out.println("  <setSpec>" + m_setSpec + "</setSpec>");
        out
                .println("  <setName>" + StreamUtility.enc(m_setName)
                        + "</setName>");
        writeDescriptions(out);
        out.println("</set>");
    }

    private void writeDescriptions(PrintWriter out) throws RepositoryException {
        if (this.m_setDescription != null && (this.m_setDescription.length() > 0)) {
            out.println("<setDescription>");
            out.println("<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"");
            out.println(" xmlns:dc=\"http://purl.org/dc/elements/1.1/\""); 
            out.println(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""); 
            out.println(" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/"); 
            out.println(" http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">");
            out.println("<dc:description> " + StreamUtility.enc(m_setDescription) + "</dc:description>");
            out.println("</oai_dc:dc>");
            out.println("</setDescription>");
        }
    }

}
