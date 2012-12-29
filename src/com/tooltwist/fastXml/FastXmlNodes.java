package com.tooltwist.fastXml;

import java.util.Iterator;

import com.tooltwist.xdata.X2DataException;
import com.tooltwist.xdata.X2DataIterator;
import com.tooltwist.xdata.XIteratorCallback;
import com.tooltwist.xdata.XSelectable;

public class FastXmlNodes implements XSelectable, Iterable<XSelectable> {
	private static final int LIST_SIZE = 1024;

	private int[] nodeNum = new int[LIST_SIZE];
	private FastXml data;

	private FastXmlNodes next = null;

	// The following fields are only used in the first of the linked list
	private FastXmlNodes addList = this; // last in the list
	private int addPos = 0;
	private FastXmlNodes selectList = this; // current in the list
	private int selectPos = -1;
	private int numNodes = 0;

	protected FastXmlNodes(FastXml data) {
		this.data = data;
		this.addList = this;
		this.addPos = 0;
	}

	protected void addNode(int nodeNum) {
		// If the last list is full, add another record on the end of the linked list
		if (addPos >= LIST_SIZE) {
			// Add a new list onto the end
			FastXmlNodes newList = new FastXmlNodes(data);
			addList.next = newList;
			addList = newList;
			addPos = 0;
		}
		addList.nodeNum[addPos++] = nodeNum;
		numNodes++;
	}

	public boolean setPos(int pos) {
		if (pos < 0 || pos >= numNodes)
			return false;
		selectList = this;
		for (; pos >= LIST_SIZE; pos -= LIST_SIZE)
			selectList = selectList.next;
		selectPos = pos - 1;
		return true;
	}

	protected int getNodeNum() {
		if (selectPos < 0 || (selectList == addList && selectPos >= addPos))
			return -1;
		return selectList.nodeNum[selectPos];
	}
	
	public String getValue() throws FastXmlException {
		if (selectList == addList && selectPos >= addPos)
			return null;
		return data.getValue(selectList.nodeNum[selectPos], true); // Need to un-escape stuff
	}

	//ZZZZZZ Remove some of this legacy stuff.
	public String getText(String xpath) {
		String value = getText(xpath, 0);
		return value;
	}

	public String getText(String xpath, int occurance) {
		// If next() hasn't been called yet, use the fist node.
		if (selectPos < 0) {
			if (numNodes < 1)
				return "";
			int nodeNum = selectList.nodeNum[0];
			return data.getText(nodeNum, xpath, occurance);
		}
		
		// Check we haven't run off the end
		if (selectList == addList && selectPos >= addPos)
			return null;

		// Select from the current node.
		int nodeNum = selectList.nodeNum[selectPos];
		return data.getText(nodeNum, xpath, occurance);
	}

	private FastXmlNodes getNodes(String xpath) {
		// If next() hasn't been called yet, default to the first node.
		if (selectPos < 0) {
			if (numNodes < 1)
				return null;
			int nodeNum = selectList.nodeNum[0];
			return data.getNodes(nodeNum, xpath);
		}

		// Check we haven't run off the end
		if (selectList == addList && selectPos >= addPos)
			return null;

		// Select from the current node.
		int nodeNum = selectList.nodeNum[selectPos];
		return data.getNodes(nodeNum, xpath);
	}

//	public int getNumNodes() {
//		return this.numNodes;
//	}

	
	//--------------------------------------------------------------------------------------------------------------------
	// Methods for accessing data.

	@Override
	public String getString(String xpath) throws X2DataException {
		try {
			String value = getText(xpath, 0);
//			String value = getText(xpath);
			return value;
		} catch (Exception e) {
			X2DataException exception = new X2DataException(e.getMessage());
			exception.setStackTrace(e.getStackTrace());
			throw exception;
		}
	}


	//--------------------------------------------------------------------------------------------------------------------
	// Return the number of records that can be iterated over.

	@Override
	public int size() {
		return this.numNodes;
	}

	
	//--------------------------------------------------------------------------------------------------------------------
	// Iterate over this object using first, next.
	
	@Override
	public void first() {
		selectList = this;
		selectPos = -1;
	}

	@Override
	public boolean hasNext() {
		int tmpSelectPos = selectPos + 1;
		FastXmlNodes tmpSelectList = selectList;
		if (tmpSelectPos >= LIST_SIZE) {
			// Overflow onto the next block
			tmpSelectList = tmpSelectList.next;
			tmpSelectPos = 0;
		}
		if (tmpSelectList == null || (tmpSelectList == addList && tmpSelectPos >= addPos))
			return false;
		return true;
	}
	
	@Override
	public boolean next() {

		// If we've run off the end previously, don't go any further.
		if (selectList == null || (selectList == addList && selectPos >= addPos))
			return false;
		
		// Check the next position in the list.
		// Move on to the next node of the linked list if necessary.
		selectPos++;
		if (selectPos >= LIST_SIZE) {
			selectList = selectList.next;
			selectPos = 0;
		}

		// If we've run off the end now, return false.
		if (selectList == null || (selectList == addList && selectPos >= addPos))
			return false;

		// We haven't run off the end.
		return true;
	}

	@Override
	public int currentIndex() {
		if (selectPos < 0 || (selectList == addList && selectPos >= addPos))
			return -1;
		return selectPos;
	}

	@Override
	public String currentName()
	{
		if (selectPos < 0 || (selectList == addList && selectPos >= addPos))
			return null;
		return data.getNameOfNode(selectList.nodeNum[selectPos]);
	}

	
	//--------------------------------------------------------------------------------------------------------------------
	// Iterate over this object using a Java iterator
	
	public Iterator<XSelectable> iterator() {
		return new X2DataIterator(this);
	}

	
	//--------------------------------------------------------------------------------------------------------------------
	// Select elements within this data object

	public XSelectable select(String xpath) {
		return getNodes(xpath);
	}

	
	//--------------------------------------------------------------------------------------------------------------------
	// Select and iterate using a callback

	public void foreach(String xpath, XIteratorCallback callback) throws X2DataException {
		foreach(xpath, callback, null);
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

	
	//--------------------------------------------------------------------------------------------------------------------
	// Select and iterate using a Java iterator
	
	public Iterable<XSelectable> foreach(String xpath) throws X2DataException {
		FastXmlNodes list = this.getNodes(xpath);
		return list;
	}

}
