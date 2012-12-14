package com.dinaa.fastXml;

class FastXmlBlockOfNodes {
	protected static final int SIZE = 1024;
	private FastXmlBlockOfNodes next = null;
	private int firstNodeNum;

	protected int type[] = new int[SIZE];
	protected int indent[] = new int[SIZE];
	protected String name[] = new String[SIZE]; // Name is left null for comments and special tags
	protected int line[] = new int[SIZE];
	protected int nextNodeAtThisLevel[] = new int[SIZE];

	// Positions in the XML string
	protected int startTag[] = new int[SIZE];
	protected int afterStartTag[] = new int[SIZE];
	protected int endTag[] = new int[SIZE];
	protected int afterEndTag[] = new int[SIZE];

	FastXmlBlockOfNodes(int firstNodeNum) {
		this.firstNodeNum = firstNodeNum;
	}

	FastXmlBlockOfNodes getBlock(int nodeNum, boolean create) {
		if (nodeNum < 0)
			return null;
		// Find the right array
		FastXmlBlockOfNodes block = this;
		while (nodeNum >= SIZE) {
			if (block.next == null) {
				if (!create)
					return null;
				// Extend the list of arrays
				block.next = new FastXmlBlockOfNodes(this.firstNodeNum + SIZE);
			}
			// Look in the next list
			block = block.next;
			nodeNum -= SIZE;
		}
		return block;
	}

	void displayNode(int index) {
		if (index >= SIZE) {
			next.displayNode(index - SIZE);
			return;
		}
		for (int i = 0; i < indent[index]; i++)
			System.out.print("  ");
		String name = this.name[index];
		if (name == null)
			name = "[null]";
		int nodeNum = firstNodeNum + index;
		System.out.println(name + "     node " + nodeNum + ": line " + line[index] + ": " + startTag[index] + "," + afterStartTag[index] + "," + endTag[index] + "," + afterEndTag[index] + "  next=" + nextNodeAtThisLevel[index]);
	}

	void list(int upTo) {
		for (int cnt = 0; cnt < upTo; cnt++) {
			if (cnt >= SIZE) {
				next.list(upTo - SIZE);
				return;
			}
			displayNode(cnt);
		}
	}
}
