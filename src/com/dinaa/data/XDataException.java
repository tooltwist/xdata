package com.dinaa.data;

//import org.apache.log4j.Category;
import com.dinaa.DinaaException;

/**
 * Exception handler.
 * 
 * @author: Philip Callender
 */
public class XDataException extends DinaaException {
	private static final long serialVersionUID = 905581710234824718L;

	// static Category catLog = Category.getInstance(XDataException.class);
	private java.lang.String errorOutput;

	/**
	 * DinaaOpException constructor.
	 */
	public XDataException() {
		super();
	}

	public XDataException(String s) {
		super(s);
	}

	public XDataException(String s, String errorOut) {
		super(s);
		this.errorOutput = errorOut;
	}

	public String getErrorOutput() {
		return errorOutput;
	}
}
