package de.fiz.karlsruhe.escidoc.services.oaiprovider;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.util.EncodingUtil;
import org.apache.log4j.Logger;
import proai.error.RepositoryException;


/**
 * An utility class for HTTP requests.
 * 
 * @author ROF
 */

public class ConnectionUtility  {

    private static final Logger log =
        Logger.getLogger(ConnectionUtility.class);

   
    private static final int HTTP_RESPONSE_OK = 200;
    
    private HttpClient httpClient = null;
    
    private static final int HTTP_MAX_CONNECTIONS_PER_HOST = 30;

    private static final int HTTP_MAX_TOTAL_CONNECTIONS_FACTOR = 3;

    
    private MultiThreadedHttpConnectionManager cm =
        new MultiThreadedHttpConnectionManager();
    
   
    /**
     * Get the HTTP Client (multi threaded).
     * 
     * @return HttpClient
     * @throws WebserverSystemException
     */
    public HttpClient getHttpClient() {
        if (this.httpClient == null) {
            this.cm.getParams().setMaxConnectionsPerHost(
                HostConfiguration.ANY_HOST_CONFIGURATION,
                HTTP_MAX_CONNECTIONS_PER_HOST);
            this.cm.getParams().setMaxTotalConnections(
                HTTP_MAX_CONNECTIONS_PER_HOST
                    * HTTP_MAX_TOTAL_CONNECTIONS_FACTOR);
            this.httpClient = new HttpClient(this.cm);
            httpClient.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, 
                new DefaultHttpMethodRetryHandler()); 
        }
        return this.httpClient;
    }
    /**
     * Call the GetMethod.
     * 
     * @param url
     *            The URL for the HTTP GET method.
     * @return GetMethod
     * @throws WebserverSystemException
     *             If connection failed.
     */
    public GetMethod get(final String url) throws RepositoryException {

        GetMethod get = null;
        try {
            get = new GetMethod(url);
            int responseCode = getHttpClient().executeMethod(get);
            if ((responseCode / 100) != (HTTP_RESPONSE_OK / 100)) {
                
                String message = get.getResponseBodyAsString();
                if (message == null) {
                    Header header = get.getResponseHeader("eSciDocException");
                    String value = header.getValue();
                    if (value != null) {
                        message =
                            "GET-Request with url " + url
                                + " results with Exception:" + value + " .";
                    }
                    else {
                        message =
                            "Connection to '" + url
                                + "' failed with response code " + responseCode;
                    }
                }

                get.releaseConnection();
                log.info(message);
                throw new RepositoryException(message);
            }
        }
        catch (HttpException e) {
            throw new RepositoryException(e.getMessage(), e);
        }
        catch (IOException e) {
            throw new RepositoryException(e.getMessage(), e);
        }

        return get;
    }
    
    /**
     * Call the GetMethod.
     * 
     * @param url
     *            The URL for the HTTP GET method.
     * @return GetMethod
     * @throws WebserverSystemException
     *             If connection failed.
     */
    public GetMethod get(final String url, final HashMap<String, String> params) throws RepositoryException {

        GetMethod get = null;
        try {
            get = new GetMethod(url);
            Set<String> paramKeys = params.keySet();
            NameValuePair [] paramsArray = new NameValuePair[paramKeys.size()]; 
            
            Iterator<String> iterator = paramKeys.iterator();
            int i = 0;
            while (iterator.hasNext()) {
                String key = iterator.next();
                String value = params.get(key);
                paramsArray [i] = new NameValuePair(key, value);
                i++;
                
            }
            if (params.size()>0) {
            String queryString = EncodingUtil.formUrlEncode(paramsArray, "UTF-8");
            get.setQueryString(queryString);
            }
           // get.setQueryString(paramsArray);
            int responseCode = getHttpClient().executeMethod(get);
            if ((responseCode / 100) != (HTTP_RESPONSE_OK / 100)) {
                
                String message = get.getResponseBodyAsString();
                if (message == null) {
                    Header header = get.getResponseHeader("eSciDocException");
                    String value = header.getValue();
                    if (value != null) {
                        message =
                            "GET-Request with url " + url
                                + " results with Exception:" + value + " .";
                    }
                    else {
                        message =
                            "Connection to '" + url
                                + "' failed with response code " + responseCode;
                    }
                }

                get.releaseConnection();
                log.info(message);
                throw new RepositoryException(message);
            }
        }
        catch (HttpException e) {
            throw new RepositoryException(e.getMessage(),e);
        }
        catch (IOException e) {
            throw new RepositoryException(e.getMessage(), e);
        }

        return get;
    }
   

  

    /**
     * Call the PostMethod.
     * 
     * @param url
     *            The URL for the HTTP PUT request
     * @param body
     *            The body for the PUT request.
     * @return PutMethod
     * @throws WebserverSystemException
     *             If connection failed.
     */
    public PostMethod post(final String url, final String body)
        throws RepositoryException {

        PostMethod post = null;
        RequestEntity entity;
        try {
            
            entity = new StringRequestEntity(body, "text/xml", "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new RepositoryException(e.getMessage(), e);
        }

        try {
            post = new PostMethod(url);
            post.setRequestEntity(entity);

            int responseCode = getHttpClient().executeMethod(post);
            if ((responseCode / 100) != (HTTP_RESPONSE_OK / 100)) {
                
                String message = post.getResponseBodyAsString();
                if (message == null) {
                    Header header = post.getResponseHeader("eSciDocException");
                    String value = header.getValue();
                    if (value != null) {
                        message =
                            "POST-Request with url " + url
                                + " results with Exception:" + value + " .";
                    }
                    else {
                        message =
                            "Connection to '" + url
                                + "' failed with response code " + responseCode;
                    }
                }

                post.releaseConnection();
                log.info(message);
                throw new RepositoryException(message);
                
            }
        }
        catch (HttpException e) {
            throw new RepositoryException(e.getMessage(), e);
        }
        catch (IOException e) {
            throw new RepositoryException(e.getMessage(), e);
        }

        return post;
    }

}
