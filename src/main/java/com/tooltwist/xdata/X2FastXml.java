package com.tooltwist.xdata;

import com.tooltwist.fastXml.FastXml;

public class X2FastXml extends XSelectorPlugin {

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
	public XSelector stringToSelectable(XD parentXd, String string) throws XDException {
		FastXml fastXml;
		try {
			fastXml = new FastXml(string);
		} catch (Exception e) {
			XDException exception = new XDException(e.getMessage());
			exception.setStackTrace(e.getStackTrace());
			throw exception;
		}
		return fastXml;
	}

	@Override
	public XSelector objectToSelectable(XD parentXd, Object object) throws XDException {
		return null;
	}

	@Override
	public String selectableToString(Object object) throws XDException {
		if (object instanceof FastXml) {
			FastXml fastXml = (FastXml) object;
			String string = fastXml.toString();
			return string;
		}
		throw new XDException("Internal error: object is not " + FastXml.class.getName());
	}

}
