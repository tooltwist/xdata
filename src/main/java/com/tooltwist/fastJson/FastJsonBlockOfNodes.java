package com.tooltwist.fastJson;

import com.tooltwist.fastJson.FastJsonBlockOfNodes;

class FastJsonBlockOfNodes
{
	protected static final int SIZE = 4096;
	private FastJsonBlockOfNodes next = null;
	private int firstNodeNum;

	protected int type[] = new int[SIZE];
	protected int indent[] = new int[SIZE];
	protected int line[] = new int[SIZE];
	protected int nextNodeAtThisLevel[] = new int[SIZE];

	// Positions in the JSON string
	protected int offsetOfName[] = new int[SIZE];
	protected int offsetOfValue[] = new int[SIZE];
	protected int offsetOfValueEnd[] = new int[SIZE];

	
	
	FastJsonBlockOfNodes(int firstNodeNum)
	{
		this.firstNodeNum = firstNodeNum;
	}
	
	FastJsonBlockOfNodes getBlock(int nodeNum, boolean create)
	{
		if (nodeNum < 0)
			return null;
		// Find the right array
		FastJsonBlockOfNodes block = this;
		while (nodeNum >= SIZE)
		{
			if (block.next == null)
			{
				if ( !create)
					return null;
				// Extend the list of arrays
				block.next = new FastJsonBlockOfNodes(this.firstNodeNum + SIZE);
			}
			// Look in the next list
			block = block.next;
			nodeNum -= SIZE;
		}
		return block;
	}
	
	void displayNode(char[] json, int index)
	{
		if (index >= SIZE)
		{
			next.displayNode(json, index - SIZE);
			return;
		}
		for (int i=0; i < indent[index]; i++)
			System.out.print("  ");
		int nameOffset = this.offsetOfName[index];
		int valueOffset = this.offsetOfValue[index];
		int valueEndOffset = this.offsetOfValueEnd[index];
		
		// Get the name
		String name = "" + nameOffset;
		if (nameOffset < 0)
			name = "[null]";
		else {
			// Work out the name
			for (int i = nameOffset; i < json.length; i++)
				if (json[i] == '\"') {
					name = new String(json, nameOffset, i - nameOffset);
					break;
				}
		}
		
		// Get a description of the value.
		String type = "?";
		String value = "";
		switch (this.type[index]) {
		case FastJson.TYPE_OBJECT:
			type = "OBJECT";
			break;
		case FastJson.TYPE_ARRAY:
			type = "ARRAY";
			break;
		case FastJson.TYPE_TRUE:
			type = "TRUE";
			value = new String(json, valueOffset, valueEndOffset-valueOffset);
			break;
		case FastJson.TYPE_FALSE:
			type = "FALSE";
			value = new String(json, valueOffset, valueEndOffset-valueOffset);
			break;
		case FastJson.TYPE_NULL:
			type = "NULL";
			value = new String(json, valueOffset, valueEndOffset-valueOffset);
			break;
		case FastJson.TYPE_STRING:
			type = "STRING";
			value = new String(json, valueOffset, valueEndOffset-valueOffset);
			break;
		case FastJson.TYPE_NUMBER:
			type = "NUMBER";
			value = new String(json, valueOffset, valueEndOffset-valueOffset);
			break;
		}
		
		int nodeNum = firstNodeNum + index;
//		System.out.println(name + "     node "+nodeNum+": line "+line[index]+": "+startTag[index]+","+afterStartTag[index]+","+endTag[index]+","+afterEndTag[index]+"  next="+nextNodeAtThisLevel[index]);
		System.out.print(nodeNum + ": " + name + " [" + type +"]: line="+line[index]+", ");
		if (nextNodeAtThisLevel[index] >= 0) {
			System.out.print("next="+nextNodeAtThisLevel[index] + ", ");
		}
		System.out.print(value);
		System.out.println();
	}
	
	void debugDump(char[] json, int upTo)
	{
		for (int cnt = 0; cnt < upTo; cnt++)
		{
			if (cnt >= SIZE)
			{
				next.debugDump(json, upTo - SIZE);
				return;
			}
			displayNode(json, cnt);
		}
	}
}
