package com.tooltwist.xdata;

public class X2XmlString extends X2DataType {

	@Override
	public PluginStyle getDataFormat() {
		return PluginStyle.STRING_REPRESENTATION;
	}

	//-------------------------------------------------------------------------------------------------------
	//
	// Methods used for a STRING_REPRESENTATION.
	//

	/**
	 * Recognize a string as XML if it starts with '<'.
	 */
	@Override
	public boolean stringIsRecognised(String string) {
		int length = string.length();
		for (int i = 0; i < length; i++) {
			
			// Skip whitespace
			char c = string.charAt(i);
			if (c == ' ' || c == '\t' || c == '\n' || c == '\r')
				continue;
			
			// We have a character. If it's <, then we can consider this to be XML.
			if (c == '<')
				return true;
			
			// Don't know what this is.
			return false;
		}
		
		// data is empty or all whitespace.
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
	public XSelectable objectToSelectable(X2Data parentXd, Object object) throws X2DataException {
		return null;
	}
	
	@Override
	public XSelectable stringToSelectable(X2Data parentXd, String string) {
		// Never called
		return null;
	}

	@Override
	public String selectableToString(Object object) {
		// Never called
		return null;
	}
}
