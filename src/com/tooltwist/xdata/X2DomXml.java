package com.tooltwist.xdata;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.tooltwist.domXml.DomXml;

public class X2DomXml extends X2DataType {

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
		if (data instanceof Document) {
			return true;
		} else if (data instanceof Node) {
			return true;
		}
		return false;
	}
	
	@Override
	public XSelectable stringToObject(String string) throws X2DataException {
		DomXml fastXml;
		try {
			fastXml = new DomXml(string);
		} catch (Exception e) {
			X2DataException exception = new X2DataException(e.getMessage());
			exception.setStackTrace(e.getStackTrace());
			throw exception;
		}
		return fastXml;
	}

	@Override
	public String objectToString(Object object) throws X2DataException {
		if (object instanceof DomXml) {
			DomXml fastXml = (DomXml) object;
			String string = fastXml.toString();
			return string;
		}
		throw new X2DataException("Internal error: object is not " + DomXml.class.getName());
	}

}
