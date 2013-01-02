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
	
	@Override
	public XSelectable stringToSelectable(X2Data parent, String string) throws X2DataException {
		DomXml fastXml;
		try {
			fastXml = new DomXml(parent, string);
		} catch (Exception e) {
			X2DataException exception = new X2DataException(e.getMessage());
			exception.setStackTrace(e.getStackTrace());
			throw exception;
		}
		return fastXml;
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
	public XSelectable objectToSelectable(X2Data xd, Object object) throws X2DataException {
		try {
			if (object instanceof Document) {
				DomXml fastXml = new DomXml(xd, (Document)object);
				return fastXml;
			} else if (object instanceof Node) {
				DomXml fastXml = new DomXml(xd, (Node)object);
				return fastXml;
			} else {
				// Should not happen, because objectIsRecognised() has already checked.
				return null;
			}
			
		} catch (Exception e) {
			X2DataException exception = new X2DataException(e.getMessage());
			exception.setStackTrace(e.getStackTrace());
			throw exception;
		}
	}

	@Override
	public String selectableToString(Object object) throws X2DataException {
		if (object instanceof DomXml) {
			DomXml domXml = (DomXml) object;
			String string = domXml.getXml();
			return string;
		}
		throw new X2DataException("Internal error: object is not " + DomXml.class.getName());
	}

}
