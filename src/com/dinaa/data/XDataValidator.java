package com.dinaa.data;

/**
 * Insert the type's description here. Creation date: (19/06/2001 11:21:59 AM)
 * 
 * @author: Administrator
 */
public interface XDataValidator {
	/**
	 * Insert the method's description here. Creation date: (19/06/2001 11:23:37 AM)
	 * 
	 * @return java.lang.String
	 */
	String getName();

	/**
	 * Insert the method's description here. Creation date: (19/06/2001 11:23:14 AM)
	 * 
	 * @param name
	 *            java.lang.String
	 * @param value
	 *            java.lang.String
	 */
	String validate(String fldName, String newName, String value) throws XDataValidationException;
}
