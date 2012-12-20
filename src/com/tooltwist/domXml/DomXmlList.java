package com.tooltwist.domXml;

import java.util.Iterator;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.tooltwist.xdata.X2DataException;
import com.tooltwist.xdata.X2DataIterator;
import com.tooltwist.xdata.XIteratorCallback;
import com.tooltwist.xdata.XSelectable;

public class DomXmlList implements XSelectable, Iterable<XSelectable> {

	private NodeList list;
	private DomXml xmlData;	// only used to get the XPath values
	private int index = -1;	// Current index in this linked list

	public DomXmlList(NodeList nl, DomXml domXml) {
		super();

		this.list = nl;
		this.xmlData = domXml;
	}
	
	private Node getCurrentNode() {
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
	public DomXmlList getNodes(String xpath) throws X2DataException {
		Node node = getCurrentNode();
		if (node == null)
			return null;
		return xmlData.getNodes(xpath, node);
	}

	public Iterator<XSelectable> iterator() {
		return new X2DataIterator(this);
	}

	public String string(String xpath) throws X2DataException {
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

	public int size() {
		return (list == null) ? 0 : list.getLength();
	}

	public XSelectable select(String xpath) throws X2DataException {
		Node node = getCurrentNode();
		if (node == null)
			return null;
		return xmlData.getNodes(xpath, node);
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

	public void foreach(String xpath, Object userData, XIteratorCallback callback) throws X2DataException {
		DomXmlList list = this.getNodes(xpath);
		for (int i = 0; list.next(); i++) {
			callback.next(list, i, userData);
		}		
	}

	public void foreach(String xpath, XIteratorCallback callback) throws X2DataException {
		foreach(xpath, null, callback);
	}

	public Iterable<XSelectable> foreach(String xpath) throws X2DataException {
		return getNodes(xpath);
	}

}
