package de.fiz.karlsruhe.escidoc.services.oaiprovider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import proai.SetInfo;
import proai.driver.RemoteIterator;
import proai.error.RepositoryException;
import de.fiz.karlsruhe.escidoc.services.oaiprovider.saxhandler.AllRecordMetadataHandler;
import de.fiz.karlsruhe.escidoc.services.oaiprovider.saxhandler.IdentifyHandler;
import de.fiz.karlsruhe.escidoc.services.oaiprovider.saxhandler.OuOrContextListHandler;
import de.fiz.karlsruhe.escidoc.services.oaiprovider.saxhandler.SetDefinitionsHandler;
import de.fiz.karlsruhe.escidoc.services.oaiprovider.saxhandler.SetMembersIdsHandler;
import fedora.common.Constants;
import fedora.server.utilities.DateUtility;

/**
 * Directly queries an escidoc and escidoc SRW service
 * 
 * @author ROF
 */
public class EscidocQueryFactory implements Constants {

    private static final Logger logger =
        Logger.getLogger(EscidocQueryFactory.class.getName());

    private static final Date ONE_BCE = new Date(-62167392000000L);

    private static final Date ONE_CE = new Date(-62135769600000L);

    private String m_escidocBaseURL;

    private String m_escidocSearchBaseURL;

    private String m_namespace_identifier;

    private SAXParserFactory saxParserFactory;

    private HashMap<String, Vector<String>> searchHitLists =
        new HashMap<String, Vector<String>>();

    private HashMap<String, SetInfo> setDefinitions =
        new HashMap<String, SetInfo>();

    public void init(
        String baseUrl, String searchBaseUrl, String namespaceIdentifier) {
        m_namespace_identifier = namespaceIdentifier;
        m_escidocBaseURL = baseUrl;
        m_escidocSearchBaseURL = searchBaseUrl;
        saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setValidating(false);
        saxParserFactory.setNamespaceAware(true);
        EscidocConnector.init(m_escidocBaseURL, m_escidocSearchBaseURL);
    }

    public Date latestRecordDate() throws RepositoryException {

        GetMethod getWithLmd = EscidocConnector.requestSearchLmdQuery();
        InputStream inputLmd = null;
        try {
            inputLmd = getWithLmd.getResponseBodyAsStream();
        }
        catch (IOException e1) {
            logger.error(e1);
            throw new RepositoryException(e1.getMessage(), e1);
        }
        logger.debug("getting latest record date");
        AllRecordMetadataHandler dh =
            new AllRecordMetadataHandler(m_namespace_identifier, null, this,
                null);
        SAXParser parser = null;

        try {
            parser = saxParserFactory.newSAXParser();
        }
        catch (ParserConfigurationException e) {
            logger.error(e);
            throw new RepositoryException(e.getMessage(), e);
        }
        catch (SAXException e) {
            logger.error(e);
            throw new RepositoryException(e.getMessage(), e);
        }
        try {
            parser.parse(inputLmd, dh);
            getWithLmd.releaseConnection();
        }
        catch (SAXException e) {
            logger.error(e);
            throw new RepositoryException(e.getMessage(), e);
        }
        catch (IOException e) {
            logger.error(e);
            throw new RepositoryException(e.getMessage(), e);
        }
        String lmd = dh.getLastModificationDate();
        return DateUtility.convertStringToDate(lmd);

    }

    public RemoteIterator<EscidocRecord> listRecords(
        Date from, Date until, EscidocMetadataFormat format,
        Set<String> newSetSpecs) throws RepositoryException {
        String fromString = convertDateToString(new Date(from.getTime() - 1));
        String untilString = convertDateToString(new Date(until.getTime() + 1));
        String mdRecordName = format.getDissemination();
        String mdUri = format.getNamespaceURI();

        Set<String> userDefinedSetKeys = this.setDefinitions.keySet();
        Iterator<String> it = userDefinedSetKeys.iterator();
        Vector<String> oldSetSpecs = new Vector<String>();
        while (it.hasNext()) {
            String setSpez = it.next();
            if (!newSetSpecs.contains(setSpez)) {
                oldSetSpecs.add(setSpez);
            }
        }
        File tempFile = null;
        OutputStream fos = null;
        try {
            tempFile = File.createTempFile("oaiprovider_record_list", ".tmp");
            tempFile.deleteOnExit(); // just in case
            fos = new FileOutputStream(tempFile);
        }
        catch (IOException e) {
            throw new RepositoryException(
                "Error creating temp record list file", e);
        }
        PrintWriter out = new PrintWriter(new OutputStreamWriter(fos));

        GetMethod getWithInputReleased = null;
        if (!format.getDissemination().equals("DC")) {
            getWithInputReleased =
                EscidocConnector.requestSearchQueryReleased(mdRecordName,
                    mdUri, fromString, untilString, "1");
        }
        else {
            getWithInputReleased =
                EscidocConnector.requestSearchQueryDcReleased(fromString,
                    untilString, "1");
        }
        InputStream inputReleased = null;
        try {
            inputReleased = getWithInputReleased.getResponseBodyAsStream();
        }
        catch (IOException e1) {
            logger.error(e1);
            throw new RepositoryException(e1.getMessage(), e1);
        }

        AllRecordMetadataHandler dh =
            new AllRecordMetadataHandler(m_namespace_identifier, oldSetSpecs,
                this, out);

        SAXParser parser = null;

        try {
            parser = saxParserFactory.newSAXParser();
        }
        catch (ParserConfigurationException e) {
            logger.error(e);
            throw new RepositoryException(e.getMessage(), e);
        }
        catch (SAXException e) {
            logger.error(e);
            throw new RepositoryException(e.getMessage(), e);
        }
        try {
            // parse all pages of search query result for released resources,
            // then parse all pages of search query result for withdrawn
            // resources
            // put all fetched data into a common Map

            parser.parse(inputReleased, dh);
            getWithInputReleased.releaseConnection();
            inputReleased.close();
            while (!dh.isFinished()) {
                String nextRecord = dh.nextRecord();
                if (!format.getDissemination().equals("DC")) {
                    getWithInputReleased =
                        EscidocConnector.requestSearchQueryReleased(
                            mdRecordName, mdUri, fromString, untilString,
                            nextRecord);
                }
                else {
                    getWithInputReleased =
                        EscidocConnector.requestSearchQueryDcReleased(
                            fromString, untilString, nextRecord);
                }
                inputReleased = getWithInputReleased.getResponseBodyAsStream();
                dh.resetRecordsNumber();
                parser.parse(inputReleased, dh);
                getWithInputReleased.releaseConnection();
                inputReleased.close();
            }
            GetMethod getWithInputWithdrawn = null;
            if (!format.getDissemination().equals("DC")) {
                getWithInputWithdrawn =
                    EscidocConnector.requestSearchQueryWithdrawn(mdRecordName,
                        mdUri, fromString, untilString, "1");
            }
            else {
                getWithInputWithdrawn =
                    EscidocConnector.requestSearchQueryDcWithdrawn(fromString,
                        untilString, "1");
            }
            InputStream inputWithdrawn = null;
            try {
                inputWithdrawn =
                    getWithInputWithdrawn.getResponseBodyAsStream();
            }
            catch (IOException e1) {
                logger.error(e1);
                throw new RepositoryException(e1.getMessage(), e1);
            }
            dh.resetRecordsNumber();
            parser.parse(inputWithdrawn, dh);
            getWithInputWithdrawn.releaseConnection();
            inputWithdrawn.close();
            while (!dh.isFinished()) {
                String nextRecord = dh.nextRecord();
                if (!format.getDissemination().equals("DC")) {
                    getWithInputWithdrawn =
                        EscidocConnector.requestSearchQueryWithdrawn(
                            mdRecordName, mdUri, fromString, untilString,
                            nextRecord);
                }
                else {
                    getWithInputWithdrawn =
                        EscidocConnector.requestSearchQueryDcWithdrawn(
                            fromString, untilString, nextRecord);
                }
                inputWithdrawn =
                    getWithInputWithdrawn.getResponseBodyAsStream();
                dh.resetRecordsNumber();
                parser.parse(inputWithdrawn, dh);
                getWithInputWithdrawn.releaseConnection();
                inputWithdrawn.close();
            }
            out.close();
        }
        catch (SAXException e) {
            logger.error(e);
            throw new RepositoryException(e.getMessage(), e);
        }
        catch (IOException e) {
            logger.error(e);
            throw new RepositoryException(e.getMessage(), e);
        }
        try {
            RecordsReader reader = new RecordsReader(tempFile, true);
            return new EscidocResourceIterator(format, reader);

        }
        catch (FileNotFoundException e) {
            throw new RepositoryException(
                "Programmer error?  Search queries result " + "file not found!");
        }

    }

    public Vector<String> retrieveIdsForSetQuery(
        String setSpecification, String setQuery) throws RepositoryException {
        SetMembersIdsHandler dh =
            new SetMembersIdsHandler(m_namespace_identifier);

        GetMethod getWithInputMemberIds =
            EscidocConnector.requestSearchFilterQuery(setQuery, "1");

        InputStream inputMemberIds = null;
        try {
            inputMemberIds = getWithInputMemberIds.getResponseBodyAsStream();
        }
        catch (IOException e) {
            logger.error(e);
            throw new RepositoryException(e.getMessage(), e);

        }

        SAXParser parser = null;
        try {
            parser = saxParserFactory.newSAXParser();
            parser.parse(inputMemberIds, dh);
            getWithInputMemberIds.releaseConnection();
            inputMemberIds.close();
            while (!dh.isFinished()) {
                String nextRecord = dh.nextRecord();
                getWithInputMemberIds =
                    EscidocConnector.requestSearchFilterQuery(setQuery,
                        nextRecord);
                inputMemberIds =
                    getWithInputMemberIds.getResponseBodyAsStream();
                parser.parse(inputMemberIds, dh);
                dh.resetRecordsNumber();
                getWithInputMemberIds.releaseConnection();
                inputMemberIds.close();
            }

        }
        catch (SAXException e) {
            logger.error(e);
            throw new RepositoryException(e.getMessage(), e);
        }
        catch (IOException e) {
            logger.error(e);
            throw new RepositoryException(e.getMessage(), e);
        }
        catch (ParserConfigurationException e) {
            logger.error(e);
            throw new RepositoryException(e.getMessage(), e);
        }
        Vector<String> resourceIds = dh.getIds();

        return resourceIds;

    }

    public Vector<String> retrieveIdsForSetQuery(String setSpecification)
        throws RepositoryException {
        return this.searchHitLists.get(setSpecification);
    }

    public String retrieveIndentity() throws RepositoryException {
        GetMethod getWithIdentify = EscidocConnector.requestIdentify();
        InputStream inputIdentify = null;
        try {
            inputIdentify = getWithIdentify.getResponseBodyAsStream();
        }
        catch (IOException e) {
            logger.error(e);
            throw new RepositoryException(e.getMessage(), e);

        }
        
        SAXParser parser = null;
        IdentifyHandler ih = new IdentifyHandler();
        try {
            parser = this.saxParserFactory.newSAXParser();
        }
        catch (ParserConfigurationException e1) {
            logger.error(e1);
            throw new RepositoryException(e1.getMessage(), e1);
        }
        catch (SAXException e1) {
            logger.error(e1);
            throw new RepositoryException(e1.getMessage(), e1);
        }
        try {
            parser.parse(inputIdentify, ih);
            getWithIdentify.releaseConnection();
            inputIdentify.close();
        }
        catch (SAXException e1) {
            logger.error(e1);
            throw new RepositoryException(e1.getMessage(), e1);
        }
        catch (IOException e1) {
            logger.error(e1);
            throw new RepositoryException(e1.getMessage(), e1);
        }
        String repositoryName = ih.getName();
        String adminEmail = ih.getEmail();
        String earliestDate = ih.getEarliestDate();
        String baseUrl = ih.getBaseUrl();
        StringBuffer identityBuffer = new StringBuffer();
        identityBuffer.append("<Identify>\n");
        if (repositoryName != null) {
            identityBuffer.append("  <repositoryName>");
            identityBuffer.append(repositoryName);
            identityBuffer.append("</repositoryName>\n");
        }
        else {
            String message = "Repository name is missing in Identify response.";
            throw new RepositoryException(message);
        }
        if (baseUrl != null) {
            identityBuffer.append("  <baseURL>");
            identityBuffer.append(baseUrl);
            identityBuffer.append("</baseURL>\n");
        }
        else {
            String message =
                "Repository base url is missing in Identify response.";
            throw new RepositoryException(message);
        }
        identityBuffer.append("  <protocolVersion>2.0</protocolVersion>\n");
        if (adminEmail != null) {
            identityBuffer.append("  <adminEmail>");
            identityBuffer.append(adminEmail);
            identityBuffer.append("</adminEmail>\n");
        }
        else {
            String message =
                "Repository admin email is missing in Identify response.";
            throw new RepositoryException(message);
        }
        if (earliestDate != null) {
            identityBuffer.append("  <earliestDatestamp>");
            identityBuffer.append(earliestDate);
            identityBuffer.append("</earliestDatestamp>\n");
        }
        else {
            String message =
                "Repository earliestDatestamp  is missing in Identify response.";
            throw new RepositoryException(message);
        }
        identityBuffer.append("  <deletedRecord>persistent</deletedRecord>\n");
        identityBuffer
            .append("<granularity>YYYY-MM-DDThh:mm:ssZ</granularity>\n");
        identityBuffer.append("</Identify>");
        return identityBuffer.toString();
    }

    public RemoteIterator<SetInfo> listSetInfo() throws RepositoryException {

        GetMethod getWithInputOuList =
            EscidocConnector.requestRetrieveOrganizationalUnits("0");
        OuOrContextListHandler listHandler = new OuOrContextListHandler();
        InputStream inputOuList;
        try {
            inputOuList = getWithInputOuList.getResponseBodyAsStream();
        }
        catch (IOException e1) {
            logger.error(e1);
            throw new RepositoryException(e1.getMessage(), e1);
        }

        SAXParser parser = null;

        Vector<SetInfo> setInfos = null;

        try {
            parser = this.saxParserFactory.newSAXParser();
        }
        catch (ParserConfigurationException e1) {
            logger.error(e1);
            throw new RepositoryException(e1.getMessage(), e1);
        }
        catch (SAXException e1) {
            logger.error(e1);
            throw new RepositoryException(e1.getMessage(), e1);
        }
        try {
            // parse all pages of ou list query result
            // then parse all pages of context list query result
            // put all fetched data into a common Map

            parser.parse(inputOuList, listHandler);
            getWithInputOuList.releaseConnection();
            inputOuList.close();
            while (!listHandler.isOuListFinished()) {
                String offset = listHandler.nextOuNumber();
                getWithInputOuList =
                    EscidocConnector.requestRetrieveOrganizationalUnits(offset);
                inputOuList = getWithInputOuList.getResponseBodyAsStream();
                parser.parse(inputOuList, listHandler);
                getWithInputOuList.releaseConnection();
                inputOuList.close();

            }
            GetMethod getWithInputContextList =
                EscidocConnector.requestRetrieveContexts("0");
            InputStream inputContextList;
            try {
                inputContextList =
                    getWithInputContextList.getResponseBodyAsStream();
            }
            catch (IOException e1) {
                logger.error(e1);
                throw new RepositoryException(e1.getMessage(), e1);
            }
            parser.parse(inputContextList, listHandler);

            getWithInputContextList.releaseConnection();
            inputContextList.close();
            while (!listHandler.isContextListFinished()) {
                String offset = listHandler.nextContextNumber();
                getWithInputContextList =
                    EscidocConnector.requestRetrieveContexts(offset);
                inputContextList =
                    getWithInputContextList.getResponseBodyAsStream();
                parser.parse(inputContextList, listHandler);
                getWithInputContextList.releaseConnection();
                inputContextList.close();
            }
            setInfos = listHandler.getData();

        }
        catch (SAXException e1) {
            logger.error(e1);
            throw new RepositoryException(e1.getMessage(), e1);
        }
        catch (IOException e1) {
            logger.error(e1);
            throw new RepositoryException(e1.getMessage(), e1);
        }
        HashMap<String, SetInfo> expliciteSetInfosMap =
            retrieveUserDefinedSetList(false);
        Collection<SetInfo> expliciteSetInfos = expliciteSetInfosMap.values();
        setInfos.addAll(expliciteSetInfos);
        return new EscidocSetInfoIterator(setInfos);

    }

    public HashMap<String, SetInfo> retrieveUserDefinedSetList(
        boolean updateStart) throws RepositoryException {
        if (updateStart) {
            GetMethod getWithInputSetDefinitions = null;
            getWithInputSetDefinitions =
                EscidocConnector.requestRetrieveSetDefinitions("0");

            InputStream inputSetDefinitions = null;
            try {
                inputSetDefinitions =
                    getWithInputSetDefinitions.getResponseBodyAsStream();
            }
            catch (IOException e1) {
                logger.error(e1);
                throw new RepositoryException(e1.getMessage(), e1);
            }

            SetDefinitionsHandler sdh = new SetDefinitionsHandler();

            SAXParser parser = null;
            try {
                parser = saxParserFactory.newSAXParser();

            }
            catch (ParserConfigurationException e) {
                logger.error(e);
                throw new RepositoryException(e.getMessage(), e);
            }
            catch (SAXException e) {
                logger.error(e);
                throw new RepositoryException(e.getMessage(), e);
            }

            try {
                parser.parse(inputSetDefinitions, sdh);
                getWithInputSetDefinitions.releaseConnection();
                inputSetDefinitions.close();
                int offset = 0;
                while (sdh.getRecordNumber() == 100) {
                    sdh.resetRecordsNumber();
                    offset = offset + 100;
                    getWithInputSetDefinitions =
                        EscidocConnector.requestRetrieveSetDefinitions(String
                            .valueOf(offset));
                    inputSetDefinitions =
                        getWithInputSetDefinitions.getResponseBodyAsStream();
                    parser.parse(inputSetDefinitions, sdh);
                    getWithInputSetDefinitions.releaseConnection();
                    inputSetDefinitions.close();

                }
            }
            catch (SAXException e) {
                logger.error(e);
                throw new RepositoryException(e.getMessage(), e);
            }
            catch (IOException e) {
                logger.error(e);
                throw new RepositoryException(e.getMessage(), e);
            }

            HashMap<String, EscidocSetInfo> escidocSetDefinitions =
                sdh.getData();

            this.searchHitLists = new HashMap<String, Vector<String>>();
            Set<String> keys = escidocSetDefinitions.keySet();
            Iterator<String> it = keys.iterator();
            while (it.hasNext()) {
                String setSpec = it.next();
                EscidocSetInfo escidocSet = escidocSetDefinitions.get(setSpec);
                SetInfo set = escidocSet;
                this.setDefinitions.put(setSpec, set);
                String setQuery = escidocSet.getSetQuery();
                Vector<String> resourceIds =
                    retrieveIdsForSetQuery(setSpec, setQuery);
                this.searchHitLists.put(setSpec, resourceIds);
            }
        }
        else {
            return this.setDefinitions;
        }

        return this.setDefinitions;

    }

    /**
     * Converts an instance of java.util.Date into an ISO 8601 String
     * representation. Uses the date format yyyy-MM-ddTHH:mm:ss.SSSZ or
     * yyyy-MM-ddTHH:mm:ssZ, depending on whether millisecond precision is
     * desired.
     * 
     * @param date
     *            Instance of java.util.Date.
     * @param millis
     *            Whether or not the return value should include milliseconds.
     * @return ISO 8601 String representation of the Date argument or null if
     *         the Date argument is null.
     */
    public static String convertDateToString(Date date) {
        if (date == null) {
            return null;
        }
        else {
            DateFormat df;

            df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

            df.setTimeZone(TimeZone.getTimeZone("UTC"));

            if (date.before(ONE_CE)) {
                StringBuilder sb = new StringBuilder(df.format(date));
                sb.insert(0, "-");
                return sb.toString();
            }
            else {
                return df.format(date);
            }
        }
    }

}
