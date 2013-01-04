package com.tooltwist.xdata;

/**
 * Definition of a data type for use by XData.
 * 
 * @author philipcallender
 *
 */
public abstract class XSelectorPlugin {
	
	public enum PluginStyle { STRING_REPRESENTATION, SELECTABLE_OBJECT };

	/**
	 * The type of data (e.g. xml-fastxml)
	 */
	private String type;
	
	/**
	 * If set, this is the preferred format for selecting this type.
	 */
	private boolean preferred;

	
	public final void setType(String type) {
		this.type = type;
	}
	
	public final String getType() {
		return this.type;
	}
	
	public final String getPrimaryType() {
		int pos = type.indexOf('-');
		if (pos <= 0)
			return type;
		String primaryType = type.substring(0, pos);
		return primaryType;
	}

	public final void setPreferred(boolean preferred) {
		this.preferred = preferred;
	}

	public final boolean isPreferred() {
		return this.preferred;
	}

	public abstract PluginStyle getDataFormat();
	
	//-------------------------------------------------------------------------------------------------------
	//
	// Methods used if getDataFormat() returns STRING_REPRESENTATION.
	//
	
	/**
	 * @param data
	 * 	is some sort of data in it's string representation (JSON, XML, CSV, etc).
	 * @return
	 * 	true if the data contains the type of data represented by this plugin.
	 */
	public abstract boolean stringIsRecognised(String data);

	/**
	 * Convert from string representation to it's data type form.
	 * 
	 * This will only be called if getDataFormat() == STRING_REPRESENTATION and stringIsRecognised(Object data) is true.
	 * 
	 * @param dataInStringRepresentation
	 * @return
	 * @throws XDException
	 */
	public abstract XSelector stringToSelectable(XD parentXd, String dataInStringRepresentation) throws XDException;



	//-------------------------------------------------------------------------------------------------------
	//
	// Methods used if getDataFormat() returns SELECTABLE_OBJECT.
	//

	/**
	 * If an XD is constructed using an object, this method is called for each non-string data
	 * type, to see if this data type can be created. For example, when a XML-DOM type is created
	 * from a Node object. 
	 * 
	 * @param data
	 * @return
	 */
	public abstract boolean objectIsRecognised(Object data);

	/**
	 * Convert from an object to it's data type form.
	 * 
	 * This will only be called if getDataFormat() == SELECTABLE_OBJECT and objectIsRecognised(Object data) is true.
	 * 
	 * @param dataInStringRepresentation
	 * @return
	 * @throws XDException
	 */
	public abstract XSelector objectToSelectable(XD parentXd, Object object) throws XDException;

	/**
	 * Convert an object of the type represented by this dataType into it's string representation.
	 *  
	 * @param object
	 * @return
	 * @throws XDException
	 */
	public abstract String selectableToString(Object object) throws XDException;

}
