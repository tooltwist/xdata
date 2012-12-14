package com.dinaa.data;

//import org.apache.log4j.Category;
//import com.dinaa.*;

/**
 * Exception handler.
 * 
 * @author: Philip Callender
 */
public class XDataNotFoundException extends XDataException {
	private static final long serialVersionUID = -2520815576404683004L;
	// static Category catLog = Category.getInstance(XDataException.class);
	private java.lang.String errorOutput;

	/**
	 * DinaaOpException constructor.
	 */
	public XDataNotFoundException() {
		super();
	}

	public XDataNotFoundException(String s) {
		super(s);
	}

	public XDataNotFoundException(String s, String errorOut) {
		super(s);
		this.errorOutput = errorOut;
	}

	public String getErrorOutput() {
		return errorOutput;
	}
}
