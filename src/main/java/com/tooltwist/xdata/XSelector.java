package com.tooltwist.xdata;

import java.util.Iterator;

/**
 * A class that implements this interface can be used to access individual data elements, using the XData API.
 * 
 *   This interface automatically implies implementation of Iterable<XDSelector> and Iterator<XDSelector>,
 *   meaning that the object can be used in standard Java iterations.
 *   
 *   Note that the definition of next() provided here is different to that in {@link XData#next()}.
 * 
 * @author philipcallender
 *
 */
public interface XSelector {


	//--------------------------------------------------------------------------------------------------------------------
	// Methods for treating the current object as a list.

	/**
	 * Iterate through the selection.
	 * @return
	 */
	public Iterator<XSelector> iterator();
	
	/**
	 * Return the number of records in this selection.
	 */
	public int size();
	
	/**
	 * Re-start from the first element in the list.
	 */
	public void first();
	
	/**
	 * @return True if there is another element in the selection. 
	 */
	public boolean hasNext();
	
	/**
	 * Advance the internal pointer to the next element in this selection.
	 * @returnTrue if there is another element in the selection. 
	 */
	public boolean next();

	/**
	 * Return the name of the field currently selected. For example, if you used select("/* /customer") to get
	 * this XDSelector object, then the field name will be "customer". 
	 * 
	 * @return
	 */
	public String currentName();
	
	/**
	 * Return the index of the field currently selected. While iterating through a selection, this
	 * counts through the items in the selection, starting at zero. 
	 * 
	 * @return
	 */
	public int currentIndex();
	

	/**
	 * Set the position in the current list.
	 * 
	 * Returns false if the index is invalid.
	 * 
	 * @param index
	 * @throws XDException
	 */
	public boolean setCurrentIndex(int index) throws XDException;



	//--------------------------------------------------------------------------------------------------------------------
	// Methods for accessing data

	/**
	 * Get a field value as a String.
	 * 
	 * @param xpath
	 * @return
	 * @throws XDataException
	 * @throws XDataNotFoundException
	 */
	public String getString(String xpath) throws XDException;
	
	/**
	 * Get a field value as a String.
	 * 
	 * If the value is null, return defaultValue. 
	 * 
	 * @param xpath
	 * @return
	 * @throws XDataException
	 * @throws XDataNotFoundException
	 */
	public String getString(String xpath, String defaultValue) throws XDException;


	/**
	 * Get a field value containing an integer.
	 *
	 * @param xpath
	 * @return
	 * @throws XDException
	 * If the field does not contain a valid integer.
	 */
	public int getInteger(String xpath) throws XDNumberFormatException, XDException;

	/**
	 * Get a field value containing an integer.
	 * 
	 * If the field's value is null, return defaultValue. 
	 *
	 * @param xpath
	 * @return
	 * @throws XDException
	 * If the field does not contain a valid integer.
	 */
	int getInteger(String xpath, int defaultValue) throws XDNumberFormatException, XDException;

	/**
	 * Get a field value containing a boolean value.
	 * <br>
	 * The first character of the value is checked, case insensitively:<br>
	 * <ul>
	 * <li>true if  '1', 't', or 'y' (1, true, yes).</li>
	 * </ul>
	 * 
	 * Any other value is returned as false. 
	 *
	 * @param xpath
	 * @return
	 * @throws XDException 
	 */
	public boolean getBoolean(String xpath) throws XDException;

	/**
	 * Get a field value containing a boolean value.
	 * <br>
	 * The first character of the value is checked, case insensitively:<br>
	 * <ul>
	 * <li>false if  '0', 'f', or 'n' (0, false, no).</li>
	 * <li>true if  '1', 't', or 'y' (1, true, yes).</li>
	 * </ul>
	 * 
	 * If the field's value is null, return defaultValue. 
	 *
	 * @param xpath
	 * @return
	 * @throws XDException 
	 */
	public boolean getBoolean(String xpath, boolean defaultValue) throws XDException;
	
	
	//--------------------------------------------------------------------------------------------------------------------
	// Methods for selecting a list from within the current element.

	
	/**
	 * Select a list.
	 * 
	 * For example:
	 * <pre>
	 * XData data = ...;
	 * XDSelector list = data.select("/* /product");
	 * while (list.next()) {
	 * 	String description = list.string("description");
	 * }
	 * </pre>
	 * 
	 * @param xpath
	 * @return
	 * @throws XDataNotFoundException
	 * @throws XDataException
	 */
	public XSelector select(String xpath) throws XDException;

	/**
	 * Iterate through matching items using a callback object to handle each item.
	 * For example:
	 * <pre>
	 * XData data = ...;
	 * MyData myData = ...;
	 * data.select("/* /product", myData, new XDCallback() {
	 *   {@code @Override }
	 *   public void next(IXData item, int index, Object data) throws XDataNotFoundException, XDataException {
	 *     MyData myData = (MyData) data;
	 * 	   String description = item.string("description");
	 *   }
	 * });
	 * </pre>
	 * @param xpath
	 * @param callback
	 * @throws XDataNotFoundException
	 * @throws XDataException
	 */
	public void foreach(String xpath, Object userData, XDCallback callback) throws XDException;

	/**
	 * Iterate through matching items using a callback object to handle each item.
	 * For example:
	 * <pre>
	 * XData data = ...;
	 * Object myData = ...;
	 * data.select("/* /product", myData, new XDCallback() {
	 *   {@code @Override }
	 *   public void next(IXData item, int index, Object data) throws XDataNotFoundException, XDataException {
	 * 	   String description = item.string("description");
	 *   }
	 * });
	 * </pre>
	 * @param xpath
	 * @param callback
	 * @throws XDataNotFoundException
	 * @throws XDataException
	 */
	public void foreach(String xpath, XDCallback callback) throws XDException;

	/**
	 * Iterate through items using Iterable notation.
	 * For example:
	 * <pre>
	 * XData data = ...;
	 * for (IXData item : data.foreach("/* /product")) {
	 *	String description = item.string("description");
	 * }
	 * </pre>
	 * 
	 * @param xpath
	 * @return
	 * @throws XDataException
	 */
	public Iterable<XSelector> foreach(String xpath) throws XDException;

}
