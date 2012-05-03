package de.fiz.karlsruhe.escidoc.services.oaiprovider;

import de.fiz.karlsruhe.cache.MetadataValidator;
import de.fiz.karlsruhe.cache.ValidationInfo;
import de.fiz.karlsruhe.cache.ValidationResult;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import proai.MetadataFormat;
import proai.Record;
import proai.SetInfo;
import proai.driver.OAIDriver;
import proai.driver.RemoteIterator;
import proai.driver.impl.RemoteIteratorImpl;
import proai.error.RepositoryException;

public class EscidocOAIDriver implements OAIDriver {

    private String DC_SCHEMALOCATION;
    private String DC_NAMESPACEURI;
    private static final String XSI_DECLARATION =
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"";
    private static final Pattern PATTERN__XSI_DECLARATION =
            Pattern.compile(
            ".*xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\".*",
            Pattern.DOTALL);
    private static final Logger logger =
            Logger.getLogger(EscidocOAIDriver.class.getName());
    public static final String NS = "driver.escidoc.";
    public static final String PROP_BASEURL = NS + "baseURL";
    public static final String PROP_SEARCH_BASEURL = NS + "search.baseURL";
    public static final String PROP_FORMATS = NS + "md.formats";
    public static final String PROP_NAMESPACE_IDENTIFIER =
            NS + "namespace-identifier";
    public static final String PROP_FORMAT_START = NS + "md.format.";
    public static final String PROP_DELETED = NS + "deleted";
    public static final String PROP_FORMAT_PFX_END = ".mdPrefix";
    public static final String PROP_FORMAT_LOC_END = ".loc";
    public static final String PROP_FORMAT_URI_END = ".uri";
    public static final String PROP_FORMAT_DISSTYPE_END = ".dissType";
    private EscidocQueryFactory m_queryFactory;
    private String m_escidocBaseURL;
    private String m_escidocSearchBaseURL;
    private String m_namespace_identifier;
    private Map<String, EscidocMetadataFormat> m_metadataFormats;
    private MetadataValidator _validator;

    public EscidocOAIDriver() {
    }

    // ////////////////////////////////////////////////////////////////////////
    // /////////////////// Methods from proai.driver.OAIDriver ////////////////
    // ////////////////////////////////////////////////////////////////////////
    public void init(Properties props) throws RepositoryException {

        DC_SCHEMALOCATION = getRequired(props, "driver.escidoc.md.format.oai_dc.loc");
        DC_NAMESPACEURI = getRequired(props, "driver.escidoc.md.format.oai_dc.uri");
        m_escidocBaseURL = getRequired(props, PROP_BASEURL);
        m_namespace_identifier = getRequired(props, PROP_NAMESPACE_IDENTIFIER);
        if (!m_escidocBaseURL.endsWith("/")) {
            m_escidocBaseURL += "/";
        }
        m_escidocSearchBaseURL = getRequired(props, PROP_SEARCH_BASEURL);
        if (!m_escidocSearchBaseURL.endsWith("/")) {
            m_escidocSearchBaseURL += "/";
        }
        m_metadataFormats = getMetadataFormats(props);
        m_queryFactory = new EscidocQueryFactory();
        m_queryFactory.init(m_escidocBaseURL, m_escidocSearchBaseURL,
                m_namespace_identifier);
        _validator = new MetadataValidator();
        _validator.init(listMetadataFormats());
    }

    protected static String getRequired(Properties props, String key)
            throws RepositoryException {
        String val = props.getProperty(key);
        if (val == null) {
            throw new RepositoryException("Required property is not set: "
                    + key);
        }
        logger.debug("Required property: " + key + " = " + val);
        return val.trim();
    }

    private Map<String, EscidocMetadataFormat> getMetadataFormats(
            Properties props) throws RepositoryException {
        String formats[], prefix, namespaceURI, schemaLocation;
        EscidocMetadataFormat mf;
        Map<String, EscidocMetadataFormat> map =
                new HashMap<String, EscidocMetadataFormat>();

        // step through formats, getting appropriate properties for each
        formats = getRequired(props, PROP_FORMATS).split(" ");
        for (int i = 0; i < formats.length; i++) {
            prefix = formats[i];
            namespaceURI =
                    getRequired(props, PROP_FORMAT_START + prefix
                    + PROP_FORMAT_URI_END);
            schemaLocation =
                    getRequired(props, PROP_FORMAT_START + prefix
                    + PROP_FORMAT_LOC_END);

            String otherPrefix =
                    props.getProperty(PROP_FORMAT_START + prefix
                    + PROP_FORMAT_PFX_END);
            if (otherPrefix != null) {
                prefix = otherPrefix;
            }

            String mdDissType =
                    PROP_FORMAT_START + prefix + PROP_FORMAT_DISSTYPE_END;

            if (prefix.equals("oai_dc")) {
                namespaceURI = DC_NAMESPACEURI;
                schemaLocation = DC_SCHEMALOCATION;
            }
            mf =
                    new EscidocMetadataFormat(prefix, namespaceURI, schemaLocation,
                    getRequired(props, mdDissType));
            map.put(prefix, mf);
        }
        if (!map.containsKey("oai_dc")) {
            logger.warn("oai_dc format is missing in the configuration file. Specifed oai_dc format"
                    + " with a dissemination type 'DC'");
            EscidocMetadataFormat dcFormat =
                    new EscidocMetadataFormat("oai_dc", DC_NAMESPACEURI,
                    DC_SCHEMALOCATION, "DC");
            map.put("oai_dc", dcFormat);
        }
        return map;
    }

    @Override
    public void write(PrintWriter out) throws RepositoryException {
        String identity = m_queryFactory.retrieveIndentity();
        out.print(identity);
    }

    @Override
    public Date getLatestDate() throws RepositoryException {
        return m_queryFactory.latestRecordDate();
    }

    @Override
    public RemoteIterator<? extends MetadataFormat> listMetadataFormats() throws RepositoryException {
        return new RemoteIteratorImpl<EscidocMetadataFormat>(m_metadataFormats.values().iterator());
    }

    @Override
    public RemoteIterator<? extends SetInfo> listSetInfo() throws RepositoryException {
        return m_queryFactory.listSetInfo();
    }

    @Override
    public RemoteIterator<? extends Record> listRecords(Date from, Date until, String mdPrefix)
            throws RepositoryException {

        if (from != null && until != null && from.after(until)) {
            throw new RepositoryException(
                    "from date cannot be later than until date.");
        }

        return m_queryFactory.listRecords(from, until, m_metadataFormats.get(mdPrefix), new HashSet<String>());
    }

    @Override
    public void writeRecordXML(String itemID, String mdPrefix, String sourceInfo, PrintWriter out)
            throws RepositoryException {
        // Parse the sourceInfo string
        String[] parts = sourceInfo.trim().split(" ");
        if (parts.length < 6) {
            throw new RepositoryException(
                    "Error parsing sourceInfo (expecting " + "6 or more parts): '"
                    + sourceInfo + "'");
        }
        String resourceId = parts[0];
        String dissURI = parts[1];
        boolean deleted = parts[2].equalsIgnoreCase("true");
        String date = parts[3];
        String releaseDate = parts[4];
        String resourceType = parts[5];
        out.println("<record xmlns=\"http://www.openarchives.org/OAI/2.0/\">");
//        if (deleted) {
//            writeRecordHeader(itemID, deleted, date, out);
//        } else {
//            writeRecordMetadata(resourceId, dissURI, resourceType, mdPrefix, out);
//            writeRecordHeader(itemID, deleted, releaseDate, out);
//        }
        if (deleted) {
            writeRecordHeader(itemID, deleted, date, out);
        } else {
            writeRecordHeader(itemID, deleted, releaseDate, out);
        }
        if (!deleted) {
            writeRecordMetadata(resourceId, dissURI, resourceType,
                    mdPrefix, out);

        } else {
            logger.info("Record was marked deleted: " + itemID + "/" + mdPrefix);
        }
        out.println("</record>");
    }

    private static void writeRecordHeader(
            String itemID, boolean deleted, String date, PrintWriter out) {
        if (deleted) {
            out.println("  <header status=\"deleted\">");
        } else {
            out.println("  <header>");
        }
        out.println("    <identifier>" + itemID + "</identifier>");
        DateTime dateTime = new DateTime(date);
        DateTime newDate = dateTime.withZone(DateTimeZone.UTC);
        String dateString = newDate.toString("yyyy-MM-dd'T'HH:mm:ss'Z'");
        out.println("    <datestamp>" + dateString + "</datestamp>");
        out.println("  </header>");
    }

    private void writeRecordMetadata(
            String resourceId, String dissURI, String resourceType,
            String mdPrefix, PrintWriter out) throws RepositoryException {
        ValidationInfo validationInfo = new ValidationInfo();
        GetMethod getWithMdRecordContent = null;
        if (dissURI.equals("DC")) {
            getWithMdRecordContent =
                    EscidocConnector.requestRetrieveDc(resourceId, resourceType);
        } else if (dissURI.startsWith("resources")) {
            getWithMdRecordContent =
                    EscidocConnector.requestRetrieveResource(resourceId,
                    resourceType, dissURI);
        } else {
            getWithMdRecordContent =
                    EscidocConnector.requestRetrieveMdRecord(resourceId,
                    resourceType, dissURI);
        }
        InputStream in = null;
        BufferedReader reader = null;
        try {
            in = getWithMdRecordContent.getResponseBodyAsStream();
            if (in == null) {
                throw new RepositoryException(
                        "Body content of a GET-request is null " + resourceId
                        + " md-prefix: " + mdPrefix);
            }
            // FIXME use xml reader for reading xml, charset of HTTP response
            // might not be charset of XML document (https://www.escidoc.org/jira/browse/INFR-930)
            String charset = getWithMdRecordContent.getResponseCharSet();
            reader = new BufferedReader(new InputStreamReader(in, charset));
            StringBuffer buf = new StringBuffer();
            String line = reader.readLine();
            while (line != null) {
                buf.append(line + "\n");
                line = reader.readLine();
            }
            String xml = buf.toString();
            validationInfo = _validator.validate(mdPrefix, xml);
            xml = xml.replaceAll("\\s*<\\?xml.*?\\?>\\s*", "");
            out.println("  <metadata>");
            out.print(xml);
            out.println("  </metadata>");
        } catch (IOException e) {
            throw new RepositoryException("IO error reading " + dissURI, e);
        } finally {
            if (reader != null) {
                try {
                    getWithMdRecordContent.releaseConnection();
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
    }

    @Override
    public void close() throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}