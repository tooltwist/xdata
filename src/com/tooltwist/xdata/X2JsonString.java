package com.tooltwist.xdata;

public class X2JsonString extends X2DataType {

	@Override
	public PluginStyle getDataFormat() {
		return PluginStyle.STRING_REPRESENTATION;
	}

	//-------------------------------------------------------------------------------------------------------
	//
	// Methods used for a STRING_REPRESENTATION.
	//

	/**
	 * Recognize a string as JSON if it starts with '{' or '['.
	 */
	@Override
	public boolean stringIsRecognised(String string) {
		int length = string.length();
		for (int i = 0; i < length; i++) {
			
			// Skip whitespace
			char c = string.charAt(i);
			if (c == ' ' || c == '\t' || c == '\n' || c == '\r')
				continue;
			
			// We have a character. If it's { or [, then we can consider this to be JSON.
			if (c == '{' || c == '[')
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
	public XSelectable stringToObject(String string) {
		// Never called
		return null;
	}

	@Override
	public String objectToString(Object object) {
		// Never called
		return null;
	}

}
