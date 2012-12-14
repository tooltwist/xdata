package com.dinaa.data;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.utils.PrefixResolver;
import org.apache.xml.utils.PrefixResolverDefault;
import org.apache.xpath.XPathContext;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import tooltwist.repository.ToolTwist;
import tooltwist.repository.RepositoryException;

import com.dinaa.fastXml.FastXml;
import com.dinaa.fastXml.FastXmlException;

/**
 * General purpose XML data manipulation class. This class provides a convenient way to create and access XML data in either text or DOM (Document Object Model) form. Conversions between these forms are performed as required.
 * 
 * @author: Philip Callender
 */
public class XDataHiddenStuff implements Serializable {
	private static final long serialVersionUID = -5950816450225886923L;

	protected static final int MODE_FAST_XML = 2;
	protected static final int MODE_DOM = 3;

	// Three data storage formats...
	private String stringXml = "<empty/>";
	private boolean stringXmlIsValid = true;

	private FastXml fastXml = null;
	private boolean usingFastXml = false;

	private Document document = null;

	// XPath stuff
	private PrefixResolver prefixResolver = null;
	private XPathContext rootXpathContext = null;
	private int rootContextNode = -1;

	// private XMLParserLiaison xpathSupport = null;
	// private XPathProcessor xpathParser = null;
	/**
	 * Constructor for empty XML data. This creates an XML document equivalent to <I>&lt;empty/&gt;</I>.
	 * 
	 * @throws RepositoryException
	 */
	protected XDataHiddenStuff(boolean useFastXML) {
		super();
		usingFastXml = useFastXML && ToolTwist.useFastXml();
	}

	protected boolean usingFastXml() {
		return usingFastXml;
	}

	protected void useDOM() {
		usingFastXml = false;
	}

	protected final FastXml getFastXml() throws FastXmlException {
		if (!usingFastXml)
			return null;

		if (fastXml == null)
			fastXml = new FastXml(stringXml);
		return fastXml;
	}

	protected String debugTypeStr() {
		String s = "";
		String sep = "";
		s += usingFastXml ? "fastXML(" : "DOM(";
		if (stringXmlIsValid) {
			s += "S";
			sep = ", ";
		}
		if (fastXml != null) {
			s += sep + "F";
			sep = ", ";
		}
		if (document != null) {
			s += sep + "D";
			sep = ", ";
		}
		s += ")";
		return s;
	}

	protected void checkParsed() throws XDataException {
		if (usingFastXml)
			getFastXml();
		else
			getDocument();
	}

	/**
	 * Get a DOM document from the XData. This document can then be manipulated directly using the DOM API. Note that if the document is changed in this way, method <code>notifyDocumentChanged</code> must be called to tell the XData object that the XML String may need to be recreated from it's internal DOM document.
	 * 
	 * @return org.w3c.dom.Document
	 * @see #notifyDocumentChanged
	 */
	protected final Document getDocument() throws XDataException {
		// If we ae in fastXML mode, we need to drop back to DOM mode now
		if (usingFastXml) {
			// We need to get the string so we can create a new DOM document
			if (!stringXmlIsValid) {
				stringXml = fastXml.getXml();
				stringXmlIsValid = true;
			}
		} else {
			if (document != null)
				return document;
			if (!stringXmlIsValid) {
				stringXml = XData.domToXml(document);
				stringXmlIsValid = true;
			}
		}

		/*
		 * old version parsing try { // Parse the XML DOMParser myParser = XData.getDOMParser(); InputSource is = new InputSource(new java.io.StringReader(xml)); myParser.parse(is); document = myParser.getDocument(); } catch (Exception e) { throw new XDataException("Error parsing XML: " + e.getMessage()); }
		 */
		try {
			// Step 1: create a DocumentBuilderFactory and configure it
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

			// Set namespaceAware to true to get a DOM level 2 tree with nodes
			// containing namespace information. This is necessary because the
			// default value from JAXP 1.0 was defined to be false.
			// dbf.setNamespaceAware(true);

			// Set the validation mode to either: no validation, DTD
			// validation, or XSD validation
			/*
			 * dbf.setValidating(dtdValidate || xsdValidate); if (xsdValidate) { try { dbf.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA); } catch (IllegalArgumentException x) { // This can happen if the parser does not support JAXP 1.2 System.err.println( "Error: JAXP DocumentBuilderFactory attribute not recognized: " + JAXP_SCHEMA_LANGUAGE);
			 * System.err.println("Check to see if the parser conforms to JAXP 1.2 spec"); System.exit(1); } }
			 */

			// Set various configuration options
			// dbf.setIgnoringComments(true);
			// dbf.setIgnoringElementContentWhitespace(true);
			// dbf.setCoalescing(putCDATAIntoText);
			// The opposite of creating entity ref nodes is expanding them inline
			// dbf.setExpandEntityReferences(!createEntityRefs);

			// Step 2: create a DocumentBuilder that satisfies the constraints
			// dpecified by the DocumentBuilderFactory
			DocumentBuilder db = dbf.newDocumentBuilder();

			// Set an error handler before parsing
			// OutputStreamWriter errorWriter = new OutputStreamWriter(System.err, outputEncoding);
			OutputStreamWriter errorWriter = new OutputStreamWriter(System.err);
			db.setErrorHandler(new org.apache.xml.utils.DefaultErrorHandler(new PrintWriter(errorWriter, true)));

			// Step 3: parse the XML
			InputSource is = new InputSource(new StringReader(stringXml));
			document = db.parse(is);

			// Remove the xpath processing structures, so they'll be recreated for the new document
			prefixResolver = null;
			rootContextNode = -1;
		} catch (IOException e) {
			throw new XDataException("Error parsing XML in XData: " + e);
		} catch (ParserConfigurationException e) {
			throw new XDataException("Error parsing XML in XData: " + e);
		} catch (SAXException e) {
			throw new XDataException("Error parsing XML in XData: " + e);
		}
		if (document == null)
			throw new XDataException("XData constructed with invalid XML");

		usingFastXml = false;
		return document;
	}

	/**
	 * Return the text value in the first node that matches a specified XPATH below a specifed starting point.
	 * 
	 * @return The text contained in the first node that matches the specified XPath.
	 * @param xpath
	 *            An XPath string to be matched.
	 * @param target
	 *            The place from which to start the search.
	 * @see getXpathNodeList
	 */
	protected PrefixResolver getPrefixResolver() throws XDataException {

		//
		// prefixResolver = null;

		// Check the document is up to date (which may remove prefixResolver and parentContextNode)
		if (this.prefixResolver == null) {

			getDocument(); // check there is a current document object
			prefixResolver = new PrefixResolverDefault(document.getDocumentElement());
		}
		return prefixResolver;
	}

	/**
	 * Return the text value in the first node that matches a specified XPATH below a specifed starting point.
	 * 
	 * @return The text contained in the first node that matches the specified XPath.
	 * @param xpath
	 *            An XPath string to be matched.
	 * @param target
	 *            The place from which to start the search.
	 * @see getXpathNodeList
	 */
	protected int getRootContextNode() throws XDataException {

		if (rootContextNode >= 0)
			return rootContextNode;

		// Check we have a DOM document, and an XPathContext for the document
		getDocument();
		getRootXPathContext();
		rootContextNode = rootXpathContext.getDTMHandleFromNode(document.getDocumentElement());
		return rootContextNode;
	}

	/**
	 * Return the text value in the first node that matches a specified XPATH below a specifed starting point.
	 * 
	 * @return The text contained in the first node that matches the specified XPath.
	 * @param xpath
	 *            An XPath string to be matched.
	 * @param target
	 *            The place from which to start the search.
	 * @see getXpathNodeList
	 */
	protected XPathContext getRootXPathContext() {

		if (rootXpathContext != null)
			return rootXpathContext;

		// Check we have a DOM document, and an XPathContext for the document
		rootXpathContext = new XPathContext();
		return rootXpathContext;
	}

	/**
	 * Get the contents of the XData as a String.
	 * 
	 * @return A String containing XML data.
	 * @see getDocument
	 */
	protected String getXml() {
		if (stringXmlIsValid)
			return stringXml;

		// Convert the document to XML
		if (usingFastXml) {
			stringXml = fastXml.getXml();
			stringXmlIsValid = true;
		} else {
			stringXml = XData.domToXml(document);
			stringXmlIsValid = true;
		}
		return stringXml;
	}

	/**
	 * Notify this class that it's internal DOM document has been changed by external code. This method should be called if the document returned by <code>getDocument</code> is modified in any way. This method lets the object know that it's internal caching needs to be flushed. Any pre-stored value based on the contents of the DOM document will need to be checked.
	 * 
	 * @see #getDocument
	 */
	protected void notifyDocumentChanged() {
		// The text version is no longer valid - it must be recreated from the DOM document 'document'
		stringXml = null;
		stringXmlIsValid = false;

		// The XPath stuff is no longer valid
		prefixResolver = null;
		rootXpathContext = null;
		rootContextNode = -1;
	}

	/**
	 * Notify this class that it's internal text version of the XML has been changed by external code. This method lets the object know that it's internal caching needs to be flushed. Any pre-stored value based on the contents of the DOM document will need to be checked.
	 * 
	 * @see #getDocument
	 */
	private void notifyXmlChanged() {
		// The text version is no longer valid - it must be recreated from the DOM document 'document'
		fastXml = null;
		document = null;

		// The XPath stuff is no longer valid
		prefixResolver = null;
		rootXpathContext = null;
		rootContextNode = -1;
	}

	/**
	 * Get the contents of the XData as a String.
	 * 
	 * @return A String containing XML data.
	 * @see getDocument
	 */
	protected void setDocument(Document doc) {
		this.document = doc;
		usingFastXml = false;
		notifyDocumentChanged();
	}

	/**
	 * Get the contents of the XData as a String.
	 * 
	 * @return A String containing XML data.
	 * @see getDocument
	 */
	protected void setXml(String xml) {
		this.stringXml = xml;
		this.stringXmlIsValid = true;
		notifyXmlChanged();
	}

}
