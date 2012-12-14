package com.dinaa.data;

import java.util.Iterator;

public interface IXData {
	
	// Get field values
	public String string(String xpath) throws XDataException, XDataNotFoundException;
	
	// Select a list
	public IXData select(String xpath) throws XDataNotFoundException, XDataException;
	public void first();
	public boolean next();

	/**
	 * Iterate through matching items using an object to handle each item.
	 * 
	 * @param xpath
	 * @param callback
	 * @throws XDataNotFoundException
	 * @throws XDataException
	 */
	public void foreach(String xpath, XIteratorCallback callback) throws XDataNotFoundException, XDataException;
	
	public Iterable<IXData> foreach(String xpath) throws XDataException;
	
	// Old stuff
	@Deprecated
	public String getText(String xpath) throws XDataException, XDataNotFoundException;// throws XDataException
	@Deprecated
	public String getText(String name, int maxLength) throws XDataException;// throws XDataException

}
