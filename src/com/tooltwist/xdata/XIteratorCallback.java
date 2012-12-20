package com.tooltwist.xdata;

public interface XIteratorCallback {
	
	public void next(XSelectable item, int index, Object userDefinedObject) throws X2DataException;

}
