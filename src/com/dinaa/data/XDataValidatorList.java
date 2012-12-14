package com.dinaa.data;

import java.util.Hashtable;

/**
 * Insert the type's description here. Creation date: (19/06/2001 12:25:27 PM)
 * 
 * @author: Administrator
 */
public class XDataValidatorList extends Hashtable<Object, XDataValidator> implements XDataValidator {
	private static final long serialVersionUID = -3449052996852578640L;

	/**
	 * XDataValidatorList constructor comment.
	 */
	public XDataValidatorList() {
		super();
	}

	/**
	 * XDataValidatorList constructor comment.
	 * 
	 * @param initialCapacity
	 *            int
	 */
	public XDataValidatorList(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * XDataValidatorList constructor comment.
	 * 
	 * @param initialCapacity
	 *            int
	 * @param loadFactor
	 *            float
	 */
	public XDataValidatorList(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * XDataValidatorList constructor comment.
	 * 
	 * @param t
	 *            java.util.Map
	 */
	public XDataValidatorList(java.util.Map<Object, XDataValidator> t) {
		super(t);
	}

	/**
	 * Insert the method's description here. Creation date: (19/06/2001 12:25:27 PM)
	 * 
	 * @return java.lang.String
	 */
	public String getName() {
		return null;
	}

	/**
	 * Insert the method's description here. Creation date: (19/06/2001 12:25:27 PM)
	 * 
	 * @param name
	 *            java.lang.String
	 * @param value
	 *            java.lang.String
	 */
	public String validate(String fldName, String newName, String value) throws XDataValidationException {
		return null;
	}
}
