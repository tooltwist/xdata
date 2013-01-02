package com.tooltwist.xdata;

import com.tooltwist.fastXml.FastXml;

public class X2FastXml extends X2DataType {

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
	public XSelectable stringToSelectable(X2Data parentXd, String string) throws X2DataException {
		FastXml fastXml;
		try {
			fastXml = new FastXml(string);
		} catch (Exception e) {
			X2DataException exception = new X2DataException(e.getMessage());
			exception.setStackTrace(e.getStackTrace());
			throw exception;
		}
		return fastXml;
	}

	@Override
	public XSelectable objectToSelectable(X2Data parentXd, Object object) throws X2DataException {
		return null;
	}

	@Override
	public String selectableToString(Object object) throws X2DataException {
		if (object instanceof FastXml) {
			FastXml fastXml = (FastXml) object;
			String string = fastXml.toString();
			return string;
		}
		throw new X2DataException("Internal error: object is not " + FastXml.class.getName());
	}

}
