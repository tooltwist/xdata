package com.dinaa.data;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpSession;
//import javax.servlet.jsp.PageContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.apache.xml.serialize.XMLSerializer;
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

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import com.dinaa.fastXml.FastXml;
import com.dinaa.fastXml.FastXmlNodes;
import com.dinaa.misc.AltLang;
import com.dinaa.xpc.ICredentials;

/**
 * General purpose XML data manipulation class. This class provides a convenient way to create and access XML data in either text or DOM (Document Object Model) form. Conversions between these forms are performed as required.
 * 
 * @author: Philip Callender
 */
public class XData implements IXData, Serializable {
	private static final long serialVersionUID = 3610205951976718872L;
	static Logger logger = Logger.getLogger(XData.class);
	public final static int NOTFOUND_CREATE = 1;
	public final static int NOTFOUND_EXCEPTION = 2;
	public final static int NOTFOUND_OK = 3;
	public final static int AT_TOP = 991;
	public final static int REPLACE = 993;
	public final static int AT_END = 992;
	private static final byte BOM1 = (byte) 0xFF;
	private static final byte BOM2 = (byte) 0xFE;

	// private java.lang.String xml = null;
	// private org.w3c.dom.Document document = null;

	// Set this using setTraceName to get trace messages
	private String traceName = null;

	private XDataHiddenStuff hidden = null;
	private boolean calledNextAlready;

	// private XMLParserLiaison xpathSupport = null;
	// private XPathProcessor xpathParser = null;
	/**
	 * Constructor for empty XML data. This creates an XML document equivalent to <I>&lt;empty/&gt;</I>.
	 */
	public XData(boolean useFastXML) {
		super();
		hidden = new XDataHiddenStuff(useFastXML);
		hidden.setXml("<empty/>");
	}

	public XData() {
		super();
		hidden = new XDataHiddenStuff(false);
		hidden.setXml("<empty/>");
	}

	/**
	 * Create an XData object from XML information read from a stream.
	 */
	@Deprecated
	public XData(Reader reader) throws XDataException {
		super();
		hidden = new XDataHiddenStuff(false);
		loadFromReader(reader);
	}

	@Deprecated
	public XData(Reader reader, boolean useFastXml) throws XDataException {
		super();
		hidden = new XDataHiddenStuff(useFastXml);
		loadFromReader(reader);
	}

	@Deprecated
	private void loadFromReader(Reader reader) throws XDataException {
		// Read the XML from a Reader object
		try {
			StringBuffer xml = new StringBuffer();
			char[] buf = new char[4096];
			for (;;) {
				int len = reader.read(buf);
				if (len < 0)
					break;
				xml.append(buf, 0, len);
			}
			reader.close();
			hidden.setXml(xml.toString());
		} catch (java.io.IOException e) {
			throw new XDataException("Error reading XML from Reader: " + e.getMessage());
		}
	}

	/**
	 * Load XML data from in input stream, with automatic detection of whether the file is Unicode, based upon BOM markers at the start of the file.
	 * 
	 * @param is
	 * @throws XDataException
	 */
	public XData(InputStream is) throws XDataException {
		super();
		hidden = new XDataHiddenStuff(false);
		loadFromReader(is);
	}

	public XData(InputStream is, boolean useFastXml) throws XDataException {
		super();
		hidden = new XDataHiddenStuff(useFastXml);
		loadFromReader(is);
	}

	/**
	 * Load XML from an input stream, while checking for Unicode character encoding, using one of the following:
	 * 
	 * 1. BOM characters at the start of the file { (byte)0xEF, (byte)0xBB, (byte)0xBF } 2. <?xml version="..." encoding="UTF-8"?> in the first line of the file
	 * 
	 * @param in
	 * @throws XDataException
	 */
	private void loadFromReader(InputStream reader) throws XDataException {
		try {
			String xml = loadStringFromReader(reader);
			this.hidden.setXml(xml);
		} catch (java.io.IOException e) {
			throw new XDataException("Error reading XML from Reader: " + e.getMessage());
		}
	}

	/**
	 * Read the contents of a file (or other Reader) and return it as a string, checking for Unicode conversion if required.
	 * 
	 * @param inputStream
	 * @return File contents as a String.
	 * @throws IOException
	 */
	public static String loadStringFromReader(InputStream inputStream) throws IOException {
		// Read the file contents from a Reader object
		ByteArrayOutputStream writer;
		byte[] buf = new byte[4 * 4096];
		writer = new ByteArrayOutputStream();
		String encoding = null;
		for (boolean startOfFile = true;; startOfFile = false) {
			int len = inputStream.read(buf);
			if (len < 0)
				break;
			if (startOfFile && len >= 3 && ((buf[0] == BOM1 && buf[1] == BOM2) || (buf[0] == BOM2 && buf[1] == BOM1)))
				encoding = "UTF-16";
			writer.write(buf, 0, len);
		}
		inputStream.close();

		byte[] array = writer.toByteArray();

		String contents = (encoding == null) ? new String(array) : new String(array, encoding);
		return contents;
	}

	/**
	 * Construct an XData object from a String.
	 * <P>
	 * 
	 * Example:<blockquote> String xml =<BR>
	 * &nbsp;&nbsp;&nbsp; &quot;&lt;Dinaa domain=\&quot;demo\&quot;&gt;&quot; +<BR>
	 * &nbsp;&nbsp;&nbsp; &quot;&nbsp;&nbsp;&nbsp; &lt;product op=\&quot;select\&quot;/&gt;&quot; +<BR>
	 * &nbsp;&nbsp;&nbsp; &quot;&nbsp;&lt;/Dinaa&gt;&quot;;<BR>
	 * XData input = new XData(xml); </blockquote>
	 */
	public XData(String xml) {
		super();
		hidden = new XDataHiddenStuff(false);
		if (xml == null)
			xml = "<empty/>";
		hidden.setXml(xml);
	}

	public XData(String xml, boolean useFastXml) {
		super();
		hidden = new XDataHiddenStuff(useFastXml);
		hidden.setXml(xml);
	}

	public XData(StringBuffer xml) {
		super();
		hidden = new XDataHiddenStuff(false);
		hidden.setXml(xml.toString());
	}

	public XData(StringBuffer xml, boolean useFastXml) {
		super();
		hidden = new XDataHiddenStuff(useFastXml);
		hidden.setXml(xml.toString());
	}

	/**
	 * Construct XData object from the DOM (Document Object Model) document object.
	 */
	public XData(Document doc) {
		super();
		hidden = new XDataHiddenStuff(false);
		hidden.setDocument(doc);
	}

	/**
	 * Construct XData object from the DOM (Document Object Model) node object.
	 */
	public XData(Node node) throws XDataException {
		super();
		hidden = new XDataHiddenStuff(false);
		/*
		 * old xerces document = new DocumentImpl(); Node newNode = document.importNode(node, true); document.appendChild(newNode);
		 */

		try {
			// Create the new document
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document document = db.newDocument();
			Node newNode = document.importNode(node, true);
			// Node newNode = node;
			document.appendChild(newNode);

			// Save it
			hidden.setDocument(document);
		} catch (ParserConfigurationException e) {
			throw new XDataException("Error contructing XData: " + e);
		}
	}

	/**
	 * Get a node that matches a specified XPATH. This method returns the index'th node that matches the provided XPath, starting at <code>root</code>. If no suitable node can be found, return null.
	 * 
	 * @return The index'th nodes that matches the specified XPath.
	 * @param code
	 *            The root from which to start the search
	 * @param xpath
	 *            An XPath string to be matched.
	 * @param index
	 *            Which node to return.
	 * @see getXpathNode
	 */
	private Node checkElementExists(Node root, String xpath) throws XDataException {
		if (traceName != null)
			logger.debug("XData[" + traceName + "].checkElementExists(Node root, xpath=" + xpath + ")");

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
				throw new XDataException("Cannot create xpath - " + xpath);
			int pos = xpath.lastIndexOf('/');
			String parentXpath = (pos >= 0) ? xpath.substring(0, pos) : null;
			String childName = (pos >= 0) ? xpath.substring(pos + 1) : xpath;
			childName = childName.trim();

			// If the child name has special xpath characters then we can't create it
			if (childName.length() == 0)
				throw new XDataException("Cannot create xPath - " + xpath + " - no child directory specified");
			if (childName.indexOf("[") >= 0 || childName.indexOf("]") >= 0)
				throw new XDataException("Cannot create xPath - " + xpath + " - wildcard characters in name");

			// Get the parent, then add the new child
			Node parent = (parentXpath == null) ? root : checkElementExists(root, parentXpath);
			Node child;
			try {
				child = parent.getOwnerDocument().createElement(childName);
			} catch (org.w3c.dom.DOMException e) {
				if (childName.indexOf(" ") >= 0 || childName.indexOf("\t") >= 0 || childName.indexOf("\n") >= 0 || childName.indexOf("\r") >= 0)
					throw new XDataException("Cannot creat node with spaces in the name (" + childName + ")");
				throw new XDataException("Cannot create new node named '" + childName + "'");
			}
			parent.appendChild(child);

			notifyDocumentChanged();
			return child;
		} catch (TransformerException e) {
			throw new XDataException("Error selecting text from document: " + e);
		}
	}

	/**
	 * Set values in the module, using information passed in the request to a servlet. This method is used in conjunction with <code>xmlFromValues</code> and <code>xmlToValues</code> to allow XML data to be modified by a Java Server Page (JSP). In particular, it handles the case where a JSP only edits some of the values in the XML, but we need to keep the rest of the XML intact.
	 * <P>
	 * 
	 * The following steps are used:<br>
	 * 1. Copy fields from the xml data into variables in the module using <code>xmlToValues</code>.<br>
	 * 2. Display a JSP and allow the user to edit the values.<br>
	 * 3. When the servlet receives the post from the JSP, use fields in the servlet request to update the module values, using <code>copyValuesFromRequest</code>.<br>
	 * 4. Go to other JSPs if required.<br>
	 * 5. Create the new XML using <code>xmlFromValues</code>.
	 * <P>
	 * 
	 * <B>Rules</B><br>
	 * A developer specifies which fields should be copied using a rules parameter. This is an array of field mappings between the request and the values in the request to the servlet, and the values in the module. The general form of these mappings is:<blockquote> <i>parameterName valueName</i> [options] </blockquote> Options may be one of the following:<br>
	 * <B>optional</B><br>
	 * If this option is not specified, and the parameter was not received by the servlet, an <code>XDataValidationException</code> is thrown.<br>
	 * <B>not-empty</B><br>
	 * This option causes an <code>XDataValidationException</code> to be thrown if the parameter is empty.<br>
	 * <B>checkbox</B><br>
	 * Treats the parameter as a checkbox. If the parameter is null, the value "N" is used, else the value "Y" is used.<br>
	 * <B>wildcard</B><br>
	 * Wildcards are used to copy entire sets of parameters and values. This saves time, but also allows a servlet to copy data without explicitly naming all the fields to be copied. This gives the JSP limited control over what is stored in the XML data. For wildcards, only the prefix of the parameter and module value need to be specified. As a matter of convention, end the wildcard with an
	 * underscore (_). For checkbox wildcards, add '_cb' to the prefix. For mandatory fields, add <i>M</i> before the underscore.
	 * 
	 * @param req
	 *            The request parameter passed to the servlet.
	 * @param rules
	 *            Rules specifying which parameter values to copy into the module variables.
	 * @throws ModuleException
	 *             If the rules are invalid.
	 * @throws XDataValidationException
	 *             A parameter did not match the specified rules.
	 * 
	 * @see #xmlFromValues
	 * @see #xmlToValues
	 * 
	 */
	public void copyValuesFromRequest(HttpServletRequest req, String[] rules, XDataValidatorList validators) throws XDataException, XDataValidationException {
		final String methodName = "copyValuesFromRequest";

		if (traceName != null)
			logger.debug("XData[" + traceName + "].copyValuesFromRequest(HttpServletRequest req, String[] rules, XDataValidatorList validators)");

		// try {

		// Start checking the rules
		// BeanInfo bi = java.beans.Introspector.getBeanInfo(this.getClass());
		// PropertyDescriptor[] pd = bi.getPropertyDescriptors();
		for (int i = 0; i < rules.length; i++) {
			String s = rules[i];
			Vector<String> args = new Vector<String>();

			// This is a variable to be taken from the request
			// Remember the spaces at the front
			int firstChar = 0;
			for (; firstChar < s.length(); firstChar++) {
				char c = s.charAt(firstChar);
				if (c != ' ' && c != '\t' && c != '\n' && c != '\r')
					break;
			}
			// convToZit String indent = s.substring(0, firstChar);

			// seperate the rule into individual arguments
			for (String tmp = s.substring(firstChar).trim(); !tmp.equals("");) {
				int pos = tmp.indexOf(' ');
				int pos2 = tmp.indexOf('\t');
				if (pos < 0 || (pos2 > 0 && pos2 < pos))
					pos = pos2;

				if (pos < 0) {
					args.addElement(tmp);
					break;
				} else {
					String word = tmp.substring(0, pos).trim();
					args.addElement(word);
					tmp = tmp.substring(pos + 1).trim();
				}
			}

			// Check the arguments/modifiers
			boolean optional = false;
			boolean notEmpty = false;
			boolean checkBox = false;
			boolean wildcard = false;
			boolean uppercase = false;
			if (args.size() < 2)
				throw new XDataException(this.getClass().getName() + "." + methodName + ": usage: [optional|non-null][checkbox] parameter xmlElement (not '" + s + "')");
			String parameter = (String) args.elementAt(0);
			String property = (String) args.elementAt(1);
			Vector<XDataValidator> genericValidations = new Vector<XDataValidator>();
			for (int argPos = 2; argPos < args.size(); argPos++) {
				String arg = (String) args.elementAt(argPos);
				if (arg.equalsIgnoreCase("optional"))
					optional = true;
				else if (arg.equalsIgnoreCase("not-empty"))
					notEmpty = true;
				else if (arg.equalsIgnoreCase("checkbox"))
					checkBox = true;
				else if (arg.equalsIgnoreCase("wildcard"))
					wildcard = true;
				else if (arg.equalsIgnoreCase("uppercase"))
					uppercase = true;
				else {
					boolean foundValidator = false;
					if (validators != null) {
						Enumeration<XDataValidator> enumx = validators.elements();
						while (enumx.hasMoreElements()) {
							XDataValidator v = (XDataValidator) enumx.nextElement();
							if (arg.equals(v.getName())) {
								foundValidator = true;
								genericValidations.add(v);
								break;
							}
						}
					}
					if (!foundValidator)
						throw new XDataException(this.getClass().getName() + "." + methodName + ": parameter '" + parameter + "': unknown option" + arg + "'");
				}
			}

			// Check for incompatible options
			if (optional && notEmpty)
				throw new XDataException(this.getClass().getName() + "." + methodName + ": parameter '" + parameter + "': cannot specify optional and not-empty options");

			// See if this is a wildcard
			if (wildcard) {
				/*
				 * Do wildcards - look for all parameters with the specified prefix.
				 */
				String fromPrefix = parameter.equals("*") ? "" : parameter;
				String toPrefix = property.equals("*") ? "" : property;
				/*
				 * ZZZZZZZZZZZZZZZZZ // Reset all checkbox values int pos = toPrefix.indexOf("/"); String parent = toPrefix.substring(0, pos); String childSuffix = toPrefix.substring(pos+1); Node parentNode = getXPathNode(parent, 0); NodeList children = parentNode.getChildNodes(); children. for (Node child = ; ; ) { ZZZZZZZZZZZZZZZZZZZZ
				 */

				/*
				 * ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ This needs to be done Enumeration enumx = values.keys(); while (enumx.hasMoreElements()) { String name = (String) enumx.nextElement(); if (!name.equals("") && !name.startsWith(fromPrefix)) continue; setValue(name, "N"); } ZZZZZZZZZZZZZZZZZZZZZZZZ
				 */

				// Look for values to set
				Enumeration<String> enumx = req.getParameterNames();
				while (enumx.hasMoreElements()) {
					String name = (String) enumx.nextElement();
					if (!name.equals("") && !name.startsWith(fromPrefix))
						continue;

					String suffix = name.substring(fromPrefix.length());
					if (suffix.equals(""))
						throw new XDataValidationException("Parameter name exactly matches wildcard prefix - name must be longer");
					String newName = toPrefix + name.substring(fromPrefix.length());
					newName = newName.trim();
					String val = req.getParameter(name);
					if (checkBox)
						val = (val == null) ? "N" : "Y";
					if (uppercase)
						val = val.toUpperCase();
					if (val.trim().equals("") && notEmpty)
						throw new XDataValidationException(parameter, "Missing Value");
					// Check the named validations that use pluggins
					for (int vCnt = 0; vCnt < genericValidations.size(); vCnt++) {
						XDataValidator fv = (XDataValidator) genericValidations.elementAt(vCnt);
						val = fv.validate(name, newName, val); // will throw XDataValidationException if a problem
					}
					if (newName.indexOf(" ") >= 0 || newName.indexOf("\t") >= 0)
						throw new XDataValidationException("Parameter name contains spaces (" + newName + ")");
					replace(newName, val);
				}
			} else {
				/*
				 * Not a wildcard - see if the value is set.
				 */
				// Get the value from the servlet request
				String val = req.getParameter(parameter);
				if (checkBox)
					val = (val == null) ? "N" : "Y";
				else if (val == null) {
					if (optional)
						continue;
					throw new XDataValidationException(this.getClass().getName() + "." + methodName + ": parameter '" + parameter + "' not in HttpServletRequest");
				}
				if (uppercase)
					val = val.toUpperCase();
				if (val.trim().equals("") && notEmpty)
					throw new XDataValidationException(parameter, "Missing Value");

				// Save the value in the module
				replace(property, val);
			}

		}
		return;
		/*
		 * } catch (java.beans.IntrospectionException e) { throw new ModuleException(this.getClass() + "." + methodName + ": " + e); } catch (java.lang.IllegalAccessException e) { throw new ModuleException(this.getClass() + "." + methodName + ": " + e); } catch (java.lang.reflect.InvocationTargetException e) { throw new ModuleException(this.getClass() + "." + methodName + ": " + e); }
		 */
	}

	/**
	 * Set values in the module, using information passed in the request to a servlet. This method is used in conjunction with <code>xmlFromValues</code> and <code>xmlToValues</code> to allow XML data to be modified by a Java Server Page (JSP). In particular, it handles the case where a JSP only edits some of the values in the XML, but we need to keep the rest of the XML intact.
	 * <P>
	 * 
	 * The following steps are used:<br>
	 * 1. Copy fields from the xml data into variables in the module using <code>xmlToValues</code>.<br>
	 * 2. Display a JSP and allow the user to edit the values.<br>
	 * 3. When the servlet receives the post from the JSP, use fields in the servlet request to update the module values, using <code>copyValuesFromRequest</code>.<br>
	 * 4. Go to other JSPs if required.<br>
	 * 5. Create the new XML using <code>xmlFromValues</code>.
	 * <P>
	 * 
	 * <B>Rules</B><br>
	 * A developer specifies which fields should be copied using a rules parameter. This is an array of field mappings between the request and the values in the request to the servlet, and the values in the module. The general form of these mappings is:<blockquote> <i>parameterName valueName</i> [options] </blockquote> Options may be one of the following:<br>
	 * <B>optional</B><br>
	 * If this option is not specified, and the parameter was not received by the servlet, an <code>XDataValidationException</code> is thrown.<br>
	 * <B>not-empty</B><br>
	 * This option causes an <code>XDataValidationException</code> to be thrown if the parameter is empty.<br>
	 * <B>checkbox</B><br>
	 * Treats the parameter as a checkbox. If the parameter is null, the value "N" is used, else the value "Y" is used.<br>
	 * <B>wildcard</B><br>
	 * Wildcards are used to copy entire sets of parameters and values. This saves time, but also allows a servlet to copy data without explicitly naming all the fields to be copied. This gives the JSP limited control over what is stored in the XML data. For wildcards, only the prefix of the parameter and module value need to be specified. As a matter of convention, end the wildcard with an
	 * underscore (_). For checkbox wildcards, add '_cb' to the prefix. For mandatory fields, add <i>M</i> before the underscore.
	 * 
	 * @param req
	 *            The request parameter passed to the servlet.
	 * @param rules
	 *            Rules specifying which parameter values to copy into the module variables.
	 * @throws ModuleException
	 *             If the rules are invalid.
	 * @throws XDataValidationException
	 *             A parameter did not match the specified rules.
	 * 
	 * @see #xmlFromValues
	 * @see #xmlToValues
	 * 
	 */
	public void copyValuesFromRequest(HttpServletRequest req, String[] rules, Hashtable<String, XDataValidator> validators) throws XDataException {
		final String methodName = "copyValuesFromRequest";

		if (traceName != null)
			logger.debug("XData[" + traceName + "].copyValuesFromRequest(HttpServletRequest req, String[] rules, Hashtable validators)");

		// try {

		// Start checking the rules
		// BeanInfo bi = java.beans.Introspector.getBeanInfo(this.getClass());
		// PropertyDescriptor[] pd = bi.getPropertyDescriptors();
		for (int i = 0; i < rules.length; i++) {
			String s = rules[i];
			Vector<String> args = new Vector<String>();

			// This is a variable to be taken from the request
			// Remember the spaces at the front
			int firstChar = 0;
			for (; firstChar < s.length(); firstChar++) {
				char c = s.charAt(firstChar);
				if (c != ' ' && c != '\t' && c != '\n' && c != '\r')
					break;
			}
			// convToZit String indent = s.substring(0, firstChar);

			// seperate the rule into individual arguments
			for (String tmp = s.substring(firstChar).trim(); !tmp.equals("");) {
				int pos = tmp.indexOf(' ');
				int pos2 = tmp.indexOf('\t');
				if (pos < 0 || (pos2 > 0 && pos2 < pos))
					pos = pos2;

				if (pos < 0) {
					args.addElement(tmp);
					break;
				} else {
					String word = tmp.substring(0, pos).trim();
					args.addElement(word);
					tmp = tmp.substring(pos + 1).trim();
				}
			}

			// Check the arguments/modifiers
			boolean optional = false;
			boolean notEmpty = false;
			boolean checkBox = false;
			boolean wildcard = false;
			boolean uppercase = false;
			if (args.size() < 2)
				throw new XDataException(this.getClass().getName() + "." + methodName + ": usage: [optional|non-null][checkbox] parameter xmlElement (not '" + s + "')");
			String parameter = (String) args.elementAt(0);
			String property = (String) args.elementAt(1);
			Vector<XDataValidator> genericValidations = new Vector<XDataValidator>();
			for (int argPos = 2; argPos < args.size(); argPos++) {
				String arg = (String) args.elementAt(argPos);
				if (arg.equalsIgnoreCase("optional"))
					optional = true;
				else if (arg.equalsIgnoreCase("not-empty"))
					notEmpty = true;
				else if (arg.equalsIgnoreCase("checkbox"))
					checkBox = true;
				else if (arg.equalsIgnoreCase("wildcard"))
					wildcard = true;
				else if (arg.equalsIgnoreCase("uppercase"))
					uppercase = true;
				else {
					boolean foundValidator = false;
					if (validators != null) {
						Enumeration<XDataValidator> enumx = validators.elements();
						while (enumx.hasMoreElements()) {
							XDataValidator v = (XDataValidator) enumx.nextElement();
							if (arg.equals(v.getName())) {
								foundValidator = true;
								genericValidations.add(v);
								break;
							}
						}
					}
					if (!foundValidator)
						throw new XDataException(this.getClass().getName() + "." + methodName + ": parameter '" + parameter + "': unknown option" + arg + "'");
				}
			}

			// Check for incompatible options
			if (optional && notEmpty)
				throw new XDataException(this.getClass().getName() + "." + methodName + ": parameter '" + parameter + "': cannot specify optional and not-empty options");

			// See if this is a wildcard
			if (wildcard) {
				/*
				 * Do wildcards - look for all parameters with the specified prefix.
				 */
				String fromPrefix = parameter.equals("*") ? "" : parameter;
				String toPrefix = property.equals("*") ? "" : property;
				/*
				 * ZZZZZZZZZZZZZZZZZ // Reset all checkbox values int pos = toPrefix.indexOf("/"); String parent = toPrefix.substring(0, pos); String childSuffix = toPrefix.substring(pos+1); Node parentNode = getXPathNode(parent, 0); NodeList children = parentNode.getChildNodes(); children. for (Node child = ; ; ) { ZZZZZZZZZZZZZZZZZZZZ
				 */

				/*
				 * ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ This needs to be done Enumeration enumx = values.keys(); while (enumx.hasMoreElements()) { String name = (String) enumx.nextElement(); if (!name.equals("") && !name.startsWith(fromPrefix)) continue; setValue(name, "N"); } ZZZZZZZZZZZZZZZZZZZZZZZZ
				 */

				// Look for values to set
				Enumeration<String> enumx = req.getParameterNames();
				while (enumx.hasMoreElements()) {
					String name = (String) enumx.nextElement();
					if (!name.equals("") && !name.startsWith(fromPrefix))
						continue;

					String suffix = name.substring(fromPrefix.length());
					if (suffix.equals(""))
						throw new XDataValidationException("Parameter name exactly matches wildcard prefix - name must be longer");
					String newName = toPrefix + name.substring(fromPrefix.length());
					newName = newName.trim();
					String val = req.getParameter(name);
					if (checkBox)
						val = (val == null) ? "N" : "Y";
					if (uppercase)
						val = val.toUpperCase();
					if (val.trim().equals("") && notEmpty)
						throw new XDataValidationException(parameter, "Missing Value");
					// Check the named validations that use pluggins
					for (int vCnt = 0; vCnt < genericValidations.size(); vCnt++) {
						XDataValidator fv = (XDataValidator) genericValidations.elementAt(vCnt);
						val = fv.validate(name, newName, val); // will throw XDataValidationException if a problem
					}
					if (newName.indexOf(" ") >= 0 || newName.indexOf("\t") >= 0)
						throw new XDataValidationException("Parameter name contains spaces (" + newName + ")");
					this.replace(newName, val);
				}
			} else {
				/*
				 * Not a wildcard - see if the value is set.
				 */
				// Get the value from the servlet request
				String val = req.getParameter(parameter);
				if (checkBox)
					val = (val == null) ? "N" : "Y";
				else if (val == null) {
					if (optional)
						continue;
					throw new XDataValidationException(this.getClass().getName() + "." + methodName + ": parameter '" + parameter + "' not in HttpServletRequest");
				}
				if (uppercase)
					val = val.toUpperCase();
				if (val.trim().equals("") && notEmpty)
					throw new XDataValidationException(parameter, "Missing Value");

				// Save the value in the data
				replace(property, val);
			}

		}
		return;
		/*
		 * } catch (java.beans.IntrospectionException e) { throw new ModuleException(this.getClass() + "." + methodName + ": " + e); } catch (java.lang.IllegalAccessException e) { throw new ModuleException(this.getClass() + "." + methodName + ": " + e); } catch (java.lang.reflect.InvocationTargetException e) { throw new ModuleException(this.getClass() + "." + methodName + ": " + e); }
		 */
	}

	/**
	 * Insert the contents of an XData document under an existing node. The parent node must not be in the document within the <code>data</code> parameter.
	 * 
	 * 
	 * @param parent
	 *            The DOM node under which the new node should be created.
	 * @param name
	 *            The tag for the new node.
	 * @param value
	 *            The value of the new node.
	 */
	public static void createChild(Element parent, XData data) throws XDataException {
		Node node = data.hidden.getDocument().getDocumentElement();
		Document doc = parent.getOwnerDocument();
		Node newNode = doc.importNode(node, true);
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
	public static void createChild(Element parent, String tag, String value) throws XDataException {
		Document doc = parent.getOwnerDocument();
		Element newElem = doc.createElement(tag);
		parent.appendChild(newElem);

		Text text = doc.createTextNode(value);
		newElem.appendChild(text);

		parent.appendChild(doc.createTextNode("\n"));
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
	public static void createChild(Element parent, Node childNode) throws XDataException {
		// Take a copy of the new node
		Document doc = parent.getOwnerDocument();
		Node newNode = doc.importNode(childNode, true);
		parent.appendChild(newNode);
	}

	/**
	 * Convert a DOM document to an XML string.
	 * 
	 * @param doc
	 *            The DOM document to convert.
	 * @return A string containing xml.
	 */
	public static String domToXml(Document doc) {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			XMLSerializer ser = new XMLSerializer(os, null);
			ser.serialize(doc);
			return os.toString();
		} catch (Exception e) {
			return "<error>Could not Serialize DOM Document</error>";
		}
	}

	/**
	 * Convert a node within a DOM document to an XML string.
	 * 
	 * @param node
	 *            The node to convert.
	 * @return A string containing xml.
	 */
	public static String domToXml(Node node) {
		try {
			if (node instanceof Element) {
				Element elem = (Element) node;
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				XMLSerializer ser = new XMLSerializer(os, null);
				ser.serialize(elem);
				return os.toString();
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

	/*
	 * Turn tracing on/off
	 */
	public void enableTrace(String traceName) {
		if (traceName != null)
			logger.debug("XData[" + traceName + "] - trace turned on");
		this.traceName = traceName;
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
	 * Get a DOM document from the XData. This document can then be manipulated directly using the DOM API. Note that if the document is changed in this way, method <code>notifyDocumentChanged</code> must be called to tell the XData object that the XML String may need to be recreated from it's internal DOM document.
	 * 
	 * @return org.w3c.dom.Document
	 * @see #notifyDocumentChanged
	 */
	public final Document getDocument() throws XDataException {
		if (traceName != null)
			logger.debug("XData[" + traceName + "].getDocument()");
		return hidden.getDocument();
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
	public Node getNode(String xpath, int index) throws XDataException, XDataNotFoundException {
		if (traceName != null)
			logger.debug("XData[" + traceName + "].getNode(String xpath, int index)");

		NodeList nl = getNodeList(xpath);

		if (index < 0 || index >= nl.getLength())
			throw new XDataNotFoundException("no node with the specified index: getNode(\"" + xpath + "\", " + index + ")");
		return nl.item(index);
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
	 * @deprecated
	 */
	public Node getNode(String xpath, int index, Node target) throws XDataException, XDataException {
		if (traceName != null)
			logger.debug("XData[" + traceName + "].getNode(String xpath, int index, Node target)");
		return getNode(xpath, target, index);
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
	public Node getNode(String xpath, Node target, int index) throws XDataException, XDataException {
		if (traceName != null)
			logger.debug("XData[" + traceName + "].getNode(String xpath, Node target, int index)");

		NodeList nl = getNodeList(xpath, target);
		if (index < 0 || index >= nl.getLength())
			throw new XDataException("no node with the specified index: getNode(\"" + xpath + "\", " + index + ")");
		return nl.item(index);
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
	public NodeList getNodeList(String xpath) throws XDataException, XDataNotFoundException {
		// com.myl.Debug.showMemory("XData.getNodeList 1");
		if (traceName != null)
			logger.debug("XData[" + traceName + "].getNode(String xpath)");
		try {
			// com.myl.Debug.showMemory("XData.getNodeList 2");
			PrefixResolver prefixResolver = hidden.getPrefixResolver();
			// com.myl.Debug.showMemory("XData.getNodeList 3");
			XPathContext xpathSupport = hidden.getRootXPathContext();
			// com.myl.Debug.showMemory("XData.getNodeList 4");
			int contextNode = hidden.getRootContextNode();
			// com.myl.Debug.showMemory("XData.getNodeList 5");

			// Select the list of nodes
			XPath _xpath = new XPath(xpath, null, prefixResolver, XPath.SELECT, null);
			// com.myl.Debug.showMemory("XData.getNodeList 6");
			// int contextNode = xpathSupport.getDTMHandleFromNode(target);
			NodeList nl = _xpath.execute(xpathSupport, contextNode, prefixResolver).nodelist();
			// com.myl.Debug.showMemory("XData.getNodeList 7");
			return nl;
		} catch (TransformerException e) {
			throw new XDataNotFoundException("Error selecting xpath (" + xpath + ") from root: " + e);
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
	public NodeList getNodeList(String xpath, Node target) throws XDataException {
		if (traceName != null)
			logger.debug("XData[" + traceName + "].getNodeList(String xpath, Node target)");

		try {
			// XPathContext xpathSupport = new XPathContext();
			XPathContext xpathSupport = hidden.getRootXPathContext(); // ZZZZZZZZZ This might be better

			Node node = (target.getNodeType() == Node.DOCUMENT_NODE) ? ((Document) target).getDocumentElement() : target;
			PrefixResolverDefault prefixResolver = new PrefixResolverDefault(node);
			int contextNode = xpathSupport.getDTMHandleFromNode(target);

			// Select the list of nodes
			XPath _xpath = new XPath(xpath, null, prefixResolver, XPath.SELECT, null);
			// int contextNode = xpathSupport.getDTMHandleFromNode(target);
			NodeList nl = _xpath.execute(xpathSupport, contextNode, prefixResolver).nodelist();
			return nl;
		} catch (TransformerException e) {
			throw new XDataException("Error selecting xpath (" + xpath + ") below specified node: " + e);
		}
	}

	/**
	 * Get a list of nodes that match the specified XPATH.
	 * 
	 * @return A list of nodes that match the XPath.
	 * @param xpath
	 *            An XPath string to be matched.
	 * @see getXpathNode
	 */
	public XNodes getNodes(String xpath) throws XDataException, XDataNotFoundException {
		if (traceName != null)
			logger.debug("XData[" + traceName + "].getNodes(String xpath)");

		if (hidden.usingFastXml()) {
			FastXml fastXml = hidden.getFastXml();
			FastXmlNodes nodes = fastXml.getNodes(xpath);
			return new XNodes(nodes, this);
		} else {
			NodeList nl = getNodeList(xpath);
			return new XNodes(nl, this);
		}
	}

	/**
	 * Get a list of nodes that match the specified XPATH, starting the search from a specific DOM node.
	 * 
	 * @return A list of nodes that match the XPath.
	 * @param xpath
	 *            An XPath string to be matched.
	 * @param target
	 *            A node from which to start matching the XPath.
	 * @see getXpathNode
	 */
	public XNodes getNodes(String xpath, Node target) throws XDataException {
		if (traceName != null)
			logger.debug("XData[" + traceName + "].getNodes(String xpath, Node target)");

		NodeList nl = getNodeList(xpath, target);

		return new XNodes(nl, this);
	}

	public void checkParsed() throws XDataException {
		hidden.checkParsed();
	}

	/**
	 * Get the tag type of the highest element in the XML document.
	 * 
	 * @return The root tag.
	 */
	public Element getRootElement() {
		if (traceName != null)
			logger.debug("XData[" + traceName + "].getRootElement()");

		try {
			Document doc = hidden.getDocument();
			return doc.getDocumentElement();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Get the tag type of the highest element in the XML document.
	 * 
	 * @return The root tag.
	 */
	public String getRootType() {
		if (traceName != null)
			logger.debug("XData[" + traceName + "].getRootType()");

		try {
			if (hidden.usingFastXml()) {
				FastXml fastXml = hidden.getFastXml();
				return fastXml.getTagName(0);
			} else {
				Document doc = hidden.getDocument();
				Element elem = doc.getDocumentElement();
				return elem.getNodeName();
			}
		} catch (Exception e) {
			return "null";
		}
	}

	/**
	 * Return the text value in the first node that matches a specified XPATH.
	 * 
	 * @return The text contained in the first node that matches the specified XPath.
	 * @param xpath
	 *            An XPath string to be matched.
	 * @see getXpathNodeList
	 */
	public String getText(String xpath) throws XDataException, XDataNotFoundException {
		if (traceName != null)
			logger.debug("XData[" + traceName + "].getText(String xpath)");

		if (hidden.usingFastXml()) {
			FastXml fastXml = hidden.getFastXml();
			return fastXml.getText(xpath);
		} else {
			NodeList nl = getNodeList(xpath);
			return getTextFromNodeList(nl);
		}
	}

	/**
	 * Return the text value in the first node that matches a specified XPATH.
	 * 
	 * @return The text contained in the first node that matches the specified XPath.
	 * @param useAlternateLanguage
	 *            boolean flag that indicates if alternate language is on.
	 * @param xpath
	 *            An XPath string to be matched.
	 * @see getXpathNodeList
	 */
	public String getText(AltLang lang, String xpath) throws XDataException, XDataNotFoundException {
		String value = "";
		if (lang.useAlternateLanguage()) {
			value = getText(xpath + "AltLang");
			if (value.equals(""))
				value = getText(xpath);
		} else {
			value = getText(xpath);
			if (value.equals(""))
				value = getText(xpath + "AltLang");
		}
		return value;
	}

	public String getText(ICredentials cred, String xpath) throws XDataException, XDataNotFoundException {
		return getText(cred.getLang(), xpath);
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
	public String getText(String xpath, int index) throws XDataException, XDataNotFoundException {
		if (traceName != null)
			logger.debug("XData[" + traceName + "].getText(String xpath, int index)");
		if (hidden.usingFastXml()) {
			FastXml fastXml = hidden.getFastXml();
			return fastXml.getText(xpath, index);
		} else {
			NodeList nl = getNodeList(xpath);
			if (index < 0 || index >= nl.getLength())
				throw new XDataException("no node with the specified index: getNode(\"" + xpath + "\", " + index + ")");
			Node node = nl.item(index);
			return getText(node);
		}
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

	public String getText(String xpath, Node target) throws XDataException {
		if (traceName != null)
			logger.debug("XData[" + traceName + "].getText(String xpath, Node target)");

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
	 * @see getXpathNodeList
	 */
	public String getText(String xpath, Node target, int index) throws XDataException {
		if (traceName != null)
			logger.debug("XData[" + traceName + "].getText(String xpath, Node target, int index)");

		try {
			XPathContext xpathSupport = new XPathContext();
			// XPathContext xpathSupport = hidden.getRootXPathContext(); ZZZZZZZZZ This might be better

			target = (target.getNodeType() == Node.DOCUMENT_NODE) ? ((Document) target).getDocumentElement() : target;
			PrefixResolverDefault prefixResolver = new PrefixResolverDefault(target);
			int contextNode = xpathSupport.getDTMHandleFromNode(target);

			XPath _xpath = new XPath(xpath, null, prefixResolver, XPath.SELECT, null);
			NodeList nl = _xpath.execute(xpathSupport, contextNode, prefixResolver).nodelist();

			if (index < 0 || index >= nl.getLength())
				throw new XDataException("no node with the specified index: getNode(\"" + xpath + "\", " + index + ")");
			Node node = nl.item(index);
			return getText(node);
		} catch (TransformerException e) {
			throw new XDataException("Error selecting text from document: " + e);
		}
	}

	/**
	 * Recursively get the text within a specified DOM node. This method does not return XML - it returns all the text within the node and it's children.
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
				s += XData.getText(child);
			}
		}
		return s;
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
		if (traceName != null)
			logger.debug("XData[" + traceName + "].getTextFromNodeList(NodeList nodelist)");

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
	 * Get the contents of the XData as a String.
	 * 
	 * @return A String containing XML data.
	 * @see getDocument
	 */
	public String getXml() {
		if (traceName != null)
			logger.debug("XData[" + traceName + "].getXml()");

		return hidden.getXml();
	}

/**
 * Convert a string to safe HTML by escaping any special characters. This method should be used
 * everywhere where untrusted text is being returned to the browser, in order to prevent malicious
 * data containing Javascript from taking over the site.
 * 
 * For example, without this conversion being done, a used could insert the following when they enter
 * their name into a database:
 * 
 * 		"&lt;script&gt;alert("This site has been hacked!");&lt;/script&gt;"
 * 
 *	If the name is displayed without a conversion such as performed by this method, then the name will
 *	be interpreted as Javascript, and can be used for a cross-site scripting attack.
 * 
 *  Among other conversions, any '<' is converted to '&lt;' so it is impossible for scripts, HTML, styles, or
 *  any malicious output to be sent to the browser.
 *  
 * @input A string that should be displayed as normal text.
 * @return Safe text, with any dangerous characters replaced with escape sequences.
 */
	public static String htmlString(String input) {
		if (input == null)
			return "";
		return htmlString(input, true);
	}

	public static String htmlString(String input, boolean preserveNewlines) {
		if (input == null)
			return "";
		// Convert a String to HTML comparible characters
		char[] arr = input.toCharArray();
		// ByteArrayOutputStream os = new ByteArrayOutputStream();
		CharArrayWriter os = new CharArrayWriter();
		boolean previousWasSpace = false;
		for (int i = 0; i < arr.length; i++) {
			char ch = arr[i];

			if (ch == ' ') {
				// A space
				if (previousWasSpace) {
					os.write('&');
					os.write('n');
					os.write('b');
					os.write('s');
					os.write('p');
					os.write(';');
				} else {
					os.write(' ');
					previousWasSpace = true;
				}
				continue;
			} else {
				// Not a space
				previousWasSpace = false;
				switch (ch) {
				case '<':
					os.write('&');
					os.write('l');
					os.write('t');
					os.write(';');
					break;
				case '>':
					os.write('&');
					os.write('g');
					os.write('t');
					os.write(';');
					break;
				case '&':
					os.write('&');
					os.write('a');
					os.write('m');
					os.write('p');
					os.write(';');
					break;
				case '"':
					os.write('&');
					os.write('q');
					os.write('u');
					os.write('o');
					os.write('t');
					os.write(';');
					break;
				case '\'':
					os.write('&');
					os.write('#');
					os.write('3');
					os.write('9');
					os.write(';');
					break;
				case '\n':
					if (preserveNewlines) {
						os.write('<');
						os.write('b');
						os.write('r');
						os.write('>');
					} else
						os.write(ch);
					break;

				default:
					os.write(ch);
				}
			}
		}
		return os.toString();
	}

	public static String htmlStringWhiteSpace(String input, boolean preserveNewlines) {
		// Convert a String to HTML comparible characters
		char[] arr = input.toCharArray();
		// ByteArrayOutputStream os = new ByteArrayOutputStream();
		CharArrayWriter os = new CharArrayWriter();
		for (int i = 0; i < arr.length; i++) {
			char ch = arr[i];
			switch (ch) {
			case '<':
				os.write('&');
				os.write('l');
				os.write('t');
				os.write(';');
				break;
			case '>':
				os.write('&');
				os.write('g');
				os.write('t');
				os.write(';');
				break;
			case '&':
				os.write('&');
				os.write('a');
				os.write('m');
				os.write('p');
				os.write(';');
				break;
			case '"':
				os.write('&');
				os.write('q');
				os.write('u');
				os.write('o');
				os.write('t');
				os.write(';');
				break;
			case '\n':
				if (preserveNewlines) {
					os.write('<');
					os.write('b');
					os.write('r');
					os.write('>');
				} else
					os.write(ch);
				break;

			default:
				os.write(ch);
			}
		}
		return os.toString();
	}

	public static String quotedString(String input) {
		// Convert a String to HTML comparible characters
		char[] arr = input.toCharArray();
		CharArrayWriter os = new CharArrayWriter();
		for (int i = 0; i < arr.length; i++) {
			char ch = arr[i];
			switch (ch) {
			case '\\':
				os.write('\\');
				os.write('\\');
				break;
			case '\'':
				os.write('\\');
				os.write('\'');
				break;
			case '"':
				os.write('\\');
				os.write('"');
				break;
			case '\t':
				os.write('\\');
				os.write('t');
				break;
			case '\n':
				os.write('\\');
				os.write('n');
				break;
			case '\r':
				os.write('\\');
				os.write('r');
				break;

			default:
				os.write(ch);
			}
		}
		return os.toString();
	}

	/**
	 * Wrapped String by inserting a blank space for every n of characters.
	 * 
	 * @param input
	 *            The description to be wrapped.
	 * @param maxlen
	 *            Length of string to be wrapped.
	 * @return
	 */
	public static String wrappedString(String input, int maxlen) {
		char[] arr = input.toCharArray();
		StringBuffer strBuff = new StringBuffer(input);

		int ctr = 0;
		for (int i = 0; i < arr.length; i++) {
			if (ctr == maxlen) {
				strBuff.insert(i, ' ');
				ctr = 0;
			}
			ctr++;
		}

		return strBuff.toString();
	}

	/**
	 * Wrapped String by inserting a blank space for every n of characters.
	 * 
	 * @param input
	 *            The description to be wrapped.
	 * @param maxlen
	 *            Length of string to be wrapped.
	 * @return
	 */
	public static String wrappedStringWithBR(String input, int maxlen) {
		char[] arr = input.toCharArray();
		StringBuffer strBuff = new StringBuffer("");

		int ctr = 0;
		int i = 0;
		for (; i < arr.length; i++) {
			strBuff.append(arr[i]);
			if (ctr >= maxlen) {
				strBuff.append("<BR>");
				ctr = 0;
			}
			ctr++;
		}

		return strBuff.toString();
	}

	/**
	 * Insert all the nodes within another XData object into this XData object. Extra nodes will be added to create the parent if required.
	 * 
	 * @param xpath
	 *            The xpath for the node under which the document will be added.
	 * @param data
	 *            The document to be inserted.
	 * @see #setXPathText
	 */
	public void insert(String xpath, XData data) throws XDataException {
		if (traceName != null)
			logger.debug("XData[" + traceName + "].insert(String xpath, XData data)");

		// Check that the tag exists
		Node root = hidden.getDocument().getDocumentElement();
		insert(root, xpath, data);
	}

	/**
	 * Insert a node into this XData object at a specified position. Extra nodes will be added to create the parent if required.
	 * 
	 * @param xpath
	 *            The xpath for the parent node.
	 * @param childNode
	 *            The new node to be added.
	 * @see #setXPathText
	 */
	public void insert(String xpath, Node childNode) throws XDataException {
		if (traceName != null)
			logger.debug("XData[" + traceName + "].insert(String xpath, Node childNode)");

		// Check that the tag exists
		Node root = hidden.getDocument().getDocumentElement();
		insert(root, xpath, childNode);
	}

	/**
	 * Insert all the nodes within another XData object into this XData object. Extra nodes will be added to create the parent if required.
	 * 
	 * @param xpath
	 *            The xpath for the node under which the document will be added.
	 * @param data
	 *            The document to be inserted.
	 * @see #setXPathText
	 */
	public void insert(Node root, String xpath, XData data) throws XDataException {
		if (traceName != null)
			logger.debug("XData[" + traceName + "].insert(Node root, String xpath, XData data)");

		// Check that the tag exists
		Node parent = checkElementExists(root, xpath);
		if (parent == null || !(parent instanceof Element))
			throw new XDataException("XData.createChild: xpath does not resolve to an Element");

		XData.createChild((Element) parent, data);
		notifyDocumentChanged();
	}

	/**
	 * Insert a node into this XData object at a specified position. Extra nodes will be added to create the parent if required.
	 * 
	 * @param xpath
	 *            The xpath for the parent node.
	 * @param childNode
	 *            The new node to be added.
	 * @see #setXPathText
	 */
	public void insert(Node root, String xpath, Node childNode) throws XDataException {
		if (traceName != null)
			logger.debug("XData[" + traceName + "].insert(Node root, String xpath, Node childNode)");

		// Check that the tag exists
		Node parent = checkElementExists(root, xpath);
		if (parent == null || !(parent instanceof Element))
			throw new XDataException("XData.createChild: xpath does not resolve to an Element");

		XData.createChild((Element) parent, childNode);
		notifyDocumentChanged();
	}

	/**
	 * Select nodes from one XData using an XPath, and insert them into this XData directly beneath the node with a specified XPath/index. The nodes are not removed from the source XData.
	 * 
	 * @param destinationXpath
	 * @param destinationIndex
	 * @param sourceXml
	 * @param sourcePath
	 * @throws XDataException
	 */
	public void insert(String destinationXpath, int destinationIndex, XData sourceXml, String sourcePath) throws XDataException, XDataNotFoundException {

		// Copy the input to this module into the exitData element (used to return from the linked module)
		Node destinationNode = this.getNode(destinationXpath, destinationIndex);
		XNodes insertNodes = sourceXml.getNodes(sourcePath);
		while (insertNodes.next()) {
			Node node = insertNodes.getCurrentNode();
			if (node != null) {
				XData.createChild((Element) destinationNode, node);
				this.notifyDocumentChanged();
			}
		}

	}

	/**
	 * Get a list of nodes from an XData object stored in the session object, from a JSP.
	 * 
	 * @deprecated
	 */
//	public static XNodes jspGetNodesZZZ(PageContext pageContext, String beanName, int actionIfNotFound, String xpath) throws ServletException, XDataException, XDataNotFoundException {
//		try {
//			XData xmlData = null;
//			HttpSession session = pageContext.getSession();
//			synchronized (session) {
//				Object obj = pageContext.getAttribute(beanName, PageContext.SESSION_SCOPE);
//				if (obj == null) {
//					switch (actionIfNotFound) {
//					case NOTFOUND_CREATE:
//						xmlData = new XData("<empty/>");
//						pageContext.setAttribute(beanName, xmlData, PageContext.SESSION_SCOPE);
//						break;
//
//					case NOTFOUND_EXCEPTION:
//						throw new XDataException("Xml data '" + beanName + "' not found in session object");
//
//					case NOTFOUND_OK:
//						return null;
//					}
//				} else if (!(obj instanceof XData))
//					throw new ServletException("Bean '" + beanName + "' is not class " + XData.class.getName());
//				xmlData = (XData) obj;
//
//			}
//			return xmlData.getNodes(xpath);
//		} catch (XDataException e) {
//			throw new ServletException(e);
//		}
//	}

//	/**
//	 * Get XML from the session object, from a JSP.
//	 * 
//	 * @deprecated
//	 */
//	public static XData jspGetXDataZZ(PageContext pageContext, String beanName, int actionIfNotFound) throws ServletException, XDataException {
//		try {
//			XData xmlData = null;
//			HttpSession session = pageContext.getSession();
//			synchronized (session) {
//				Object obj = pageContext.getAttribute(beanName, PageContext.SESSION_SCOPE);
//				if (obj == null) {
//					switch (actionIfNotFound) {
//					case NOTFOUND_CREATE:
//						xmlData = new XData("<empty/>");
//						pageContext.setAttribute(beanName, xmlData, PageContext.SESSION_SCOPE);
//						break;
//
//					case NOTFOUND_EXCEPTION:
//						throw new XDataException("Xml data '" + beanName + "' not found in session object");
//
//					case NOTFOUND_OK:
//						return null;
//					}
//				} else if (!(obj instanceof XData))
//					throw new ServletException("Bean '" + beanName + "' is not class " + XData.class.getName());
//				xmlData = (XData) obj;
//
//			}
//			return xmlData;
//		} catch (XDataException e) {
//			throw new ServletException(e);
//		}
//	}

	/**
	 * Notify this class that it's internal DOM document has been changed by external code. This method should be called if the document returned by <code>getDocument</code> is modified in any way. This method lets the object know that it's internal caching needs to be flushed. Any pre-stored value based on the contents of the DOM document will need to be checked.
	 * 
	 * @see #getDocument
	 */
	public void notifyDocumentChanged() {
		if (traceName != null)
			logger.debug("XData[" + traceName + "].notifyDocumentChanged()");

		hidden.notifyDocumentChanged();
	}

	/**
	 * Tell the XData object to use DOM (Document Object Model) to parse the XML. This method should be called if: a) You wish to manipulate the document, or b) You wish to access DOM Nodes within the XData object.
	 */
	public void useDOM() {
		hidden.useDOM();
	}

	/**
	 * Replace the a node within an XML document with a node from another Xml document.
	 * 
	 * @param xpath
	 * @param value
	 * @throws XDataException
	 * @throws XDataException
	 */
	public void replace(String xpath, XData sourceData, String srcXpath) throws XDataException, XDataException {
		if (traceName != null)
			logger.debug("XData[" + traceName + "].replace(String xpath, XData sourceData, String srcXpath)");

		// Find the node in the source
		Document srcDoc = sourceData.getDocument();
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
	 * @param xpath
	 * @param value
	 * @throws XDataException
	 * @throws XDataException
	 */
	public void replace(String xpath, String value) throws XDataException, XDataException {
		if (traceName != null)
			logger.debug("XData[" + traceName + "].replace(String xpath, String value)");

		// Check that the tag exists
		Document doc = hidden.getDocument();
		Node root = doc.getDocumentElement();
		replace(root, xpath, value);
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
	 * @throws XDataException
	 * @throws XDataException
	 */
	public void replace(Node root, String xpath, String value) throws XDataException, XDataException {
		if (traceName != null)
			logger.debug("XData[" + traceName + "].replace(Node root, String xpath, String value)");

		// Check that the tag exists
		Node parent = checkElementExists(root, xpath);

		// Delete any existing child nodes
		for (;;) {
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
	 * Create a new node below the root node of a DOM document.
	 * 
	 * @param name
	 *            The name of the new node.
	 * @param value
	 *            The value of the new node.
	 * @deprecated
	 */
	public void setText(String name, String value) throws XDataException {
		if (traceName != null)
			logger.debug("XData[" + traceName + "].setText(String name, String value)");

		setText(name, value, null, AT_END);
	}

	/**
	 * Create a new node below an existing node in a document. The <code>pos</code> parameter specifies where the new node should be added:<BR>
	 * <B>AT_END</B><br>
	 * After all other elements with the same name.<br>
	 * <B>AT_TOP</B><br>
	 * Before all other elements with the same name.
	 * <P>
	 * 
	 * If <code>parent</code> is null, add the new node directly below the root node.
	 * 
	 * @param name
	 *            The name of the new node.
	 * @param value
	 *            The value of the new node.
	 * @param parent
	 *            The node under which the new node is added.
	 * @param pos
	 *            Where should the new node be added, relative to existing nodes with the same name.
	 * @deprecated
	 */
	public void setText(String name, String value, Element parent, int pos) throws XDataException {
		if (traceName != null)
			logger.debug("XData[" + traceName + "].setText(String name, String value, Element parent, int pos)");

		Document doc = hidden.getDocument();
		if (parent == null)
			parent = doc.getDocumentElement();

		// Create the new element
		Element newElem = doc.createElement(name);
		Text text = doc.createTextNode(value);
		newElem.appendChild(text);

		// Look for a node with this name
		NodeList nodes = parent.getElementsByTagName(name);
		if (nodes.getLength() == 0) {
			if (pos == AT_TOP)
				parent.insertBefore(newElem, parent.getFirstChild());
			else
				parent.appendChild(newElem);
		} else {
			Node node = nodes.item(0);
			parent.replaceChild(newElem, node);
		}
		hidden.notifyDocumentChanged();
	}

	/**
	 * Add a new node below the root node of a DOM document.
	 * 
	 * @param name
	 *            The name of the new node.
	 * @param value
	 *            The node to be inserted into the document.
	 * @deprecated
	 */
	public void setValue(String name, Node value) throws XDataException {
		if (traceName != null)
			logger.debug("XData[" + traceName + "].setValue(String name, Node value)");

		setValue(name, value, null, AT_END);
	}

	/**
	 * Add a new node below an existing node in a document. The <code>pos</code> parameter specifies where the new node should be added:<BR>
	 * <B>AT_END</B><br>
	 * After all other elements with the same name.<br>
	 * <B>AT_TOP</B><br>
	 * Before all other elements with the same name.
	 * <P>
	 * 
	 * If <code>parent</code> is null, add the new node directly below the root node.
	 * 
	 * @param name
	 *            The name of the new node.
	 * @param value
	 *            The node to be inserted into the document.
	 * @param parent
	 *            The node under which the new node is added.
	 * @param pos
	 *            Where should the new node be added, relative to existing nodes with the same name.
	 * @deprecated
	 */
	public void setValue(String name, Node value, Element parent, int pos) throws XDataException {
		if (traceName != null)
			logger.debug("XData[" + traceName + "].setValue(String name, Node value, Element parent, int pos)");

		Document doc = hidden.getDocument();
		if (parent == null)
			parent = doc.getDocumentElement();

		// Create the new node
		Element elem = doc.createElement(name);
		if (value.getOwnerDocument() != doc)
			value = doc.importNode(value, true);
		elem.appendChild(value);

		// Look for a node with this name
		NodeList nodes = parent.getElementsByTagName(name);
		if (nodes.getLength() == 0) {
			if (pos == AT_TOP)
				parent.insertBefore(elem, parent.getFirstChild());
			else
				parent.appendChild(elem);
		} else {
			Node node = nodes.item(0);
			parent.replaceChild(value, node);
		}

		hidden.notifyDocumentChanged();
	}

	/**
	 * Use sortElements instead
	 * 
	 * @deprecated
	 */
	public void sort(String parentXPath, String childType, String sortFields, boolean sortFieldIsInteger) throws XDataException, XDataNotFoundException {
		if (sortFieldIsInteger)
			sortFields = "#" + sortFields;
		sortElements(parentXPath, childType, sortFields, true);
	}

	/**
	 * Use sortElements instead
	 * 
	 * @deprecated
	 */
	public void sort(String xPath, String sortFields, boolean sortFieldIsInteger) throws XDataException, XDataNotFoundException {
		if (sortFieldIsInteger)
			sortFields = "#" + sortFields;
		sortElements(xPath, null, sortFields, true);
	}

	/**
	 * Use sortElements instead
	 * 
	 * @deprecated
	 */
	public void sort(String xPath, String sortFields, boolean sortFieldIsInteger, boolean isAscending) throws XDataException, XDataNotFoundException {
		if (sortFieldIsInteger)
			sortFields = "#" + sortFields;
		sortElements(xPath, null, sortFields, isAscending);
	}

	/**
	 * Sort the elements in an XData. The <i>xPath</i> parameter is the path to the element to be sorted. The sortFields parameter specifies the value(s) used to specify the sort order. This <i>SortFields</i> may contain multiple element names, seperated by semicolons. A '#' before a name indicates that it should be treated as numeric.
	 * 
	 * @param xPath
	 * @param sortFields
	 * @param isAscending
	 * @throws XDataException
	 * @throws XDataNotFoundException
	 */
	public void sortElements(String xPath, String sortFields, boolean isAscending) throws XDataException, XDataNotFoundException {
		sortElements(xPath, null, sortFields, isAscending);
	}

	/**
	 * Sort the elements in an XData. The <i>xPath</i> parameter is the path to the element to be sorted. The sortFields parameter specifies the value(s) used to specify the sort order. This <i>SortFields</i> may contain multiple element names, seperated by semicolons. A '#' before a name indicates that it should be treated as numeric.
	 * 
	 * @param parentXPath
	 * @param elementName
	 * @param sortFields
	 * @param isAscending
	 * @throws XDataException
	 * @throws XDataNotFoundException
	 */
	public void sortElements(String parentXPath, String elementName, String sortFields, boolean isAscending) throws XDataException, XDataNotFoundException {
		if (traceName != null)
			logger.debug("XData[" + traceName + "].sort(String xPath, String sortFieldXPath, boolean sortFieldIsInteger)");

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
		XNodes parents = this.getNodes(parentXPath);
		while (parents.next()) {
			Node parentNode = parents.getCurrentNode();
			XNodes children = parents.getNodes(elementName);
			Vector<XDataSortElement> elements = new Vector<XDataSortElement>(); // vector of SortNode
			while (children.next()) {

				Node childNode = children.getCurrentNode();
				Object sortValues[] = new Object[numSortFields];
				for (int i = 0; i < numSortFields; i++) {
					sortValues[i] = children.getText(sortFieldNames[i]);
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
				XDataSortElement element = new XDataSortElement(childNode, sortValues, isAscending);
				elements.addElement(element);
				/*
				 * String stringVal = children.getText(sortFieldXPath); if (sortFieldIsInteger) { long val; try { val = Long.parseLong(stringVal); } catch (NumberFormatException e) { val = Long.MIN_VALUE; } XDataIntegerSortElement element = new XDataIntegerSortElement(childNode, val, isAscending); elements.addElement(element); } else { XDataStringSortElement element = new
				 * XDataStringSortElement(childNode, stringVal, isAscending); elements.addElement(element); } //String name = children.getText("name"); //logger.debug("name = " + name + ", sequence=" + stringVal);
				 */

				// Delete the child from the parent
				parentNode.removeChild(childNode);
				this.notifyDocumentChanged();
			}

			// Sort the list
			Collections.sort(elements);

			// Add the elements back to their parent
			for (int i = 0; i < elements.size(); i++) {
				XDataSortElement rec = (XDataSortElement) elements.elementAt(i);
				Node childNode = rec.getNode();
				// String str = XData.getText(childNode);
				// logger.debug("node is " + str);
				// parentNode.appendChild(childNode);
				createChild((Element) parentNode, childNode);
				this.notifyDocumentChanged();
			}
		}
	}

	public String toString() {
		return "XData: " + hidden.debugTypeStr() + ":\n" + this.getXml();
	}

	/**
	 * Write xml content to an output stream.
	 * 
	 * @param os
	 * @param useUnicode
	 * @throws IOException
	 */
	public void writeToStream(OutputStream os, boolean useUnicode) throws IOException {
		String xml = this.getXml();
		// if (useUnicode)
		// {
		// os.write(BOM1);
		// os.write(BOM2);
		// os.write(BOM3);
		//
		// ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		// OutputStreamWriter writer = new OutputStreamWriter(byteArrayOutputStream, "UTF-16");
		// writer.write(xml);
		// writer.close();
		//
		// byte[] tmp = byteArrayOutputStream.toByteArray();
		// os.write(tmp);
		// }
		// else
		// {
		// OutputStreamWriter writer = new OutputStreamWriter(os);
		// writer.write(xml);
		// writer.close();
		// }
		Writer writer = getOutputWriter(os, useUnicode);
		writer.write(xml);
		writer.close();
	}

	/**
	 * Get the appropriate output writer, for unicode or not. If it's a Unicode file, place the BOM identifierbytes at the start of the file.
	 * 
	 * @param os
	 * @param useUnicode
	 * @return
	 * @throws IOException
	 */
	public static Writer getOutputWriter(OutputStream os, boolean useUnicode) throws IOException {
		if (useUnicode)
			return new OutputStreamWriter(os, "UTF-16");

		// Not unicode - use default encoding
		return new OutputStreamWriter(os);
	}

	/**
	 * Write an XML error message to an output stream. This method is used when a Dinaa operation returns XML containing an error message. The output is converted to HTML, suitable for output by a servlet.
	 * 
	 * @param out
	 *            java.io.PrintWriter
	 */
	public void writeError(PrintWriter out)// throws IOException
	{
		/*
		 * try { HTMLDocumentImpl doc = new HTMLDocumentImpl(); doc.setTitle("Application Error"); HTMLElement head = doc.getHead(); HTMLPreElementImpl h1 = new HTMLPreElementImpl(doc, "H1");
		 * 
		 * 
		 * head.appendChild(h1); Text txt = doc.createTextNode("Internal Error"); h1.appendChild(txt);
		 * 
		 * HTMLElement body = doc.getBody(); HTMLPreElementImpl pre = new HTMLPreElementImpl(doc, "PRE"); body.appendChild(pre); txt = doc.createTextNode(getXml()); pre.appendChild(txt);
		 * 
		 * HTMLSerializer ser = new HTMLSerializer(out, null); ser.serialize(doc);
		 * 
		 * } catch (IOException e) { System.err.println("Error writing error to PrintWriter:"); System.err.println(getXml()); }
		 */
		String s = "" + "<HTML>\n" + "<HEAD><TITLE>Application Error</TITLE></HEAD>\n" + "<BODY BGCOLOR=\"#FFFFFF\">\n" + "<FONT face=\"Arial\">Application Error:</FONT><BR>\n" + "<PRE>" + htmlString(getXml()) + "</PRE>\n" + "</BODY>\n" + "</HTML>\n";
		out.print(s);
	}

	/**
	 * Check that a value for an XML entity contains legal XML characters. If the required value contains non-legal XML characters, it is enclosed in <B>&lt;![CDATA[</B> and <B>]]&gt;</B>.
	 * <P>
	 * 
	 * <B>Example:</B><blockquote> String xml =<BR>
	 * &quot;&lt;stuff&gt;&quot; +<BR>
	 * &quot;&nbsp;&nbsp;&nbsp; &lt;badChars&gt; + XData.xmlDataFmt(&quot;&amp;, &lt; and &gt;&quot;) + &quot;&lt;/badChars&gt;&quot; +<BR>
	 * &quot;&lt;/stuff&gt;&quot;</blockquote>
	 * 
	 * @return A string cointaining only valid XML data.
	 * @param rawValue
	 *            The value to be used as XML data.
	 */
	static public String xmlFmt(char[] arr) {
		if (arr == null)
			return "";
		for (int i = 0; i < arr.length; i++) {
			char ch = arr[i];
			if (ch == '&' || ch == '<' || ch == '>')
				return convertCharsToXML(arr, 100);
		}
		return new String(arr);
	}

	static public String xmlFmt(StringBuffer buf) {
		return xmlFmt(buf.toString());
	}

	/**
	 * This method does not do special conversions other than those done by <code>xmlDataFmt</code>- it is provided for completeness only.
	 * 
	 * @return java.lang.String
	 * @param rawValue
	 *            java.lang.String
	 * @see #xmlDataFmt
	 */
	static public String xmlFmt(char rawValue) {
		switch (rawValue) {
		case '<':
			return "&lt;";
		case '>':
			return "&gt;";
		case '&':
			return "&amp;";
		default:
			return String.valueOf(rawValue);
		}
	}

	/**
	 * This method does not do special conversions other than those done by <code>xmlDataFmt</code>- it is provided for completeness only.
	 * 
	 * @return java.lang.String
	 * @param rawValue
	 *            java.lang.String
	 * @see #xmlDataFmt
	 */
	static public String xmlFmt(double rawValue) {
		return String.valueOf(rawValue);
	}

	/**
	 * This method does not do special conversions other than those done by <code>xmlDataFmt</code>- it is provided for completeness only.
	 * 
	 * @return java.lang.String
	 * @param rawValue
	 *            java.lang.String
	 * @see #xmlDataFmt
	 */
	static public String xmlFmt(float rawValue) {
		return String.valueOf(rawValue);
	}

	/**
	 * This method does not do special conversions other than those done by <code>xmlDataFmt</code>- it is provided for completeness only.
	 * 
	 * @return java.lang.String
	 * @param rawValue
	 *            java.lang.String
	 * @see #xmlDataFmt
	 */
	static public String xmlFmt(int rawValue) {
		return String.valueOf(rawValue);
	}

	/**
	 * This method does not do special conversions other than those done by <code>xmlDataFmt</code>- it is provided for completeness only.
	 * 
	 * @return java.lang.String
	 * @param rawValue
	 *            java.lang.String
	 * @see #xmlDataFmt
	 */
	static public String xmlFmt(long rawValue) {
		return String.valueOf(rawValue);
	}

	/**
	 * This method does not do special conversions other than those done by <code>xmlDataFmt</code>- it is provided for completeness only.
	 * 
	 * @return java.lang.String
	 * @param rawValue
	 *            java.lang.String
	 * @see #xmlDataFmt
	 */
	static public String xmlFmt(Double rawValue) {
		if (rawValue == null)
			return "";
		return rawValue.toString();
	}

	/**
	 * This method does not do special conversions other than those done by <code>xmlDataFmt</code>- it is provided for completeness only.
	 * 
	 * @return java.lang.String
	 * @param rawValue
	 *            java.lang.String
	 * @see #xmlDataFmt
	 */
	static public String xmlFmt(Float rawValue) {
		if (rawValue == null)
			return "";
		return rawValue.toString();
	}

	/**
	 * This method does not do special conversions other than those done by <code>xmlDataFmt</code>- it is provided for completeness only.
	 * 
	 * @return java.lang.String
	 * @param rawValue
	 *            java.lang.String
	 * @see #xmlDataFmt
	 */
	static public String xmlFmt(Integer rawValue) {
		if (rawValue == null)
			return "";
		return rawValue.toString();
	}

	/**
	 * This method does not do special conversions other than those done by <code>xmlDataFmt</code>- it is provided for completeness only.
	 * 
	 * @return java.lang.String
	 * @param rawValue
	 *            java.lang.String
	 * @see #xmlDataFmt
	 */
	static public String xmlFmt(Long rawValue) {
		if (rawValue == null)
			return "";
		return rawValue.toString();
	}

	/**
	 * This method does not do special conversions other than those done by <code>xmlDataFmt</code>- it is provided for completeness only.
	 * 
	 * @return java.lang.String
	 * @param rawValue
	 *            java.lang.String
	 * @see #xmlDataFmt
	 */
	static public String xmlFmt(Short rawValue) {
		if (rawValue == null)
			return "";
		return rawValue.toString();
	}

	/**
	 * Check that a value for an XML entity contains legal XML characters. If the required value contains non-legal XML characters, it is enclosed in <B>&lt;![CDATA[</B> and <B>]]&gt;</B>.
	 * <P>
	 * 
	 * <B>Example:</B><blockquote> String xml =<BR>
	 * &quot;&lt;stuff&gt;&quot; +<BR>
	 * &quot;&nbsp;&nbsp;&nbsp; &lt;badChars&gt; + XData.xmlDataFmt(&quot;&amp;, &lt; and &gt;&quot;) + &quot;&lt;/badChars&gt;&quot; +<BR>
	 * &quot;&lt;/stuff&gt;&quot;</blockquote>
	 * 
	 * @return A string cointaining only valid XML data.
	 * @param rawValue
	 *            The value to be used as XML data.
	 */
	static public String xmlFmt(String rawValue) {
		if (rawValue == null)
			return "";
		// convToZit char[] arr = rawValue.toCharArray();
		// In the next line, it is not a .(dot) character but a unicode character

		if (rawValue.indexOf("<") < 0 && rawValue.indexOf('&') < 0 && rawValue.indexOf('Æ') < 0 && rawValue.indexOf('¸') < 0)
			return rawValue;
		return convertCharsToXML(rawValue.toCharArray(), 100);
	}

	/**
	 * This method does not do special conversions other than those done by <code>xmlDataFmt</code>- it is provided for completeness only.
	 * 
	 * @return java.lang.String
	 * @param rawValue
	 *            java.lang.String
	 * @see #xmlDataFmt
	 */
	static public String xmlFmt(boolean rawValue) {
		return rawValue ? "Y" : "N";
	}

	/**
	 * Return the data previously placed into XML using {@link #binaryDataFmt(byte[])}.
	 * 
	 * @param xpath
	 * @return
	 * @throws XDataException
	 * @throws XDataNotFoundException
	 * @throws IOException
	 */
	public byte[] getBinaryData(String xpath) throws XDataException, XDataNotFoundException {
		try {
			String encoded = getText(xpath);
			BASE64Decoder decoder = new BASE64Decoder();
			byte[] decoded = decoder.decodeBuffer(encoded);
			return decoded;
		} catch (IOException e) {
			XDataException exception = new XDataException("Error extracting binary data: " + e.toString());
			exception.setStackTrace(e.getStackTrace());
			throw exception;
		}
	}

	/**
	 * Encode binary data so it may be stored in XML. The data is encoded using Base64 encoding. The data can be retreived using {@link #getBinaryData(String)}.
	 * 
	 * @param arr
	 * @return
	 */
	static public String binaryDataFmt(byte[] arr) {
		// Encode
		BASE64Encoder encoder = new BASE64Encoder();
		String encoded = encoder.encode(arr);

		StringBuilder buf = new StringBuilder();
		// buf.append("<![CDATA[");
		buf.append(encoded);
		// buf.append("]]>");
		return buf.toString();
	}

	public static final XData loadXDataFromFile(String path, boolean useUnicode, boolean useFastXml) throws XDataException {

		FileInputStream is = null;
		InputStreamReader in = null;
		XData def;
		try {
			is = new FileInputStream(path);
			// if (is == null)
			// throw new XDataException("Could not load '" + path + "'");
			if (useUnicode)
				in = new InputStreamReader(is, "UTF-16");
			else
				in = new InputStreamReader(is);

			// Parse the definition
			def = new XData(in, useFastXml);
			return def;
		} catch (XDataException e) {
			if (useUnicode)
				throw new XDataException("Cannot parse entity definition '" + path + "': perhaps it is not Unicode?: " + e);
			throw new XDataException("Cannot parse entity definition '" + path + "': " + e);
		} catch (FileNotFoundException e) {
			throw new XDataException("Cannot find entity definition '" + path + "': " + e);
		} catch (UnsupportedEncodingException e) {
			throw new XDataException("Error reading entity definition '" + path + "': " + e);
		} finally {
			try {
				if (in != null)
					in.close();
				if (is != null)
					is.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * Convert an array of chars to an XML text string. Converts < to %lt; and & to &amp; Extra is the amount of extra space to allow for growth in the string length. If insufficient space is left for growth, this method will automatically recall itself with twice the value for extra.
	 * 
	 * @return A string cointaining only valid XML data.
	 * @param arr
	 *            An array o fbytes to be converted.
	 * @param extra
	 *            How much to allow for growth.
	 */
	static public String convertCharsToXML(char[] arr, int extra) {
		if (arr == null)
			return "";

		char newArr[] = new char[arr.length + extra];
		int newpos = 0;
		for (int i = 0; i < arr.length; i++) {
			char ch = arr[i];
			switch (ch) {
			case '<':
				newArr[newpos++] = '&';
				newArr[newpos++] = 'l';
				newArr[newpos++] = 't';
				newArr[newpos++] = ';';
				break;
			case '>':
				newArr[newpos++] = '&';
				newArr[newpos++] = 'g';
				newArr[newpos++] = 't';
				newArr[newpos++] = ';';
				break;
			case '&':
				newArr[newpos++] = '&';
				newArr[newpos++] = 'a';
				newArr[newpos++] = 'm';
				newArr[newpos++] = 'p';
				newArr[newpos++] = ';';
				break;
			case 'Æ':
				newArr[newpos++] = '&';
				newArr[newpos++] = '#';
				newArr[newpos++] = '1';
				newArr[newpos++] = '7';
				newArr[newpos++] = '4';
				newArr[newpos++] = ';';
				break;
			// In the next line, it is not a .(dot) character but a unicode character
			case '¸':
				newArr[newpos++] = '&';
				newArr[newpos++] = '#';
				newArr[newpos++] = '2';
				newArr[newpos++] = '5';
				newArr[newpos++] = '2';
				newArr[newpos++] = ';';
				break;
			default:
				newArr[newpos++] = ch;
			}

			int remaining = newArr.length - newpos;
			int togo = arr.length - i;
			if (remaining < 3 || remaining < togo)
				return convertCharsToXML(arr, extra * 2);
		}
		return new String(newArr, 0, newpos);
	}

	/**
	 * Convert an XML string to HTML
	 * 
	 * @return XML input to the module.
	 * @see #enterModule
	 * @see #getJspUrl
	 */
	public static String textareaString(String input) {
		// Convert a String to HTML comparible characters
		char[] arr = input.toCharArray();
		CharArrayWriter os = new CharArrayWriter();
		for (int i = 0; i < arr.length; i++) {
			char ch = arr[i];
			switch (ch) {
			case '<':
				os.write('&');
				os.write('l');
				os.write('t');
				os.write(';');
				break;
			case '>':
				os.write('&');
				os.write('g');
				os.write('t');
				os.write(';');
				break;
			case '&':
				os.write('&');
				os.write('a');
				os.write('m');
				os.write('p');
				os.write(';');
				break;
			case '"':
				os.write('&');
				os.write('q');
				os.write('u');
				os.write('o');
				os.write('t');
				os.write(';');
				break;
			// case '\n': os.write('<');os.write('b');os.write('r');os.write('>');break;
			default:
				os.write(ch);
			}
		}
		return os.toString();
	}

	/****************************
	 * New IXData methods
	 */
	public String string(String xpath) throws XDataException, XDataNotFoundException {
		return getText(xpath);
	}
	
	public void foreach(String xpath, XIteratorCallback callback) throws XDataNotFoundException, XDataException {
		XNodes list = this.getNodes(xpath);
		while (list.next()) {
			callback.next(list);
		}		
	}

	public IXData select(String xpath) throws XDataNotFoundException, XDataException {
		return getNodes(xpath);
	}

	/**
	 * The first and next methods actually do nothing, except return true the first time next() is called.
	 */
	public void first() {
		calledNextAlready = false;
	}

	/**
	 * The first and next methods actually do nothing, except return true the first time next() is called.
	 */
	public boolean next() {
		if (calledNextAlready)
			return false;
		calledNextAlready = true;
		return true;
	}

	public Iterable<IXData> foreach(String xpath) throws XDataNotFoundException, XDataException {
		return getNodes(xpath);
	}
}
