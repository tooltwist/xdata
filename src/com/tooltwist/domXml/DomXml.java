package com.tooltwist.domXml;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.DocumentBuilderFactory;
//import javax.xml.parsers.ParserConfigurationException;
//import javax.xml.transform.TransformerException;

import org.apache.xml.utils.PrefixResolver;
import org.apache.xml.utils.PrefixResolverDefault;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.tooltwist.xdata.X2Data;
import com.tooltwist.xdata.X2DataException;
import com.tooltwist.xdata.X2DataNotFoundException;
import com.tooltwist.xdata.XIteratorCallback;
import com.tooltwist.xdata.XSelectable;

public class DomXml implements XSelectable, Iterable<XSelectable> {

	private Document document;
//	private PrefixResolverDefault prefixResolver;
//	private XPathContext rootXpathContext = null;
//	private int rootContextNode = -1;
	private X2Data parentXD;

	public DomXml(X2Data parent, String xml) throws DomXmlException {
		this.parentXD = parent;
		
		init(xml.toCharArray());
	}

	public DomXml(X2Data parent, char xml[]) throws DomXmlException {
		this.parentXD = parent;
		
		init(xml);
	}

	public DomXml(X2Data parent, File file, boolean useUnicode) throws DomXmlException {
		this.parentXD = parent;

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
//			prefixResolver = null;
//			rootContextNode = -1;
			
//	        DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();    
//	        DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("XML 3.0 LS 3.0");
//	        if (impl == null) {
//	            System.out.println("No DOMImplementation found !");
//	            System.exit(0);
//	        }
//
//	        System.out.printf("DOMImplementationLS: %s\n", impl.getClass().getName());
//
//	        LSParser parser = impl.createLSParser(
//	                DOMImplementationLS.MODE_SYNCHRONOUS,
//	                "http://www.w3.org/TR/REC-xml");
//	        // http://www.w3.org/2001/XMLSchema
//	        System.out.printf("LSParser: %s\n", parser.getClass().getName());
//
//	        
//	        impl.createLSInput()DomXml.;
//	        LSInput input = LSInput;
//			Document doc = parser.parse(input );

			
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
	 * Return the document object at the head of the DOM representation of the XML.
	 * 
	 * @return
	 */
	public Document getDocument() {
		return document;
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
//	public PrefixResolver getPrefixResolver() {
//
//		//zzzzz
//		 prefixResolver = null;
//
//		// Check the document is up to date (which may remove prefixResolver and parentContextNode)
//		if (this.prefixResolver == null) {
//
////XXXX			getDocument(); // check there is a current document object
//			prefixResolver = new PrefixResolverDefault(document.getDocumentElement());
//		}
//		return prefixResolver;
//return null;
//	}

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
//	public int getRootContextNode() {
////zzzzzzz
//rootContextNode = -1;
//
//		if (rootContextNode >= 0)
//			return rootContextNode;
//
//		// Check we have a DOM document, and an XPathContext for the document
////		getDocument();
//		getRootXPathContext();
//		rootContextNode = rootXpathContext.getDTMHandleFromNode(document.getDocumentElement());
//		return rootContextNode;
//	}

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
//	public XPathContext getRootXPathContext() {
//		
//		//zzzzzz
//		//rootXpathContext = null;
//
//		if (rootXpathContext != null)
//			return rootXpathContext;
//
//		// Check we have a DOM document, and an XPathContext for the document
//		rootXpathContext = new XPathContext();
//		return rootXpathContext;
//	}

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
	public NodeList getNodeList(String xpath) throws X2DataException {
//		if (traceName != null)
//			logger.debug("XData[" + traceName + "].getNode(String xpath)");
		try {
			//PrefixResolver prefixResolver = getPrefixResolver();
			PrefixResolver prefixResolver = new PrefixResolverDefault(document.getDocumentElement());
			
			//cccc
			//XPathContext xpathSupport = getRootXPathContext();
			XPathContext xpathSupport = new XPathContext();

			//cccc
			//int contextNode = getRootContextNode();
			//getRootXPathContext();
			int contextNode = xpathSupport.getDTMHandleFromNode(document.getDocumentElement());


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
	public NodeList getNodeList(String xpath, Node target) throws X2DataException {
		try {
			// XPathContext xpathSupport = new XPathContext();
			
			//ccccc
			//XPathContext xpathSupport = getRootXPathContext(); // ZZZZZZZZZ This might be better
			XPathContext xpathSupport = new XPathContext();


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

	@Override
	public String getString(String xpath) throws X2DataException {
		NodeList nl = getNodeList(xpath);
		String text = getTextFromNodeList(nl);
		return text;
	}

//	@Override
//	public String getString(String xpath, int index) throws X2DataException {
//		NodeList nl = getNodeList(xpath);
//		if (index < 0 || index >= nl.getLength())
//			throw new X2DataException("no node with the specified index: getNode(\"" + xpath + "\", " + index + ")");
//		Node node = nl.item(index);
//		String string = getString(node);
//		return string;
//	}


	//--------------------------------------------------------------------------------------------------------------------
	// Iterating over this current object, even though it's not a list.

	@Override
	public int size() {
		return 0;
	}

	@Override
	public Iterator<XSelectable> iterator() {
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
	@Override
	public void first() {
		beenToFirst = false;
	}

	/**
	 * This data type does not provide a list of records, so this method only returns true until {@link #next()} is called.<p> 
	 * To iterate over elements within this object, use {@link #select(String)} or one of the {@link #foreach(String)} methods.
	 */
	@Override
	public boolean hasNext() {
		if (beenToFirst)
			return false;
		return true;
	}

	@Override
	public int currentIndex() {
		return 0;
	}

	@Override
	public String currentName() {
		Element element = document.getDocumentElement();
		return element.getNodeName();
	}

	@Override
	public boolean setCurrentIndex(int index) throws X2DataException {
		if (index == 0)
			return true;
		return false;
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

	
	//--------------------------------------------------------------------------------------------------------------------
	// Methods specific to manipulating the DOM.
	// i.e. non-XSelectable methods

	public DomXml(X2Data parent, Document document) {
		this.parentXD = parent;

		this.document = document;
	}

	public DomXml(X2Data parent, Node node) throws DomXmlException {
		this.parentXD = parent;

		try {
			 // Create the new document
			 DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			 DocumentBuilder db = dbf.newDocumentBuilder();
			 Document newDocument = db.newDocument();
			 Node newNode = newDocument.importNode(node, true);
			 // Node newNode = node;
			 newDocument.appendChild(newNode);
			
			 // Save it
			 document = newDocument;
		 }
		 catch (ParserConfigurationException e)
		 {
			 throw new DomXmlException("Error contructing XData: " + e);
		 }
	}
		
	/**
	 * Recursively get the text within a specified DOM node. This method does not return XML - it returns all the text
	 * within the node and it's children.
	 * 
	 * @param elem
	 *            The specified element or node within a DOM document.
	 * @return The text within the node.
	 */
	public static String getString(Node elem) {
		if (elem.getNodeType() == org.w3c.dom.Node.TEXT_NODE || elem.getNodeType() == org.w3c.dom.Node.CDATA_SECTION_NODE)
			return elem.getNodeValue();
	
		String s = "";
		if (elem.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
			NodeList children = elem.getChildNodes();
			for (int k = 0; k < children.getLength(); k++) {
				Node child = children.item(k);
				s += DomXml.getString(child);
			}
		}
		return s;
	}

	public void notifyDocumentChanged() throws X2DataException {
		parentXD.invalidateAllSelectorsExcept(this);
	}

	public void insert(Node root, String xpath, X2Data xd) throws X2DataException {
		// Check that the tag exists
		Node parent = checkElementExists(root, xpath);
		if (parent == null || !(parent instanceof Element))
			throw new X2DataException("xpath does not resolve to an Element: " + xpath);

		createChild((Element) parent, xd);
		notifyDocumentChanged();
	}

	/**
	 * Find the node that matches a specified XPATH, starting at <code>root</code>.
	 * If no suitable node can be found, create a new element.
	 */
	private Node checkElementExists(Node root, String xpath) throws X2DataException {
//		if (traceName != null)
//			logger.debug("XData[" + traceName + "].checkElementExists(Node root, xpath=" + xpath + ")");

		try {
			// Select the list of nodes
			XPathContext xpathSupport = new XPathContext();
			// XPathContext xpathSupport = hidden.getRootXPathContext(); ZZZZZZZZZ This might be better

			Node node = (root.getNodeType() == Node.DOCUMENT_NODE) ? ((Document) root).getDocumentElement() : root;
			PrefixResolverDefault prefixResolver = new PrefixResolverDefault(node);

			XPath _xpath = new XPath(xpath, null, prefixResolver, XPath.SELECT, null);
			int contextNode = xpathSupport.getDTMHandleFromNode(root);
			NodeList nl = _xpath.execute(xpathSupport, contextNode, prefixResolver).nodelist();
			if (nl.getLength() > 0) {
				// The xpath exists - return the first node
				return nl.item(0);
			}

			// The node does not exist - check the parent exists, then add a new child
			if (xpath.endsWith("//"))
				throw new X2DataException("Cannot create xpath - " + xpath);
			int pos = xpath.lastIndexOf('/');
			String parentXpath = (pos >= 0) ? xpath.substring(0, pos) : null;
			String childName = (pos >= 0) ? xpath.substring(pos + 1) : xpath;
			childName = childName.trim();

			// If the child name has special xpath characters then we can't create it
			if (childName.length() == 0)
				throw new X2DataException("Cannot create xPath - " + xpath + " - no child directory specified");
			if (childName.indexOf("[") >= 0 || childName.indexOf("]") >= 0)
				throw new X2DataException("Cannot create xPath - " + xpath + " - wildcard characters in name");

			// Get the parent, then add the new child
			Node parent = (parentXpath == null) ? root : checkElementExists(root, parentXpath);
			Node child;
			try {
				child = parent.getOwnerDocument().createElement(childName);
			} catch (org.w3c.dom.DOMException e) {
				if (childName.indexOf(" ") >= 0 || childName.indexOf("\t") >= 0 || childName.indexOf("\n") >= 0 || childName.indexOf("\r") >= 0)
					throw new X2DataException("Cannot creat node with spaces in the name (" + childName + ")");
				throw new X2DataException("Cannot create new node named '" + childName + "'");
			}
			parent.appendChild(child);

			notifyDocumentChanged();
			return child;
		} catch (TransformerException e) {
			throw new X2DataException("Error selecting text from document: " + e);
		}
	}

	/**
	 * Insert the contents of an XData document under an existing node. The <code>parent<code> element must
	 * not be in the document within the <code>data</code> parameter.
	 * @throws X2DataException 
	 */
	public static void createChild(Element parent, X2Data data) throws X2DataException {
		XSelectable selector = data.getSelector("xml-dom");
		DomXml domXml = (DomXml) selector;
		Node node = domXml.document.getDocumentElement();
		Document doc = parent.getOwnerDocument();
		Node newNode = doc.importNode(node, true);
		parent.appendChild(newNode);
	}

	public void insert(Node root, String xpath, Node childNode) throws X2DataException {
		// Check that the tag exists
		Node parent = checkElementExists(root, xpath);
		if (parent == null || !(parent instanceof Element))
			throw new X2DataException("XData.createChild: xpath does not resolve to an Element");

		createChild((Element) parent, childNode);
		notifyDocumentChanged();
	}

	/**
	 * Insert a node under an existing node.
	 * 
	 * 
	 * @param parent
	 *            The DOM node under which the new node should be created.
	 * @param name
	 *            The tag for the new node.
	 * @param value
	 *            The value of the new node.
	 */
	public static void createChild(Element parent, Node childNode) {
		// Take a copy of the new node
		Document doc = parent.getOwnerDocument();
		Node newNode = doc.importNode(childNode, true);
		parent.appendChild(newNode);
	}

	/**
	 * Insert a new text node under an existing node.
	 * 
	 * <B>Example</B><br>
	 * The following code demonstrates the use of <code>createChild</code>. <blockquote> String xml =<BR>
	 * &quot;&lt;product&gt;&quot; +<BR>
	 * &quot;&nbsp;&nbsp;&nbsp; &lt;code&gt;HAM01&lt;/code&gt;&quot; +<BR>
	 * &quot;&nbsp;&nbsp;&nbsp; &lt;description&gt;A large hammer&lt;/description&gt;&quot; +<BR>
	 * &quot;&lt;/product&gt;&quot;;<BR>
	 * XData data = new XData(xml);<BR>
	 * Node productNode = data.getXpathNode(&quot;/product&quot;, 0);<BR>
	 * XData.<B>createChild</B>(productNode, &quot;cost&quot;, &quot;$12.50&quot;);<BR>
	 * logger.debug(data.getXml());<BR>
	 * </blockquote> <I>Output:</I><br>
	 * <blockquote> &lt;product&gt;<BR>
	 * &nbsp;&nbsp;&nbsp; &lt;code&gt;HAM01&lt;/code&gt;<BR>
	 * &nbsp;&nbsp;&nbsp; &lt;description&gt;A large hammer&lt;/description&gt;<BR>
	 * &nbsp;&nbsp;&nbsp; &lt;cost&gt;$12.50&lt;/cost&gt;<BR>
	 * &lt;/product&gt; </blockquote>
	 * 
	 * @param parent
	 *            The DOM node under which the new node should be created.
	 * @param name
	 *            The tag for the new node.
	 * @param value
	 *            The value of the new node.
	 */
	public static void createChild(Element parent, String tag, String value) {
		Document doc = parent.getOwnerDocument();
		Element newElem = doc.createElement(tag);
		parent.appendChild(newElem);

		Text text = doc.createTextNode(value);
		newElem.appendChild(text);

		parent.appendChild(doc.createTextNode("\n"));
	}

	/**
	 * Get the value of tag directly below a specified DOM element within a DOM document.
	 * 
	 * @param elem
	 *            The parent DOM element or node.
	 * @param name
	 *            The tag of the required node.
	 * @return A string containing XML data.
	 */
	public static String getChildValue(Element elem, String name) {
		NodeList children = elem.getChildNodes();
		for (int k = 0; k < children.getLength(); k++) {
			Node child = children.item(k);
			if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE && child.getNodeName().equals(name)) {
				// Add together the text in the grandchild.
				String s = "";
				NodeList grandChildren = child.getChildNodes();
				for (int i = 0; i < grandChildren.getLength(); i++) {
					if (grandChildren.item(i).getNodeType() == org.w3c.dom.Node.TEXT_NODE)
						s += grandChildren.item(i).getNodeValue();
					else if (grandChildren.item(i).getNodeType() == org.w3c.dom.Node.CDATA_SECTION_NODE)
						s += grandChildren.item(i).getNodeValue();
				}
				return s;
			}
		}
		return null;
	}

	/**
	 * Recursively get the text within a specified DOM node. This method does not return XML - it returns all the text
	 * within the node and it's children.
	 * 
	 * @param elem
	 *            The specified element or node within a DOM document.
	 * @return The text within the node.
	 */
	public static String getText(Node elem) {
		if (elem.getNodeType() == org.w3c.dom.Node.TEXT_NODE || elem.getNodeType() == org.w3c.dom.Node.CDATA_SECTION_NODE)
			return elem.getNodeValue();

		String s = "";
		if (elem.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
			NodeList children = elem.getChildNodes();
			for (int k = 0; k < children.getLength(); k++) {
				Node child = children.item(k);
				s += DomXml.getText(child);
			}
		}
		return s;
	}

	/**
	 * Select nodes from one XData using an XPath, and insert them into this XData directly beneath the node with a
	 * specified XPath/index. The nodes are not removed from the source XData.
	 * 
	 * @param destinationXpath
	 * @param destinationIndex
	 * @param sourceXml
	 * @param sourcePath
	 * @throws X2DataException 
	 * @throws XDataException
	 */
	public void insert(String destinationXpath, int destinationIndex, X2Data sourceXml, String sourcePath) throws X2DataException {
		// Copy the input to this module into the exitData element (used to return from the linked module)
		Node destinationNode = this.getNode(destinationXpath, destinationIndex);
		
		DomXml selector = (DomXml) sourceXml.getSelector("xml-dom");
		NodeList nodeList = selector.getNodeList(sourcePath);
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node != null) {
				createChild((Element) destinationNode, node);
				this.notifyDocumentChanged();
			}
		}
	}

	/**
	 * Get a node that matches a specified XPATH. This method returns the index'th node that matches the provided XPath.
	 * If no suitable node can be found, return null.
	 * 
	 * @return The index'th nodes that matches the specified XPath.
	 * @param xpath
	 *            An XPath string to be matched.
	 * @param index
	 *            Which node to return.
	 * @throws X2DataException 
	 * @see getXpathNode
	 */
	public Node getNode(String xpath, int index) throws X2DataException {
		NodeList nodeList = getNodeList(xpath);
		if (index < 0 || index >= nodeList.getLength())
			throw new X2DataNotFoundException("no node with the specified index: getNode(\"" + xpath + "\", " + index + ")");
		return nodeList.item(index);
	}

	/**
	 * Get a node that matches a specified XPATH below a specifed starting point. This method returns the index'th node
	 * that matches the provided XPath. If no suitable node can be found, return null.
	 * 
	 * @return The index'th nodes that matches the specified XPath.
	 * @param xpath
	 *            An XPath string to be matched.
	 * @param index
	 *            Which node to return.
	 * @param target
	 *            The place from which to start the search.
	 * @throws X2DataException 
	 * @see getXpathNodeList
	 */
	public Node getNode(String xpath, Node target, int index) throws X2DataException {
		NodeList nl = getNodeList(xpath, target);
		if (index < 0 || index >= nl.getLength())
			throw new X2DataException("no node with the specified index: getNode(\"" + xpath + "\", " + index + ")");
		return nl.item(index);
	}

	/**
	 * Insert a node into this XData object at a specified position. Extra nodes will be added to create the parent if
	 * required.
	 * 
	 * @param xpath
	 *            The xpath for the parent node.
	 * @param childNode
	 *            The new node to be added.
	 */
	public void insert(String xpath, Node childNode) throws X2DataException {
		// Check that the tag exists
		Node root = document.getDocumentElement();
		insert(root, xpath, childNode);
	}

	/**
	 * Replace the text value of a node within the XML.
	 * @throws X2DataException 
	 */
	public void replace(String xpath, String value) throws X2DataException {
		// Check that the tag exists
		Node root = document.getDocumentElement();
		replace(root, xpath, value);
	}

	/**
	 * Replace the a node within an XML document with a node from another document.
	 * @throws X2DataException 
	 */
	public void replace(String xpath, X2Data sourceData, String srcXpath) throws X2DataException {
		// Find the node in the source
		DomXml domXml = (DomXml) sourceData.getSelector("xml-dom");
		Document srcDoc = domXml.getDocument();
		Node srcRoot = srcDoc.getDocumentElement();
		Node srcNode = checkElementExists(srcRoot, srcXpath);

		// Find the node in the destination
		Document destDoc = this.getDocument();
		Node destRoot = destDoc.getDocumentElement();
		Node destNode = checkElementExists(destRoot, xpath);

		// Delete the existing node
		Node nextSibling = destNode.getNextSibling();
		Node parentNode = destNode.getParentNode();
		parentNode.removeChild(destNode);

		// Add the new node into the same position
		Node newNode = destDoc.importNode(srcNode, true);
		parentNode.insertBefore(newNode, nextSibling);

		notifyDocumentChanged();
	}

	/**
	 * Replace the text value of a node within the XML.
	 * 
	 * @param root
	 *            Root of the tree.
	 * @param xpath
	 *            Position of the node to be replaced.
	 * @param value
	 *            If null, the node is removed, not replaced.
	 * @throws X2DataException
	 */
	public void replace(Node root, String xpath, String value) throws X2DataException {
		// Check that the tag exists
		Node parent = checkElementExists(root, xpath);

		// Delete any existing child nodes
		for ( ; ; ) {
			Node child = parent.getFirstChild();
			if (child == null)
				break;
			parent.removeChild(child);
		}

		// Add the new child node
		if (value != null) {
			Node newNode = parent.getOwnerDocument().createTextNode(value);
			parent.appendChild(newNode);
		}
		notifyDocumentChanged();
	}

	/**
	 * Insert all the nodes within another X2Data object into this X2Data object. Extra nodes will be added to create the
	 * parent if required.
	 * 
	 * @param xpath
	 *            The xpath for the node under which the document will be added.
	 * @param data
	 *            The document to be inserted.
	 */
	public void insert(String xpath, X2Data xd) throws X2DataException {
		// Check that the tag exists
		Node root = document.getDocumentElement();
		insert(root, xpath, xd);
	}

	/**
	 * Sort the elements in this data. The <i>xPath</i> parameter is the path to the element to be sorted. The sortFields
	 * parameter specifies the value(s) used to specify the sort order. This <i>SortFields</i> may contain multiple
	 * element names, separated by semicolons. A '#' before a name indicates that it should be treated as numeric.
	 * <p>
	 * Note that this method will force conversion to 'xml-dom', with the associated cost of parsing, etc. 
	 * 
	 * @param parentXPath
	 * 		The element above the elements that will be sorted.
	 * @param elementName
	 * 		The name of the elements to be sorted.
	 * @param sortFields
	 * 		Fields within the element that will be used to determine the ordering.
	 * @param isAscending
	 * 		Sort in increasing or decreasing order.
	 * @throws X2DataException 
	 */
	public void sortElements(String parentXPath, String elementName, String sortFields, boolean isAscending) throws X2DataException {
		if (elementName == null || elementName.equals("")) {
			int index = parentXPath.lastIndexOf("/");
			if (index < 0) {
				elementName = parentXPath;
				parentXPath = null;
			} else {
				elementName = parentXPath.substring(index + 1);
				parentXPath = parentXPath.substring(0, index);
			}
		}
		if (parentXPath == null || parentXPath.equals(""))
			parentXPath = "/*";

		// Break the sort field string into separate parts
		Vector<String> sortFieldsList = new Vector<String>();
		String str = sortFields;
		for (;;) {
			int pos = str.indexOf(";");
			if (pos < 0) {
				sortFieldsList.add(str.trim());
				break;
			}
			String part = str.substring(0, pos).trim();
			if (!part.equals(""))
				sortFieldsList.add(part);
			str = str.substring(pos + 1);
		}
		int numSortFields = sortFieldsList.size();
		String[] sortFieldNames = new String[numSortFields];
		boolean isNumeric[] = new boolean[numSortFields];
		for (int i = 0; i < numSortFields; i++) {
			String s = (String) sortFieldsList.elementAt(i);
			if (s.startsWith("#")) {
				isNumeric[i] = true;
				sortFieldNames[i] = s.substring(1).trim();
			} else {
				isNumeric[i] = false;
				sortFieldNames[i] = s;
			}
		}

		// Find all the parent nodes
		DomXmlList parents = this.getNodes(parentXPath);
		while (parents.next()) {
			Node parentNode = parents.getCurrentNode();
			DomXmlList children = parents.getNodes(elementName);
			Vector<DomXmlSortElement> elements = new Vector<DomXmlSortElement>(); // vector of SortNode
			while (children.next()) {

				Node childNode = children.getCurrentNode();
				Object sortValues[] = new Object[numSortFields];
				for (int i = 0; i < numSortFields; i++) {
					sortValues[i] = children.getString(sortFieldNames[i]);
					if (isNumeric[i]) {
						Long numericValue;
						try {
							numericValue = new Long((String) sortValues[i]);
						} catch (NumberFormatException e) {
							numericValue = new Long(Long.MIN_VALUE);
						}
						sortValues[i] = numericValue;
					}
				}
				DomXmlSortElement element = new DomXmlSortElement(childNode, sortValues, isAscending);
				elements.addElement(element);
				/*
				 * String stringVal = children.getText(sortFieldXPath); if (sortFieldIsInteger) { long val; try { val =
				 * Long.parseLong(stringVal); } catch (NumberFormatException e) { val = Long.MIN_VALUE; }
				 * XDataIntegerSortElement element = new XDataIntegerSortElement(childNode, val, isAscending);
				 * elements.addElement(element); } else { XDataStringSortElement element = new
				 * XDataStringSortElement(childNode, stringVal, isAscending); elements.addElement(element); } //String
				 * name = children.getText("name"); //logger.debug("name = " + name + ", sequence=" + stringVal);
				 */

				// Delete the child from the parent
				parentNode.removeChild(childNode);
				this.notifyDocumentChanged();
			}

			// Sort the list
			Collections.sort(elements);

			// Add the elements back to their parent
			for (int i = 0; i < elements.size(); i++) {
				DomXmlSortElement rec = (DomXmlSortElement) elements.elementAt(i);
				Node childNode = rec.getNode();
				// String str = XData.getText(childNode);
				// logger.debug("node is " + str);
				// parentNode.appendChild(childNode);
				createChild((Element) parentNode, childNode);
				this.notifyDocumentChanged();
			}
		}
	}

	/**
	 * Return the value of the first node that matches a specified XPATH below the element specified by <code>target</code>.
	 * 
	 * @return The text contained in the first node that matches the specified XPath.
	 * @param xpath
	 *            An XPath string to be matched.
	 * @param target
	 *            The place from which to start the search.
	 * @see getXpathNodeList
	 */
	public String getString(String xpath, Node target) throws X2DataException {
		NodeList nl = getNodeList(xpath, target);
		return getTextFromNodeList(nl);
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
	public String getString(String xpath, Node target, int index) throws X2DataException {
		try {
			XPathContext xpathSupport = new XPathContext();
			// XPathContext xpathSupport = hidden.getRootXPathContext(); ZZZZZZZZZ This might be better

			target = (target.getNodeType() == Node.DOCUMENT_NODE) ? ((Document) target).getDocumentElement() : target;
			PrefixResolver prefixResolver = new PrefixResolverDefault(target);
			int contextNode = xpathSupport.getDTMHandleFromNode(target);

			XPath _xpath = new XPath(xpath, null, prefixResolver, XPath.SELECT, null);
			NodeList nl = _xpath.execute(xpathSupport, contextNode, prefixResolver).nodelist();

			if (index < 0 || index >= nl.getLength())
				throw new X2DataException("no node with the specified index: getNode(\"" + xpath + "\", " + index + ")");
			Node node = nl.item(index);
			String string = getText(node);
			return string;
		} catch (TransformerException e) {
			throw new X2DataException("Error selecting text from document: " + e);
		}
	}

	/**
	 * Return a string representation of this XML document.
	 */
	public String getXml() {
		String string = DomXml.domToXml(this.document);
		return string;
	}

	/**
	 * Convert a DOM document object to XML.
	 * 
	 * @param document
	 * @return
	 * 	String representation of the XML document.
	 */
	public static String domToXml(Document document) {
		try {
	        DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();    
	        DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("XML 3.0 LS 3.0");
	        if (impl == null)
	            throw new X2DataException("No DOMImplementation found.");
			
			LSSerializer serializer = impl.createLSSerializer();
//	        LSOutput output = impl.createLSOutput();
//	        output.setEncoding("UTF-8");
//	        output.setByteStream(System.out);
//	        serializer.write(document, output);
//	        System.out.println();
	        
	        String string = serializer.writeToString(document);
			return string;
		} catch (Exception e) {
			return "<error>Could not Serialize DOM Document</error>";
		}
	}

	/**
	 * Return the string representation of a DOM XML node.
	 */
	public static String domToXml(Node node) {
		try {
			if (node instanceof Element) {
				Element elem = (Element) node;
		        DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();    
		        DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("XML 3.0 LS 3.0");
		        if (impl == null)
		            throw new X2DataException("No DOMImplementation found.");
				
				LSSerializer serializer = impl.createLSSerializer();
//		        LSOutput output = impl.createLSOutput();
//		        output.setEncoding("UTF-8");
//		        output.setByteStream(System.out);
//		        serializer.write(document, output);
//		        System.out.println();
		        
		        String string = serializer.writeToString(elem);
				return string;

			} else if (node instanceof Text) {
				Text text = (Text) node;
				return text.getNodeValue();
			} else if (node instanceof CDATASection) {
				CDATASection cdata = (CDATASection) node;
				return cdata.getNodeValue();
			} else
				return "";
		} catch (Exception e) {
			return "<error>Could not Serialize DOM Element</error>";
		}
	}

}
