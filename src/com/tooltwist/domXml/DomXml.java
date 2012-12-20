package com.tooltwist.domXml;

import java.io.CharArrayReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.xml.utils.PrefixResolver;
import org.apache.xml.utils.PrefixResolverDefault;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.tooltwist.xdata.X2DataException;
import com.tooltwist.xdata.X2DataNotFoundException;
import com.tooltwist.xdata.XIteratorCallback;
import com.tooltwist.xdata.XSelectable;

public class DomXml implements XSelectable, Iterable<XSelectable> {

	private Document document;
	private PrefixResolverDefault prefixResolver;
	private XPathContext rootXpathContext;
	private int rootContextNode;

	public DomXml(String xml) throws DomXmlException {
		init(xml.toCharArray());
	}

	public DomXml(char xml[]) throws DomXmlException {
		init(xml);
	}

	public DomXml(File file, boolean useUnicode) throws DomXmlException {
		String path = file.getAbsolutePath();
		long fileSize = file.length();
		char arr[] = new char[(int) fileSize];
		FileInputStream is = null;
		InputStreamReader in = null;
		try {
			is = new FileInputStream(path);
			if (useUnicode) {
				in = new InputStreamReader(is, "UTF-16");
				// in = new InputStreamReader(is, "UTF-8");
			} else
				in = new InputStreamReader(is);
			int num = in.read(arr);
			if (num != fileSize)
				throw new DomXmlException("Did not read entire file");
		} catch (IOException e) {
			System.err.println("Cannot load XML from file '" + path + "': " + e);
		} finally {
			try {
				if (in != null)
					in.close();
				if (is != null)
					is.close();
			} catch (IOException e) {
			}
		}

		init(arr);
	}

	private void init(char[] charArray) throws DomXmlException {

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
			CharArrayReader reader = new CharArrayReader(charArray);
			InputSource is = new InputSource(reader);
			document = db.parse(is);

			// Remove the xpath processing structures, so they'll be recreated for the new document
			prefixResolver = null;
			rootContextNode = -1;
		} catch (IOException e) {
			throw new DomXmlException("Error parsing XML in XData: " + e);
		} catch (ParserConfigurationException e) {
			throw new DomXmlException("Error parsing XML in XData: " + e);
		} catch (SAXException e) {
			throw new DomXmlException("Error parsing XML in XData: " + e);
		}
		if (document == null)
			throw new DomXmlException("XData constructed with invalid XML");

	}

	/**
	 * Return the text value in the first node that matches a specified XPATH below a specified starting point.
	 * 
	 * @return The text contained in the first node that matches the specified XPath.
	 * @param xpath
	 *            An XPath string to be matched.
	 * @param target
	 *            The place from which to start the search.
	 * @see getXpathNodeList
	 */
	protected PrefixResolver getPrefixResolver() {

		//
		// prefixResolver = null;

		// Check the document is up to date (which may remove prefixResolver and parentContextNode)
		if (this.prefixResolver == null) {

//XXXX			getDocument(); // check there is a current document object
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
	protected int getRootContextNode() {

		if (rootContextNode >= 0)
			return rootContextNode;

		// Check we have a DOM document, and an XPathContext for the document
//		getDocument();
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
	private XPathContext getRootXPathContext() {

		if (rootXpathContext != null)
			return rootXpathContext;

		// Check we have a DOM document, and an XPathContext for the document
		rootXpathContext = new XPathContext();
		return rootXpathContext;
	}

	/**
	 * Get a list of nodes that match the specified XPATH.
	 * 
	 * @return A list of nodes that match the XPath.
	 * @param xpath
	 *            An XPath string to be matched.
	 * @see getXpathNode
	 */
	public DomXmlList getNodes(String xpath) throws X2DataException {
		NodeList nl = getNodeList(xpath);
		return new DomXmlList(nl, this);
	}

	/**
	 * Get a list of nodes that match the specified XPATH, starting the search from a specific DOM node.
	 * 
	 * @return A list of nodes that match the XPath.
	 * @param xpath
	 *            An XPath string to be matched.
	 * @param target
	 *            A node from which to start matching the XPath.
	 * @throws X2DataException 
	 * @see getXpathNode
	 */
	public DomXmlList getNodes(String xpath, Node target) throws X2DataException {
		NodeList nl = getNodeList(xpath, target);
		return new DomXmlList(nl, this);
	}

	/**
	 * Get a node that matches a specified XPATH. This method returns the index'th node that matches the provided XPath. If no suitable node can be found, return null.
	 * 
	 * @return The index'th nodes that matches the specified XPath.
	 * @param xpath
	 *            An XPath string to be matched.
	 * @param index
	 *            Which node to return.
	 * @see getXpathNode
	 */
	private NodeList getNodeList(String xpath) throws X2DataException {
//		if (traceName != null)
//			logger.debug("XData[" + traceName + "].getNode(String xpath)");
		try {
			PrefixResolver prefixResolver = getPrefixResolver();
			XPathContext xpathSupport = getRootXPathContext();
			int contextNode = getRootContextNode();

			// Select the list of nodes
			XPath _xpath = new XPath(xpath, null, prefixResolver, XPath.SELECT, null);
			NodeList nl = _xpath.execute(xpathSupport, contextNode, prefixResolver).nodelist();
			return nl;
		} catch (TransformerException e) {
			throw new X2DataNotFoundException("Error selecting xpath (" + xpath + ") from root: " + e);
		}
	}

	/**
	 * Get a node that matches a specified XPATH below a specifed starting point. This method returns the index'th node that matches the provided XPath. If no suitable node can be found, return null.
	 * 
	 * @return The index'th nodes that matches the specified XPath.
	 * @param xpath
	 *            An XPath string to be matched.
	 * @param index
	 *            Which node to return.
	 * @param target
	 *            The place from which to start the search.
	 * @see getXpathNodeList
	 */
	private NodeList getNodeList(String xpath, Node target) throws X2DataException {
		try {
			// XPathContext xpathSupport = new XPathContext();
			XPathContext xpathSupport = getRootXPathContext(); // ZZZZZZZZZ This might be better

			Node node = (target.getNodeType() == Node.DOCUMENT_NODE) ? ((Document) target).getDocumentElement() : target;
			PrefixResolverDefault prefixResolver = new PrefixResolverDefault(node);
			int contextNode = xpathSupport.getDTMHandleFromNode(target);

			// Select the list of nodes
			XPath _xpath = new XPath(xpath, null, prefixResolver, XPath.SELECT, null);
			// int contextNode = xpathSupport.getDTMHandleFromNode(target);
			NodeList nl = _xpath.execute(xpathSupport, contextNode, prefixResolver).nodelist();
			return nl;
		} catch (TransformerException e) {
			throw new X2DataException("Error selecting xpath (" + xpath + ") below specified node: " + e);
		}
	}

	/**
	 * Return the text value in the first node that matches a specified XPATH below a specifed starting point. IMPORTANT: Assumes that initialiseXpath has already been called.
	 * 
	 * @return The text contained in the first node that matches the specified XPath.
	 * @param xpath
	 *            An XPath string to be matched.
	 * @param target
	 *            The place from which to start the search.
	 * @see getXpathNodeList
	 */
	private String getTextFromNodeList(NodeList nodelist) // throws TransformerException
	{
//		if (traceName != null)
//			logger.debug("XData[" + traceName + "].getTextFromNodeList(NodeList nodelist)");

		// return the text from the first node that contains text
		String s = "";
		for (int i = 0; i < nodelist.getLength(); i++) {
			Node node = nodelist.item(i);
			short type = node.getNodeType();
			if (type == Node.TEXT_NODE || type == Node.CDATA_SECTION_NODE) {
				s += node.getNodeValue();
				break;
			} else if (type == Node.ELEMENT_NODE) {
				// Join together the text of the children
				for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
					short childType = child.getNodeType();
					if (childType == Node.TEXT_NODE || childType == Node.CDATA_SECTION_NODE)
						s += child.getNodeValue();
				}
				break;
			}
		}
		return s;
	}

	/**
	 * Return the text value in the first node that matches a specified XPATH below a specifed starting point.
	 * 
	 * @return The text contained in the first node that matches the specified XPath.
	 * @param xpath
	 *            An XPath string to be matched.
	 * @param target
	 *            The place from which to start the search.
	 * @throws X2DataException 
	 * @see getXpathNodeList
	 */
	public String getText(String xpath, Node target) throws X2DataException {
		NodeList nl = getNodeList(xpath, target);
		return getTextFromNodeList(nl);
	}


	//--------------------------------------------------------------------------------------------------------------------
	// Methods for the XSelectable interface

	public String string(String xpath) throws X2DataException {
		NodeList nl = getNodeList(xpath);
		String text = getTextFromNodeList(nl);
		return text;
	}


	//--------------------------------------------------------------------------------------------------------------------
	// Iterating over this current object, even though it's not a list.

	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	public Iterator<XSelectable> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Only iterate one time - as if there was a list of one record.
	 */
	private boolean beenToFirst = false;

	/**
	 * This data type does not provide a list of records, so the {@link #next()} method only returns true once.<p>
	 * To iterate over elements within this object, use {@link #select(String)} or one of the {@link #foreach(String)} methods.
	 */
	public void first() {
		beenToFirst = false;
	}

	/**
	 * This data type does not provide a list of records, so this method only returns true until {@link #next()} is called.<p> 
	 * To iterate over elements within this object, use {@link #select(String)} or one of the {@link #foreach(String)} methods.
	 */
	public boolean hasNext() {
		if (beenToFirst)
			return false;
		return true;
	}

	/**
	 * This data type does not provide a list of records, so this method only returns true one time. Actually it serves
	 * no purpose other than to allow iterators to access the data that can be accessed directly. For example,
	 * <pre>
	 * FastXml data = ...;
	 * String value = data.string("./name");
	 * </pre>
	 * will return exactly the same as:
	 * <pre>
	 * FastXml data = ...;
	 * while (data.next()) {
	 *   String value = data.string("./name");
	 * }
	 * </pre>
	 * <p>
	 * To iterate over a list of elements <i>within</i> this data object, use {@link #select(String)} or one of the {@link #foreach(String)} methods.
	 */
	public boolean next() {
		if (beenToFirst)
			return false;
		beenToFirst = true;
		return true;
	}

	
	//--------------------------------------------------------------------------------------------------------------------
	// Selections relative to this object, using an xpath.
	
	public XSelectable select(String xpath) throws X2DataException {
		NodeList nl = getNodeList(xpath);
		return new DomXmlList(nl, this);
	}

	public void foreach(String xpath, Object userData, XIteratorCallback callback) throws X2DataException {
		try {
			XSelectable list = this.getNodes(xpath);
			for (int index = 0; list.next(); index++) {
				callback.next(list, index, userData);
			}		
		} catch (Exception e) {
			X2DataException exception = new X2DataException(e.getMessage());
			exception.setStackTrace(e.getStackTrace());
			throw exception;
		}
	}

	public void foreach(String xpath, XIteratorCallback callback) throws X2DataException {
		foreach(xpath, null, callback);
	}

	public Iterable<XSelectable> foreach(String xpath) throws X2DataException {
		try {
			return getNodes(xpath);
		} catch (Exception e) {
			X2DataException exception = new X2DataException(e.getMessage());
			exception.setStackTrace(e.getStackTrace());
			throw exception;
		}
	}

}
