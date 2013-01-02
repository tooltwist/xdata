package com.tooltwist.xdata;

import java.io.InputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.tooltwist.xdata.X2DataType.PluginStyle;

public class X2Data {

	//-----------------------------------------------------------------------------------------------------
	//	Code related to a the types used in X2Data objects.
	//
	private static boolean defaultsConfigured = false;
	private static Object syncObject = new Object();
	
	/**
	 * A list of data types.
	 * 
	 * A few rules are required for this list, because the data in the X2Data instances is stored in a matching array.
	 * 	- the list must never get re-ordered.
	 *  - types may NOT be removed from this list.
	 *  - types must be added using registerType_internal, either directly or via registerType().
	 */
	private static X2DataType[] types = new X2DataType[6]; // Initial size
	
	/**
	 * The number of registered data types.
	 */
	private static int numTypes = 0;
	
	private static void checkDefaultsLoaded() {
		if (defaultsConfigured)
			return;
		synchronized(syncObject) {
			if (defaultsConfigured)
				return; // belt and braces

			// Register the standard types
			registerType_internal("xml-string", new X2XmlString());
			registerType_internal("xml-dom", new X2DomXml());
			registerType_internal("xml-fast", new X2FastXml());
			registerType_internal("json-string", new X2JsonString());
			registerType_internal("json-fast", new X2FastJson());
			
			defaultsConfigured = true;
		}
	}
	
	public synchronized static void registerType(String type, X2DataType plugin) {
		checkDefaultsLoaded();
		registerType_internal(type, plugin);
	}
	
	private synchronized static void registerType_internal(String type, X2DataType plugin) {
		
		// Make sue this plugin knows it's type
		plugin.setType(type);
		
		// Make sure the type list is big enough to add another.
		if (numTypes + 1 > types.length) {
			int newSize = types.length + 10;
			X2DataType[] newList = new X2DataType[newSize];
			for (int i = 0; i < numTypes; i++) {
				newList[i] = types[i];
			}
			types = newList;
		}
		
		types[numTypes++] = plugin;
	}
	
	private static int typeIndex(String type) {
		for (int i = 0; i < numTypes; i++) {
			if (types[i].getType().equals(type))
				return i;
		}
		return -1;
	}
	
	private static int detectType(String string) {
		for (int i = 0; i < numTypes; i++) {
			if (
				types[i].getDataFormat() == PluginStyle.STRING_REPRESENTATION
				&&
				types[i].stringIsRecognised(string)
			)
				return i;
		}
		return -1;
	}
	
	public static String detectTypeName(String string) {
		int type = detectType(string);
		if (type >= 0)
			return types[type].getType();
		return null;
	}

	//-----------------------------------------------------------------------------------------------------
	//	Code related to a specific X2Data instance.
	
	private Object[] dataForType = null;

	public X2Data(String data) throws X2DataException {
		checkDefaultsLoaded();
		init();
		
		setString(data);
	}
	
	/**
	 * Try to find a document type that understands this object type.
	 * 
	 * @param object
	 * @throws X2DataException
	 */
	public X2Data(Object object) throws X2DataException {
		checkDefaultsLoaded();
		init();
		
		// Find a data type that knows what to do with this object
		int typeIndex = -1;
		for (int i = 0; i < numTypes; i++) {
			if (
				types[i].getDataFormat() == PluginStyle.SELECTABLE_OBJECT
				&&
				types[i].objectIsRecognised(object)
			) {
				typeIndex = i;
				if (types[i].isPreferred())
					break;
			}
		}

		if (typeIndex < 0)
			throw new X2DataException("Could not determine data type");
		
		XSelectable selectable = types[typeIndex].objectToSelectable(this, object);
		dataForType[typeIndex] = selectable;
	}

	public X2Data(InputStream is) throws X2DataException {
		// TODO Auto-generated constructor stub
		//ZZZZZZZZZZZZZZ
	}

	/**
	 * Initialize this specific X2Data object.
	 * (not to be confused with initializing the types above) 
	 */
	private void init() {
		// Create space for each data type
		dataForType = new Object[numTypes];
	}

	public String getString() throws X2DataException {
		if (dataForType == null)
			return null;
		
		// See if we have the data already in a string format.
		for (int i = 0; i < numTypes; i++) {
			if (dataForType[i] == null)
				continue;
			if (types[i].getDataFormat() == PluginStyle.STRING_REPRESENTATION) {
				Object data = dataForType[i];
				return (String) data;
			}
		}
		
		// we'll need to convert to string format
		int typeToConvertToString = -1;
		for (int i = 0; i < numTypes; i++) {
			if (dataForType[i] == null)
				continue;
			if (types[i].getDataFormat() == PluginStyle.STRING_REPRESENTATION)
				break; // shouldn't happen because of the check above

			// Remember this type. We'll convert the last type found, unless we come across a preferred type.
			typeToConvertToString = i;
			if (types[i].isPreferred())
				break;
		}
		
		// Convert the object to a string
		if (typeToConvertToString >= 0) {
			// Find the string version of this type
			String type = types[typeToConvertToString].getPrimaryType();
			int stringType = typeIndex(type + "-string");
			
			// Convert from the object to the string, and remember the string version.
			Object object = dataForType[typeToConvertToString];
			String string = types[typeToConvertToString].selectableToString(object);
			dataForType[stringType] = string;
			return string;
		}
		throw new X2DataException("Could not convert " + types[typeToConvertToString].getType() + " to a String");
	}
	
	public void setString(String string) throws X2DataException {
		
		// Determine the data type
		int typeIndex = detectType(string);
		if (typeIndex < 0)
			throw new X2DataException("Could not determine data type");
		
		// Clear out all other data, and set this data
		for (int i = 0; i < numTypes && i < dataForType.length; i++) {
			if (i == typeIndex)
				dataForType[i] = string;
			else
				dataForType[i] = null;
		}
	}

	public XSelectable getSelector() throws X2DataException {
		
		// See if we already have data in a selectable format.
		for (int i = 0; i < numTypes && i < dataForType.length; i++) {
			X2DataType type = types[i];
			Object object = dataForType[i];

			if (object == null)
				continue;
			if (type.getDataFormat() == PluginStyle.SELECTABLE_OBJECT) {
				// Have data, and it's a selectable
				return (XSelectable) object;
			}
		}
		
		// We don't have a selectable object already, so we need to create one from any string data we have.
		for (int i = 0; i < numTypes && i < dataForType.length; i++) {
			X2DataType type = types[i];
			Object object = dataForType[i];

			if (object == null)
				continue;
			if (type.getDataFormat() == PluginStyle.STRING_REPRESENTATION) {
				// Yeah - we have data as a string.
				int indexForStringData = i;
				
				// Let's see if we can convert this to a selectable format.
				String prefix = type.getPrimaryType() + "-";
				int indexForSelectableData = -1;
				for (int i2 = 0; i2 < dataForType.length; i2++) {
					X2DataType type2 = types[i2];
					
					// If this type selectable, and can it convert from the type of string we have?
					// i.e. does it have the same primary type (json, xml, etc)
					if (
						type2.getDataFormat() == PluginStyle.SELECTABLE_OBJECT
						&&
						type2.getType().startsWith(prefix)
					) {
						indexForSelectableData = i2;
						if (type2.isPreferred())
							break; // look no further
					}
				}
				
				if (indexForSelectableData >= 0) {
					String dataInStringRepresentation = (String) dataForType[indexForStringData];
					Object convertedObject = types[indexForSelectableData].stringToSelectable(this, dataInStringRepresentation);
					dataForType[indexForSelectableData] = convertedObject;
					return (XSelectable) convertedObject;
				}
				
				// We couldn't convert this type of string... try the next.
				
			}
		}
		
		// No conversion could be found
		throw new X2DataUnknownConversionException("Could not convert to a selectable format");//ZZZZZ check message
	}

	public XSelectable getSelector(String type) throws X2DataException {
		
		// Check the requested type is actually selectable
		int objectTypeIndex = typeIndex(type);
		if (objectTypeIndex < 0)
			throw new X2DataIncompatibleFormatException("Unknown format '" + type + "'.");
		if (types[objectTypeIndex].getDataFormat() != PluginStyle.SELECTABLE_OBJECT)
			throw new X2DataIncompatibleFormatException("Format '" + type + "' is not a selectable format."); //ZZZZZ check message
		
		// Do we already have the data in the required format?
		if (dataForType[objectTypeIndex] != null)
			return (XSelectable) dataForType[objectTypeIndex];
		
		// We'll need to convert from the string version.
		// Do we have the requested data type in it's string representation?
		X2DataType objectType = types[objectTypeIndex];		
		String stringTypeName = types[objectTypeIndex].getPrimaryType() + "-string";
		int stringTypeIndex = typeIndex(stringTypeName);
		if (dataForType[stringTypeIndex] != null) {
			
			// We have a string version of the data, so do a conversion to required selectable form.
			String dataInStringRepresentation = (String) dataForType[stringTypeIndex];
			XSelectable object = objectType.stringToSelectable(this, dataInStringRepresentation);
			dataForType[objectTypeIndex] = object;
			return object;			
		}
		
		
		// We couldn't convert to this type of object from it's string format,
		// so we'll need to convert from some other data format that we do have.
		//ZZZZZ
		throw new X2DataUnknownConversionException("Could not convert to '" + type + "' format");
	}

	/**
	 * Remove the definitions for all data formats except the one specified.
	 * 
	 * This is called if the contents of a specific data type has been changed by direct
	 * communication with it's selector.
	 * 
	 * Note: this method is normally only ever called by a selector.
	 * 
	 * @param type
	 * @throws X2DataIncompatibleFormatException 
	 */
	public void invalidateAllSelectorsExcept(Object object) throws X2DataIncompatibleFormatException {
		for (int i = 0; i < numTypes; i++) {
			Object stringOrObject = dataForType[i];
			if (stringOrObject != object)
				dataForType[i] = null;
		}
	}
}
