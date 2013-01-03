package com.tooltwist.domXml;

import org.w3c.dom.Node;

/**
 * Insert the type's description here.
 * Creation date: (12/02/2002 12:49:24 PM)
 * @author: Administrator
 */
class DomXmlSortElement implements Comparable<DomXmlSortElement>
{
	protected Node node;
	protected Object[] values;
	protected boolean isAscending=true;

/**
 * XDataSortElement constructor comment.
 */
protected DomXmlSortElement(Node node, Object[] values, boolean isAscending)
{
	super();
	this.node = node;
	this.values = values;
	this.isAscending = isAscending;
}

/**
 * XDataSortElement constructor comment.
 */
public int compareTo(DomXmlSortElement other)
{
	int result;
	for (int i = 0; ; i++)
	{
		if (i>=values.length)
			return 0;
		if (values[i] instanceof Long)
		{
			Long val1 = (Long) values[i];
			Long val2 = (Long) other.values[i];
			result = val1.compareTo(val2);
		}
		else
		{
			String val1 = (String) values[i];
			String val2 = (String) other.values[i];
			result = val1.compareTo(val2);
		}
		if (result != 0)
			break;
	}
	
	// reverse result for descending order
	if (!isAscending && result != 0)
		result *= -1;

	return result;
}

protected Node getNode()
{
	return node;
}
}
