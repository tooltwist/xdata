package com.dinaa.data;

/**
 * Incorrect parameter passed to a servlet. This class is used to report exceptions in the DinaaModule convenience methods for accessing servlet parameters.
 * 
 * @see DinaaModule#getRequestInteger
 * 
 * @author: Philip Callender
 */
public class InvalidParameterException extends XDataException {
	private static final long serialVersionUID = -2786035099202601127L;

	public InvalidParameterException() {
		super();
	}

	public InvalidParameterException(String s) {
		super(s);
	}

	public InvalidParameterException(String s, String errorOut) {
		super(s, errorOut);
	}

}
