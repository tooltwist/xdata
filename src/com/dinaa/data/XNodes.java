package com.dinaa.data;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.dinaa.fastXml.FastXmlNodes;
import com.dinaa.misc.AltLang;

/**
 * The class represents a list of node in an XML document, along with methods to access those nodes. This class also acts as an enumerator to access the nodes, one at a time, using the <code>first</code> and <code>next</code> methods.
 * 
 * @author: Philip Callender
 */
public class XNodes implements IXData, Iterable<IXData> {
	static Logger logger = Logger.getLogger(XNodes.class);

	// Two data formats - NodeList (from DOM) or FastXmlNodes (from FastXml)
	private boolean useFastXml = false;
	private FastXmlNodes fastXmlNodes = null;
	private NodeList list = null;

	private XData xmlData; // only used for to get the XPath values
	private int index = -1;
	private String fieldXpath = new String();

	public XNodes(FastXmlNodes nodes, XData xmlData) {
		super();

		this.fastXmlNodes = nodes;
		this.useFastXml = true;
		this.xmlData = xmlData;
	}

	public XNodes(NodeList list, XData xmlData) {
		super();

		this.list = list;
		this.useFastXml = false;
		this.xmlData = xmlData;
	}

	/**
	 * Move the current position to just before the first node. Note that the <I>next</I> method must be used before the node can be used.
	 */
	public void first() {
		if (useFastXml) {
			fastXmlNodes.first();
		} else {
			index = -1;
		}
	}

	/**
	 * Get a boolean value from the current node, using the provided xpath. This is used primarily to set checkbox values. This method also sets the XPath property of this bean.
	 * 
	 * @return The text value of the node that matches the given XPath.
	 * @param xpath
	 *            An XPath specifier.
	 */
	public boolean getCheckBox(String xpath) throws XDataException {
		String val;
		if (useFastXml) {
			fieldXpath = xpath;
			val = fastXmlNodes.getText(xpath);
		} else {
			Node current = getCurrentNode();
			fieldXpath = xpath;
			if (current == null || xpath == null || xpath.equals(""))
				return false;
			val = xmlData.getText(xpath, current);
		}
		return (val != null && (val.equals("true") || val.equals("Y")));
	}

	public String getCurrentTagName() throws XDataException {
		if (useFastXml) {
			return fastXmlNodes.getNodeName();
		} else {
			return getCurrentNode().getNodeName();
		}
	}

	/**
	 * Return the current node in the list.
	 * 
	 * @return org.w3c.dom.Node
	 * @throws XDataException
	 */
	@Deprecated
	public Node getCurrentNode() throws XDataException {
		if (useFastXml)
			throw new XDataException("XNodes.getCurrentNode() called for an XData without DOM - call XData.useDOM() first.");
		if (index >= 0 && index < list.getLength())
			return list.item(index);
		return null;
	}

	/**
	 * Gets the xpath property set by the most recent <code>getText</code> or <code>setXpath</code> method call.
	 * 
	 * @return The xpath property value.
	 * @see #setXpath
	 */
	public String getCurrentXpath() {
		return fieldXpath;
	}

	/**
	 * Return the index'th node in the list.
	 * 
	 * @return org.w3c.dom.Node
	 * @param index
	 *            The position of the required node (starting at zero).
	 * @throws XDataException
	 */
	@Deprecated
	public Node getNode(int index) throws XDataException {
		if (useFastXml)
			throw new XDataException("XNodes.getNode() called for an XData without DOM - call XData.getDocument() first.");
		if (index >= 0 && index < list.getLength())
			return list.item(index);
		return null;
	}

	/**
	 * Get a node that matches a XPath, starting at the current node in this list. This method returns the index'th node that matches the specified XPath, starting at the current node in the list.
	 * 
	 * @return The list of nodes that match the XPath.
	 * @param xpath
	 *            An XPath specifier.
	 * @param index
	 *            The index of the node to return (startinf at zero).
	 * @see #getXmlNodeList
	 */
	@Deprecated
	public Node getNode(String xpath, int index) throws XDataException {
		if (useFastXml)
			throw new XDataException("XNodes.getNode() called for an XData without DOM - call XData.getDocument() first.");
		Node node = getCurrentNode();
		if (node == null)
			return null;

		return xmlData.getNode(xpath, node, index);
	}

	/**
	 * Get a list of nodes that match a XPath, starting at the current node in this list.
	 * 
	 * @return The list of nodes that match the XPath.
	 * @param xpath
	 *            An XPath specifier.
	 */
	public XNodes getNodes(String xpath) throws XDataException {
		if (useFastXml) {
			FastXmlNodes nodes = fastXmlNodes.getNodes(xpath);
			return new XNodes(nodes, this.xmlData);
		} else {
			Node node = getCurrentNode();
			if (node == null)
				return null;
			return xmlData.getNodes(xpath, node);
		}
	}

	/**
	 * Return the number of nodes in the list..
	 * 
	 * @return The number of nodes.
	 */
	public int getNumNodes() {
		if (useFastXml)
			return fastXmlNodes.getNumNodes();
		else
			return (list == null) ? 0 : list.getLength();
	}

	/**
	 * Return the position in the list of the current node.
	 * 
	 * @return The current position.
	 */
	public int getPos() {
		/* Perform the getPos method. */
		return index;
	}

	/**
	 * Gets the text value of the current Node.
	 * 
	 * @return The text value.
	 * @throws XDataException
	 */
	public String getText() throws XDataException {
		return getText(fieldXpath);
	}

	/**
	 * Get a text value from the current node, using the provided xpath. This method also sets the XPath property of this bean.
	 * 
	 * @return The text value of the node that matches the given XPath.
	 * @param useAlternateLanguage
	 *            boolean flag that indicates if alternate language is on.
	 * @param xpath
	 *            An XPath specifier.
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

	/**
	 * Get a text value from the current node, using the provided xpath. This method also sets the XPath property of this bean.
	 * 
	 * @return The text value of the node that matches the given XPath.
	 * @param xpath
	 *            An XPath specifier.
	 * @throws XDataException
	 */
	public String getText(String xpath)// throws XDataException
	{
		if (useFastXml) {
			fieldXpath = xpath;
			return fastXmlNodes.getText(xpath);
		} else {
			try {
				Node current = getCurrentNode();
				fieldXpath = xpath;
				if (current == null || xpath == null || xpath.equals(""))
					return "";
				return xmlData.getText(xpath, current);
			} catch (XDataException e) {
				// Strange - the XData should have already been parsed.
				logger.error("Internal Error in XNodes.getText(String): " + e, e);
				return "";
			}
		}
	}

	/**
	 * Get a text value from the current node, using the provided xpath. If the value exceeds <code>maxLength</code> the value is truncated. This method also sets the XPath property of this bean.
	 * 
	 * @return The text value of the node that matches the given XPath.
	 * @param xpath
	 *            An XPath specifier.
	 * @throws XDataException
	 */
	public String getText(String name, int maxLength) throws XDataException// throws XDataException
	{
		String value = getText(name);
		if (value.length() > maxLength)
			value = value.substring(0, maxLength);
		return value;
	}

	/**
	 * Create a new row for a table in a Java Server Page (JSP). This method is used when a JSP wants to display information from this list of nodes as columns in a table. This method can be called for each node as the JSP code works through the list. When the specified number of columns have been output, this method will start a new row in the table, by writing %lt;/TR%gt; and %lt;TR%gt; to the
	 * JSPs output.
	 * 
	 * @param pageContext
	 *            The pagecontext variable in the JSP.
	 * @param colsPerRow
	 *            The number of columns in each row of the table.
	 */
	public void jspCheckRow(PageContext pageContext, int colsPerRow) throws IOException {
		if (index != 0 && (index % colsPerRow) == 0) {
			JspWriter jw = pageContext.getOut();
			jw.println("    </TR>");
			jw.println("    <TR>");
		}
	}

	/**
	 * Return true if there are more nodes in this list, after making the next node the current node.
	 * 
	 * @return True, unless the list is already empty, or the last node in the list is already current.
	 */
	public boolean hasNext() {
		if (useFastXml) {
			return fastXmlNodes.hasNext();
		} else {
			int length = 0;
			if (list != null) {
				length = list.getLength();
			}
			int nextIndex = index + 1;
			return (nextIndex  >= 0 && nextIndex < length);
		}
	}

	/**
	 * Return true if there are more nodes in this list, after making the next node the current node.
	 * 
	 * @return True, unless the list is already empty, or the last node in the list is already current.
	 */
	public boolean next() {
		if (useFastXml) {
			return fastXmlNodes.next();
		} else {
			index++;
			int length = 0;
			if (list != null) {
				length = list.getLength();
			}
			return (index >= 0 && index < length);
		}
	}

	/**
	 * Get a node for the next column in a JSP table. This method is a combination of the <code>next</code> and <code>checkJspRow</code> methods. It returns true if there are more nodes in this list. Before doing so, it makes the next node the current node, and outputs a new row to the current table, by writing %lt;/TR%gt; and %lt;TR%gt; to the JSP's output.
	 * 
	 * @return True, unless the list is already empty, or the last node in the list is already current.
	 */
	public boolean nextColumn(PageContext pageContext, int columnsPerRow) throws IOException {
		index++;
		boolean haveNode = (index >= 0 && index < list.getLength());
		if (haveNode)
			jspCheckRow(pageContext, columnsPerRow);
		return haveNode;
	}

	/**
	 * Return the current node in the list.
	 * 
	 * @return org.w3c.dom.Node
	 * @throws XDataException
	 */
	public String getCurrentNodeTag() throws XDataException {
		if (useFastXml)
			return fastXmlNodes.getNodeName();
		else {
			if (index >= 0 && index < list.getLength()) {
				Node node = list.item(index);
				return node.getNodeName();
			}
			return null;
		}
	}

	/**
	 * Return the current node in the list.
	 * 
	 * @return org.w3c.dom.Node
	 * @throws XDataException
	 */
	public String getCurrentNodeValue() throws XDataException {
		if (useFastXml) {
			return fastXmlNodes.getValue();
		} else {
			if (index >= 0 && index < list.getLength()) {
				Node node = list.item(index);
				String value = XData.getText(node);
				return value;
			}
			return null;
		}
	}

	/**
	 * Set the current node in the list.
	 * 
	 * @return False, if in invalid node index was passed.
	 */
	public boolean setPos(int nodeIndex) {
		if (useFastXml) {
			return fastXmlNodes.setPos(nodeIndex);
		} else {
			if (nodeIndex < 0 || nodeIndex >= list.getLength())
				return false;
			index = nodeIndex;
			return true;
		}
	}

	public String toString() {
		String s = "XNodes(";
		s += useFastXml ? "fastXML" : "DOM";
		s += ", " + getNumNodes() + " nodes)";
		return s;
	}

	/****************************
	 * New IXData methods
	 */
	public void foreach(String xpath, XIteratorCallback callback) throws XDataNotFoundException, XDataException {
		XNodes list = this.getNodes(xpath);
		while (list.next()) {
			callback.next(list);
		}		
	}

	public IXData select(String xpath) throws XDataNotFoundException, XDataException {
		return getNodes(xpath);
	}

	public String string(String xpath) throws XDataException, XDataNotFoundException {
		return getText(xpath);
	}

	public Iterable<IXData> foreach(String xpath) throws XDataException {
		return getNodes(xpath);
	}

	public Iterator<IXData> iterator() {
		return new XNodesIterator(this);
	}

}
