package com.tooltwist.domXml;

import java.util.Iterator;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.tooltwist.xdata.XDException;
import com.tooltwist.xdata.XDNumberFormatException;
import com.tooltwist.xdata.XIterator;
import com.tooltwist.xdata.XDCallback;
import com.tooltwist.xdata.XSelector;

public class DomXmlList implements XSelector, Iterable<XSelector> {

	private NodeList list;
	private DomXml xmlData;	// only used to get the XPath values
	private int index = -1;	// Current index in this linked list

	//--------------------------------------------------------------------------------------------------------------------
	// Methods for accessing data.
	

	public String getString(String xpath) throws XDException {
		Node current = null;
		if (index < 0) {
			// Before calling next() - use the first record.
			if (list.getLength() > 0)
				current = list.item(0);
		} else {
			// Use the currently selected record
			current = getCurrentNode();
		}
//			fieldXpath = xpath;
		if (current == null)
			return null;
		if (xpath == null || xpath.equals(""))
			return "";
		return xmlData.getText(xpath, current);
	}

	@Override
	public String getString(String xpath, String defaultValue) throws XDException {
		String string = getString(xpath);
		if (string.equals(""))
			return defaultValue;
		return string;
	}

	@Override
	public int getInteger(String xpath) throws XDNumberFormatException, XDException {
		String string = getString(xpath).trim();
		try {
			int val = Integer.parseInt(string);
			return val;
		} catch (NumberFormatException e) {
			throw new XDNumberFormatException("Expected an Integer (" + string + ")");
		}
	}

	@Override
	public int getInteger(String xpath, int defaultValue) throws XDNumberFormatException, XDException {
		String string = getString(xpath).trim();
		if (string.equals(""))
			return defaultValue;
		try {
			int val = Integer.parseInt(string);
			return val;
		} catch (NumberFormatException e) {
			throw new XDNumberFormatException("Expected an Integer (" + string + ")");
		}
	}


	//--------------------------------------------------------------------------------------------------------------------
	// Iterate over this object using first, next.

	public int size() {
		return (list == null) ? 0 : list.getLength();
	}

	public void first() {
		index = -1;
	}

	public boolean hasNext() {
		int length = 0;
		if (list != null) {
			length = list.getLength();
		}
		int nextIndex = index  + 1;
		return (nextIndex  >= 0 && nextIndex < length);
	}

	public boolean next() {
		index++;
		int length = 0;
		if (list != null) {
			length = list.getLength();
		}
		return (index >= 0 && index < length);
	}

	@Override
	public int currentIndex() {
		if (index >= 0 && index < list.getLength())
			return index;
		return -1;
	}

	@Override
	public boolean setCurrentIndex(int index) throws XDException {
		if (index >= 0 && index < list.getLength()) {
			this.index = index;
			return true;
		}
		return false;
	}
	
	@Override
	public String currentName() {
		if (index >= 0 && index < list.getLength())
			return list.item(index).getNodeName();
		return null;
	}

	
	//--------------------------------------------------------------------------------------------------------------------
	// Iterate over this object using a Java iterator

	public Iterator<XSelector> iterator() {
		return new XIterator(this);
	}
	
	
	//--------------------------------------------------------------------------------------------------------------------
	// Select elements within this data object

	public XSelector select(String xpath) throws XDException {
		Node node = getCurrentNode();
		if (node == null)
			return null;
		return xmlData.getNodes(xpath, node);
	}

	
	//--------------------------------------------------------------------------------------------------------------------
	// Select and iterate using a callback

	public void foreach(String xpath, Object userData, XDCallback callback) throws XDException {
		DomXmlList list = this.getNodes(xpath);
		for (int i = 0; list.next(); i++) {
			callback.next(list, i, userData);
		}		
	}

	public void foreach(String xpath, XDCallback callback) throws XDException {
		foreach(xpath, null, callback);
	}

	
	//--------------------------------------------------------------------------------------------------------------------
	// Select and iterate using a Java iterator
	
	public Iterable<XSelector> foreach(String xpath) throws XDException {
		return getNodes(xpath);
	}
	

	/*--------------------------------------------------------------------------------------------------------------------
	 *
	 *											DOM Specific code below here.
	 *
	 */

	protected DomXmlList(NodeList nl, DomXml domXml) {
		super();
		this.list = nl;
		this.xmlData = domXml;
	}
	
	public Node getCurrentNode() {
		if (index >= 0 && index < list.getLength())
			return list.item(index);
		return null;
	}


	/**
	 * Get a list of nodes that match a XPath, starting at the current node in this list.
	 * 
	 * @return The list of nodes that match the XPath.
	 * @param xpath
	 *            An XPath specifier.
	 */
	public DomXmlList getNodes(String xpath) throws XDException {
		Node node = getCurrentNode();
		if (node == null)
			return null;
		return xmlData.getNodes(xpath, node);
	}

	/**
	 * Return the index'th node in the list.
	 * 
	 * @return org.w3c.dom.Node
	 * @param index
	 *            The position of the required node (starting at zero).
	 * @throws XDataException
	 */
	public Node getNode(int index) {
		if (index >= 0 && index < list.getLength())
			return list.item(index);
		return null;
	}


	public Node getNode(String xpath, int index2) throws XDException {
		Node node = getCurrentNode();
		if (node == null)
			return null;
		return xmlData.getNode(xpath, node, index);
	}


}
