package de.fiz.karlsruhe.escidoc.services.oaiprovider;

import java.util.HashMap;

import org.apache.commons.httpclient.methods.GetMethod;

import proai.error.RepositoryException;

/**
 * An utility class for requests to Escidoc and Escidoc search service.
 * 
 * @author ROF
 * 
 */
public class EscidocConnector {
    private static ConnectionUtility utility = null;

    private static String escidocUrl = null;

    private static String searchUrl = null;

    public static void init(final String baseUrl, final String searchBaseUrl){
        utility = new ConnectionUtility();
        escidocUrl = baseUrl;
        searchUrl = searchBaseUrl;
    }

    /**
     * 
     * @param spoQuery
     * @return
     * @throws TripleStoreSystemException
     * 
     * TODO move to TriplestoreUtility implementation
     */
    public static GetMethod requestSearchQueryReleased(
        final String mdRecordName, final String mdRecordUri,
        final String timeFrom, final String timeUntil, final String startRecord)
        throws RepositoryException {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("maximumRecords", "100");
        params.put("startRecord", startRecord);
        String query =
            "escidoc.md-record-identifier=\"" + mdRecordName + "@" + mdRecordUri + "\""
                + " and escidoc.latest-release.date<=\""
                + timeUntil
                + "\" and escidoc.latest-release.date>\""
                + timeFrom
                + "\" and escidoc.public-status=released ";
        params.put("query", query);
        return requestSearchQuery(params);
    }
    /**
     * 
     * @param timeFrom
     * @param timeUntil
     * @param startRecord
     * @return
     * @throws RepositoryException
     */
    public static GetMethod requestSearchQueryDcReleased(
        final String timeFrom, final String timeUntil, final String startRecord)
        throws RepositoryException {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("maximumRecords", "100");
        params.put("startRecord", startRecord);
        String query =
                  "escidoc.latest-release.date<=\""
                + timeUntil
                + "\" and escidoc.latest-release.date>\""
                + timeFrom
                + "\" and escidoc.public-status=released ";
        params.put("query", query);
        return requestSearchQuery(params);
    }

    /**
     * 
     * @param spoQuery
     * @return
     * @throws TripleStoreSystemException
     * 
     * TODO move to TriplestoreUtility implementation
     */
    public static GetMethod requestSearchQueryWithdrawn(
        final String mdRecordName, final String mdRecordUri,
        final String timeFrom, final String timeUntil, final String startRecord)
        throws RepositoryException {
        HashMap<String, String> params = new HashMap<String, String>();
        
        params.put("maximumRecords", "100");
        params.put("startRecord", startRecord);
        String query =
            "escidoc.md-record-identifier=\"" + mdRecordName + "@" + mdRecordUri + "\""
                + " and escidoc.last-modification-date<=\""
                + timeUntil
                + "\" and escidoc.last-modification-date>\""
                + timeFrom
                + "\" and escidoc.public-status=withdrawn ";
        params.put("query", query);
        return requestSearchQuery(params);
    }
    /**
     * 
     * @param timeFrom
     * @param timeUntil
     * @param startRecord
     * @return
     * @throws RepositoryException
     */
    public static GetMethod requestSearchQueryDcWithdrawn(
       final String timeFrom, final String timeUntil, final String startRecord)
        throws RepositoryException {
        HashMap<String, String> params = new HashMap<String, String>();
        
        params.put("maximumRecords", "100");
        params.put("startRecord", startRecord);
        String query = "escidoc.last-modification-date<=\""
                + timeUntil
                + "\" and escidoc.last-modification-date>\""
                + timeFrom
                + "\" and escidoc.public-status=withdrawn ";
        params.put("query", query);
        return requestSearchQuery(params);
    }

    /**
     * 
     * @param spoQuery
     * @return
     * @throws TripleStoreSystemException
     * 
     * TODO move to TriplestoreUtility implementation
     */
    public static GetMethod requestSearchLmdQuery()
        throws RepositoryException {
        HashMap<String, String> params = new HashMap<String, String>();
        String query = "escidoc.objecttype=item or escidoc.objecttype=container";
        params.put("query", query);
        params.put("maximumRecords", "1");
        params.put("sortKeys","escidoc.last-modification-date,,0");
        return requestSearchQuery(params);
    }
    
    /**
     * Returns a GET Method with the http response containing the result of the
     * search request with the provided search query.
     * 
     * @param searchFilterQuery
     * @param startRecord
     * @return GET Method
     * @throws RepositoryException
     */
    public static GetMethod requestSearchFilterQuery(
        final String searchFilterQuery, final String startRecord)
        throws RepositoryException {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("query", searchFilterQuery);
        params.put("maximumRecords", "100");
        params.put("startRecord", startRecord);
        return requestSearchQuery(params);
    }

    /**
     * Returns a GET Method with the http response containing the result of the
     * invocation of the escidoc method getRepositoryInfo().
     * 
     * @return GET Method
     * @throws RepositoryException
     */
    public static GetMethod requestIdentify()
        throws RepositoryException {
        String url = null;
        HashMap<String, String> params = new HashMap<String, String>();
        if (escidocUrl.endsWith("/")) {
            url = escidocUrl + "adm/admin/get-repository-info";
        }
        else {
            url = escidocUrl + "/adm/admin/get-repository-info";
        }
        return utility.get(url, params);
    }
    
    /**
     * Returns a GET Method with the http response containing the result
     * of the search request with the provided query and query parameters.
     * @param params
     * @return GET Method
     * @throws RepositoryException
     */
    public static GetMethod requestSearchQuery(final HashMap <String, String> params)
        throws RepositoryException {
        String url = null;
        if (searchUrl.endsWith("/")) {
            url = searchUrl + "escidocoaipmh_all";
        }
        else {
            url = searchUrl + "/escidocoaipmh_all";
        }

        return utility.get(url, params);

    }

    /**
     * Returns a GET Method with the http response containing the result of the
     * invocation of the escidoc method retrieveOrganizationalUnits().
     * 
     * @param offset
     * @return GET Method
     * @throws RepositoryException
     */
    public static GetMethod requestRetrieveOrganizationalUnits(final String offset)
    throws RepositoryException {
        StringBuffer parameters = new StringBuffer("maximumRecords=20");
        if (offset != null && !offset.equals("") && !offset.equals("0")) {
        	parameters.append("&startRecord=").append(offset);
        }
        String url = null;
        if (escidocUrl.endsWith("/")) {
            url = escidocUrl + "oum/organizational-units?" + parameters.toString();
        }
        else {
            url = escidocUrl + "/oum/organizational-units?" + parameters.toString();
        }

        return utility.get(url);

    }
    /**
     * Returns a GET Method with the http response containing the result of the
     * invocation of the escidoc method retrieveSetDefinitions().
     * 
     * @param offset
     * @return GET Method
     * @throws RepositoryException
     */
    public static GetMethod requestRetrieveSetDefinitions(final String offset)
    throws RepositoryException {
        StringBuffer parameters = new StringBuffer("maximumRecords=100");
        if (offset != null && !offset.equals("") && !offset.equals("0")) {
        	parameters.append("&startRecord=").append(offset);
        }
        String url = null;
        if (escidocUrl.endsWith("/")) {
            url = escidocUrl + "oai/set-definitions?" + parameters.toString();
        }
        else {
            url = escidocUrl + "/oai/set-definitions?" + parameters.toString();
        }

        return utility.get(url);

    }

    /**
     * Returns a GET Method with the http response containing the result of the
     * invocation of the escidoc method retrieveContexts().
     * 
     * @param offset
     * @return GET Method
     * @throws RepositoryException
     */
    public static GetMethod requestRetrieveContexts(final String offset)
    throws RepositoryException {
        StringBuffer parameters = new StringBuffer("maximumRecords=20");
        if (offset != null && !offset.equals("") && !offset.equals("0")) {
        	parameters.append("&startRecord=").append(offset);
        }
        String url = null;
        if (escidocUrl.endsWith("/")) {
            url = escidocUrl + "ir/contexts?" + parameters.toString();
        }
        else {
            url = escidocUrl + "/ir/contexts?" + parameters.toString();
        }

        return utility.get(url);

    }
    
    /**
     * Returns a GET Method with the http response containing the content of a
     * md-record with the provided name of an escidoc resource with the provided
     * id and the provided resource type.
     * 
     * @param resourceId
     *            id of the escidoc resource
     * @param resourceType
     *            type of the escidoc resource
     * @param mdRecordName 
     *            provided md-record name
     * @return Get Method
     * @throws RepositoryException
     */
    public static GetMethod requestRetrieveMdRecord(final String resourceId, 
        final String resourceType, final String mdRecordName)
    throws RepositoryException {
        String url = null;
        if (escidocUrl.endsWith("/")) {
            url = escidocUrl + "ir/" + resourceType +  "/" + resourceId + "/md-records/md-record/" + mdRecordName + "/content";
        }
        else {
            url = escidocUrl + "/ir/" + resourceType +  "/" + resourceId + "/md-records/md-record/" + mdRecordName + "/content";
        }
        return utility.get(url);
    }
    
    /**
     * Returns a GET Method with the http response containing the content of a
     * resource with the provided relative URL of an escidoc resource with the provided
     * id and the provided resource type.
     * 
     * @param resourceId
     *            id of the escidoc resource
     * @param resourceType
     *            type of the escidoc resource
     * @param mdRecordName 
     *            provided md-record name
     * @return Get Method
     * @throws RepositoryException
     */
    public static GetMethod requestRetrieveResource(final String resourceId, 
        final String resourceType, final String suffix)
    throws RepositoryException {
        String resource = suffix.substring("resources".length());
        String url = null;
        if (escidocUrl.endsWith("/")) {
            url = escidocUrl + "ir/" + resourceType +  "/" + resourceId + "/resources/" + resource;
        }
        else {
            url = escidocUrl + "/ir/" + resourceType +  "/" + resourceId + "/resources/" + resource;
        }
        return utility.get(url);
    }
    
    /**
     * Returns a GET Method with the http response containing the content of a
     * DC data stream of an escidoc resource with the provided id and the provided
     * resource type.
     * 
     * @param resourceId
     *            id of the escidoc resource
     * @param resourceType
     *            type of the escidoc resource
     * @return Get Method
     * @throws RepositoryException
     */
    public static GetMethod requestRetrieveDc(final String resourceId, 
        final String resourceType) throws RepositoryException {
        String url = null;
        if (escidocUrl.endsWith("/")) {
            url = escidocUrl + "ir/" + resourceType +  "/" + resourceId + "/resources/dc/content";
        }
        else {
            url = escidocUrl + "/ir/" + resourceType +  "/" + resourceId + "/resources/dc/content";
        }
        return utility.get(url);
    }
    
}
