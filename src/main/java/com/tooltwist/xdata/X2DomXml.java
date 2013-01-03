package com.tooltwist.xdata;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.tooltwist.domXml.DomXml;

public class X2DomXml extends XDSelectorType {

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
	public XDSelector stringToSelectable(XD parent, String string) throws XDException {
		DomXml fastXml;
		try {
			fastXml = new DomXml(parent, string);
		} catch (Exception e) {
			XDException exception = new XDException(e.getMessage());
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
	public XDSelector objectToSelectable(XD xd, Object object) throws XDException {
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
			XDException exception = new XDException(e.getMessage());
			exception.setStackTrace(e.getStackTrace());
			throw exception;
		}
	}

	@Override
	public String selectableToString(Object object) throws XDException {
		if (object instanceof DomXml) {
			DomXml domXml = (DomXml) object;
			String string = domXml.getXml();
			return string;
		}
		throw new XDException("Internal error: object is not " + DomXml.class.getName());
	}

}
