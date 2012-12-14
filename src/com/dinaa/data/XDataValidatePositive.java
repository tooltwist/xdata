package com.dinaa.data;

/**
 * Insert the type's description here. Creation date: (19/06/2001 11:26:48 AM)
 * 
 * @author: Administrator
 */
public class XDataValidatePositive implements XDataValidator {
	/**
	 * FieldValidateInteger constructor comment.
	 */
	public XDataValidatePositive() {
		super();
	}

	/**
	 * Insert the method's description here. Creation date: (19/06/2001 11:26:48 AM)
	 * 
	 * @return java.lang.String
	 */
	public String getName() {
		return "positive";
	}

	/**
	 * Insert the method's description here. Creation date: (19/06/2001 11:26:48 AM)
	 * 
	 * @param name
	 *            java.lang.String
	 * @param value
	 *            java.lang.String
	 */
	public String validate(String fldName, String newName, String value) throws XDataValidationException {
		String trimmed = value.trim();
		char[] arr = trimmed.toCharArray();
		for (int i = 0; i < arr.length; i++)
			if (!Character.isDigit(arr[i]) && arr[i] != '.')
				throw new XDataValidationException(fldName, "Expected Value >= 0 for " + newName);
		return trimmed;
	}
}
