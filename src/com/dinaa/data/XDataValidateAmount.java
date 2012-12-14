package com.dinaa.data;

/**
 * Insert the type's description here. Creation date: (19/06/2001 11:26:48 AM)
 * 
 * @author: Administrator
 */
public class XDataValidateAmount implements XDataValidator {
	/**
	 * FieldValidateInteger constructor comment.
	 */
	public XDataValidateAmount() {
		super();
	}

	/**
	 * Insert the method's description here. Creation date: (19/06/2001 11:26:48 AM)
	 * 
	 * @return java.lang.String
	 */
	public String getName() {
		return "integer";
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
		// convToZit int cntDigitsAfterDot = -1;
		for (int i = 0; i < arr.length; i++) {
			if (!Character.isDigit(arr[i]) && arr[i] != '-' && arr[i] != '.')
				throw new XDataValidationException(fldName, "Expected Numeric Value for " + newName);
		}

		// Round to 2 decimal places
		double num = Double.parseDouble(value);
		long lnum = (long) ((num * 100) + 0.4999);
		num = ((double) lnum) / 100;

		return String.valueOf(num);
	}
}
