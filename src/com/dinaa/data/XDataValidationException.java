package com.dinaa.data;

/**
 * Incorrect parameter passed to a servlet. This class is used to report exceptions in the DinaaModule convenience methods for accessing servlet parameters.
 * 
 * @see DinaaModule#getRequestInteger
 * 
 * @author: Philip Callender
 */
public class XDataValidationException extends XDataException {
	private static final long serialVersionUID = 6347679568807620779L;

	public XDataValidationException() {
		super();
	}

	public XDataValidationException(String s) {
		super(s);
	}

	public XDataValidationException(String s, String errorOut) {
		super(s, errorOut);
	}
}
