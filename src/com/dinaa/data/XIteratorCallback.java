package com.dinaa.data;

public interface XIteratorCallback {
	
	public void next(IXData item) throws XDataNotFoundException, XDataException;

}
