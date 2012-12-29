package com.tooltwist.xdata;

import com.tooltwist.fastJson.FastJson;

public class X2FastJson extends X2DataType {

	@Override
	public PluginStyle getDataFormat() {
		return PluginStyle.SELECTABLE_OBJECT;
	}

	//-------------------------------------------------------------------------------------------------------
	//
	// Methods used for a STRING_REPRESENTATION.
	//

	@Override
	public boolean stringIsRecognised(String string) {
		// Never called
		return false;
	}

	//-------------------------------------------------------------------------------------------------------
	//
	// Methods used for a SELECTABLE_OBJECT.
	//

	@Override
	public boolean objectIsRecognised(Object data) {
		return false;
	}
	
	@Override
	public XSelectable stringToObject(String string) throws X2DataException {
		FastJson fastJson;
		try {
			fastJson = new FastJson(string);
		} catch (Exception e) {
			X2DataException exception = new X2DataException(e.getMessage());
			exception.setStackTrace(e.getStackTrace());
			throw exception;
		}
		return fastJson;
	}

	@Override
	public String objectToString(Object object) throws X2DataException {
		if (object instanceof FastJson) {
			FastJson fastXml = (FastJson) object;
			String string = fastXml.toString();
			return string;
		}
		throw new X2DataException("Internal error: object is not " + FastJson.class.getName());
	}

}
