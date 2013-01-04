package com.tooltwist.xdata;

import com.tooltwist.fastJson.FastJson;

public class X2FastJson extends XSelectorPlugin {

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
	public XSelector objectToSelectable(XD parentXd, Object object) throws XDException {
		return null;
	}
	
	@Override
	public XSelector stringToSelectable(XD parentXd, String string) throws XDException {
		FastJson fastJson;
		try {
			fastJson = new FastJson(string);
		} catch (Exception e) {
			XDException exception = new XDException(e.getMessage());
			exception.setStackTrace(e.getStackTrace());
			throw exception;
		}
		return fastJson;
	}

	@Override
	public String selectableToString(Object object) throws XDException {
		if (object instanceof FastJson) {
			FastJson fastXml = (FastJson) object;
			String string = fastXml.toString();
			return string;
		}
		throw new XDException("Internal error: object is not " + FastJson.class.getName());
	}

}
