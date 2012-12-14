package com.dinaa.data;

import java.util.Iterator;

public class XIterable implements Iterable<IXData> // Iterator<IXData>
{
	private Iterator<IXData> iterator;
	
	public XIterable(Iterator<IXData> iterator) {
		this.iterator = iterator;
	}

	public Iterator<IXData> iterator() {
		return iterator;
	}

}
