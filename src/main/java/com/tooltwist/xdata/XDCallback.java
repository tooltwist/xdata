package com.tooltwist.xdata;

public interface XDCallback {
	
	public void next(XSelector item, int index, Object userDefinedObject) throws XDException;

}
