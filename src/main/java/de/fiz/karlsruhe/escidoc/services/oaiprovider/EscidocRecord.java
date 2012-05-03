package de.fiz.karlsruhe.escidoc.services.oaiprovider;

import org.apache.log4j.Logger;

import proai.Record;

/**
 * @author Edwin Shin
 * @author cwilper@cs.cornell.edu
 */
public class EscidocRecord
        implements Record {

    public static Logger logger =
            Logger.getLogger(EscidocRecord.class.getName());

    private String m_itemID;

    private String m_mdPrefix;

    private String m_sourceInfo;
    
    
    public EscidocRecord(String itemID,
                        String resourceId,
                        String mdPrefix,
                        String recordDiss,
                        String date,
                        boolean deleted,
                        String[] setSpecs,
                        String releaseDate,
                        String resourceType) {

        m_itemID = itemID;
        m_mdPrefix = mdPrefix;
        StringBuffer buf = new StringBuffer();
        buf.append(resourceId);
        buf.append(" " + recordDiss);
        buf.append(" " + deleted);
        buf.append(" " + date);
        buf.append(" " + releaseDate);
        buf.append(" " + resourceType);
        for (int i = 0; i < setSpecs.length; i++) {
            buf.append(" " + setSpecs[i]);
        }
        m_sourceInfo = buf.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see proai.Record#getItemID()
     */
    public String getItemID() {
        return m_itemID;
    }
    
    public String getPrefix() {
        return m_mdPrefix;
    }

    public String getSourceInfo() {
        logger.debug("Returning source info line: " + m_sourceInfo);
        return m_sourceInfo;
    }
}
