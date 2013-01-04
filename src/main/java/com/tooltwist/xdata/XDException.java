package com.tooltwist.xdata;

public class XDException extends Exception {
	private static final long serialVersionUID = -2546614232592554711L;

	public XDException(String string) {
		super(string);
	}

	public XDException(Exception e) {
		super(e);
	}

}
