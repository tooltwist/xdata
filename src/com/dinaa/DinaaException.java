package com.dinaa;

/**
 * Incorrect parameter passed to a servlet. This class is used to report exceptions in the DinaaModule convenience methods for accessing servlet parameters.
 * 
 * @see DinaaModule#getRequestInteger
 * 
 * @author: Philip Callender
 */
public class DinaaException extends Exception {
	private static final long serialVersionUID = 7530467081392389306L;
	private int errorCode = 0;

	public DinaaException() {
		super();
	}

	public DinaaException(String s) {
		super(s);
	}

	public DinaaException(Exception ex)
	{
		super(ex.toString());
		this.setStackTrace(ex.getStackTrace());
	}
	
	public DinaaException(int code, String s) {
		super(s);
		errorCode = code;
	}

	public int getErrorCode() {
		return errorCode;
	}
}
