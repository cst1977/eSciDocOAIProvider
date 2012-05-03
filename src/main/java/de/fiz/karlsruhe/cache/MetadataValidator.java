package de.fiz.karlsruhe.cache;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import proai.MetadataFormat;
import proai.driver.RemoteIterator;
import proai.error.RepositoryException;

public class MetadataValidator {
    private static final Logger logger =
        Logger.getLogger(MetadataValidator.class.getName());

    private Map<String, MetadataFormat> m_metadataFormats;

    private Map<String, Schema> schemaCache = null;

    private Map<String, Throwable> failedConnectCache = null;

    private Map<String, Throwable> failedFileNotFoundCache = null;

    private Map<String, Throwable> failedParseSchemaCache = null;

    private Map<String, Throwable> wrongTargetNamespaceCache = null;

    private Map<String, Throwable> malformedSchemaUrl = null;

    private SAXParserFactory saxParserFactory;

    public MetadataValidator() {
        saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setValidating(false);
        saxParserFactory.setNamespaceAware(true);

    }
    
    public void init(RemoteIterator<? extends MetadataFormat> riter) {
        m_metadataFormats = new HashMap<String, MetadataFormat> (); 
    
        schemaCache = new HashMap<String, Schema>();
        failedConnectCache = new HashMap<String, Throwable>();
        failedParseSchemaCache = new HashMap<String, Throwable>();
        failedFileNotFoundCache = new HashMap<String, Throwable>();
        wrongTargetNamespaceCache = new HashMap<String, Throwable>();
        malformedSchemaUrl = new HashMap<String, Throwable>();
   
            while (riter.hasNext()) {
              MetadataFormat format = riter.next();
              String mdPrefix = format.getPrefix();
              m_metadataFormats.put(mdPrefix, format);
              putSchemaInMap(mdPrefix);
            }           
    }

    /**
     * On the beginning of every update cycle tries to instantiate schemas which
     * are in the failedConnectCache and puts them in a schema map or in an
     * appropriated Exception map.
     */
    public void updateStart() {
        HashMap <String, Throwable> copy = new HashMap <String, Throwable>();
        copy.putAll(failedConnectCache);
        failedConnectCache = new HashMap<String, Throwable> ();
        Iterator<String> iterator = copy.keySet().iterator();
        while (iterator.hasNext()) {
            String mdPrefix = iterator.next();
            putSchemaInMap(mdPrefix);   
        }
        
    }

    /**
     * Put the schema instance for the provided md prefix in the schema map, if
     * the schema can be instantiated. Otherwise put an Exception message in the
     * appropriate Exception map.
     * 
     * @param mdPrefix
     *            The mdPrefix fir the schema.
     */
    private void putSchemaInMap(final String mdPrefix) {

        MetadataFormat format = m_metadataFormats.get(mdPrefix);
        String schemaLocation = format.getSchemaLocation();
        URL schemaUrl = null;
        try {
            schemaUrl = new URL(schemaLocation);
        }
        catch (MalformedURLException e1) {
            malformedSchemaUrl.put(mdPrefix, e1);
        }
        if (schemaUrl != null) {
            InputStream schemaStream1 =  null;
            InputStream schemaStream2 = null;
            try {
                URLConnection conn1 = schemaUrl.openConnection();
                URLConnection conn2 = schemaUrl.openConnection();
                schemaStream1 = conn1.getInputStream();
                schemaStream2 = conn2.getInputStream();
            }
            catch (IOException e1) {
                if (e1 instanceof FileNotFoundException) {
                    failedFileNotFoundCache.put(mdPrefix, e1);
                }
                else {
                    failedConnectCache.put(mdPrefix, e1);
                }
            }
            if (schemaStream1 != null && schemaStream2 != null) {
                String formatNameSpaceUri = format.getNamespaceURI();
                String targetNameSpace = null;
                URL formatNameSpaceUriURL = null;
                if (formatNameSpaceUri != null) {
                    try {
                        formatNameSpaceUriURL = new URL(formatNameSpaceUri);
                    }
                    catch (MalformedURLException e1) {

                    }
                }
                if (formatNameSpaceUriURL != null) {
                    try {
                        targetNameSpace = getTargetNameSpace(schemaStream2);
                    }
                    catch (RepositoryException e) {
                        failedParseSchemaCache.put(mdPrefix, e);
                    }
                    if ((targetNameSpace == null)
                        || (!targetNameSpace.equals(formatNameSpaceUri))) {
                        String message =
                            "name space uri '" + formatNameSpaceUri
                                + "' of the format with prefix " + mdPrefix
                                + " differs from target name space "
                                + targetNameSpace + "of a schema located at "
                                + format.getSchemaLocation();
                        wrongTargetNamespaceCache.put(mdPrefix,
                            new RepositoryException(message));

                    }
                }
                try {
                    Schema schema = getSchema(schemaStream1);
                    schemaCache.put(mdPrefix, schema);
                }
                catch (SAXException e) {
                    failedParseSchemaCache.put(mdPrefix, e);

                }
                finally {
                    if (schemaStream1 != null)
                        try {
                            schemaStream1.close();
                        }
                        catch (IOException e) {
                        }
                    if (schemaStream2 != null)
                        try {
                            schemaStream2.close();
                        }
                        catch (IOException e) {
                        }
                }
            }
        }
    }

    /**
     * Gets the <code>Schema</code> object for the provided
     * <code>InputStream</code>.
     * 
     * @param schemaStream
     *            The Stream containing the schema.
     * @return Returns the <code>Schema</code> object.
     * @throws Exception
     *             If anything fails.
     */
    private static Schema getSchema(final InputStream schemaStream)
        throws SAXException {
        SchemaFactory sf =
            SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema theSchema =
            sf.newSchema(new SAXSource(new InputSource(schemaStream)));
        return theSchema;
    }

    /**
     * Returns a target name space of the provided schema.
     * 
     * @param schema
     * @return
     */
    private String getTargetNameSpace(InputStream schema) {
        SAXParser parser = null;
        SchemaHandler sh = new SchemaHandler();
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
            parser.parse(schema, sh);
            String targetNamespace = sh.getTargetNamespace();
            return targetNamespace;

        }
        catch (SAXException e) {
            logger.error(e);
            throw new RepositoryException(
                "Exception while reading schema target namespace", e);
        }
        catch (IOException e) {
            logger.error(e);
            throw new RepositoryException(
                "Exception while reading schema target namespace", e);
        }

    }
/**
 * Validates the provided xml against a schema for a format of the provided
 * prefix.
 * @param mdPrefix
 * @param xml
 * @return ValidationInfo containing result of the validation
 */
    public ValidationInfo validate(String mdPrefix, String xml) {
        ValidationInfo validationInfo = new ValidationInfo();
        StringReader toValidate = new StringReader(xml);
        long retrievalDelay = 0;
        Schema schema = schemaCache.get(mdPrefix);
        if (schema == null) {
            if (malformedSchemaUrl.containsKey(mdPrefix)) {
                validationInfo.setFailReason(malformedSchemaUrl.get(mdPrefix));
                validationInfo.setResult(ValidationResult.wrongSchemaLocation);
            }
            else if (wrongTargetNamespaceCache.containsKey(mdPrefix)) {
                validationInfo.setFailReason(wrongTargetNamespaceCache
                    .get(mdPrefix));
                validationInfo.setResult(ValidationResult.wrongSchemaLocation);
            }
            else if (failedConnectCache.containsKey(mdPrefix)) {
                validationInfo.setFailReason(failedConnectCache.get(mdPrefix));
                validationInfo.setResult(ValidationResult.connectionFailure);
            }
            else if (failedFileNotFoundCache.containsKey(mdPrefix)) {
                validationInfo.setFailReason(failedFileNotFoundCache
                    .get(mdPrefix));
                validationInfo.setResult(ValidationResult.wrongSchemaLocation);
            }
            else if (failedParseSchemaCache.containsKey(mdPrefix)) {
                validationInfo.setFailReason(failedParseSchemaCache
                    .get(mdPrefix));
                validationInfo.setResult(ValidationResult.wrongSchemaLocation);
            }
            return validationInfo;
        }
        else {

            try {
                Validator validator = schema.newValidator();
                long startValidationTime = System.currentTimeMillis();
                validator.validate(new SAXSource(new InputSource(toValidate)));
                long endValidationTime = System.currentTimeMillis();
                retrievalDelay = endValidationTime - startValidationTime;
                validationInfo.setResult(ValidationResult.valid);
                validationInfo.setValidationDelay(retrievalDelay);
            }
            catch (SAXException e) {
                validationInfo.setFailReason(e);
                validationInfo.setResult(ValidationResult.invalid);
            }
            catch (IOException e) {
                validationInfo.setFailReason(e);
                validationInfo.setResult(ValidationResult.invalid);
            }
            return validationInfo;
        }

    }

}
