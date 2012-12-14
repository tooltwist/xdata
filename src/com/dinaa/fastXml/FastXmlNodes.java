package com.dinaa.fastXml;

public class FastXmlNodes {
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

	public void first() {
		selectList = this;
		selectPos = -1;
	}

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

	public boolean next() {
		selectPos++;
		if (selectPos >= LIST_SIZE) {
			selectList = selectList.next;
			selectPos = 0;
		}
		if (selectList == null || (selectList == addList && selectPos >= addPos))
			return false;
		return true;
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

	public String getNodeName() {
		if (selectPos < 0 || (selectList == addList && selectPos >= addPos))
			return null;
		return data.getTagName(selectList.nodeNum[selectPos]);
	}

	public String getValue() throws FastXmlException {
		if (selectList == addList && selectPos >= addPos)
			return null;
		return data.getValue(selectList.nodeNum[selectPos], true); // Need to un-escape stuff
	}

	public String getText(String xpath) {
		return getText(xpath, 0);
	}

	public String getText(String xpath, int occurance) {
		if (selectPos < 0 || (selectList == addList && selectPos >= addPos))
			return "";

		int nodeNum = selectList.nodeNum[selectPos];
		return data.getText(nodeNum, xpath, occurance);
	}

	public FastXmlNodes getNodes(String xpath) {
		if (selectPos < 0 || (selectList == addList && selectPos >= addPos))
			return null;

		int nodeNum = selectList.nodeNum[selectPos];
		return data.getNodes(nodeNum, xpath);
	}

	public int getNumNodes() {
		return this.numNodes;
	}
}
