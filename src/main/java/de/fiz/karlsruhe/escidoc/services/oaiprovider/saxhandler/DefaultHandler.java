package de.fiz.karlsruhe.escidoc.services.oaiprovider.saxhandler;

/**
 * DefaultHandler that does not resolve dtds.
 * 
 * @author MIH
 * 
 */
public class DefaultHandler extends org.xml.sax.helpers.DefaultHandler {

	@Override
	public org.xml.sax.InputSource resolveEntity(String publicId,
			String systemId) throws org.xml.sax.SAXException,
			java.io.IOException {
		return new org.xml.sax.InputSource(new java.io.StringReader(""));
	}
}
