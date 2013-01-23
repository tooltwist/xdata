package com.tooltwist.xdata;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import com.tooltwist.xdata.XSelectorPlugin.PluginStyle;

/**
 * This class provides a data-independent way of accessing data.
 * 
 * Data may be loaded into the class in various formats, but then accessed with a single consistent API
 * as provided by the {@link XSelector} interface.
 * 
 * The XSelector interface provides iteration over a list of data items. In the case of this
 * class there is only a single data hierarchy, which is treated as a list of one record
 * (the top of the hierarchy). The XSelector methods can then be used to select data or
 * further lists beneath this hierarchy.
 *
 * @see {@link https://github.com/tooltwist/xdata}
 *   
 * @author philipcallender
 */
public class XD implements XSelector {

	//-----------------------------------------------------------------------------------------------------
	//	Code related to a the types used in XD objects.
	//
	private static boolean defaultsConfigured = false;
	private static Object syncObject = new Object();
	
	/**
	 * A list of data types.
	 * 
	 * A few rules are required for this list, because the data in the XD instances is stored in a matching array.
	 * 	- the list must never get re-ordered.
	 *  - types may NOT be removed from this list.
	 *  - types must be added using registerType_internal, either directly or via registerType().
	 */
	private static XSelectorPlugin[] types = new XSelectorPlugin[6]; // Initial size
	
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
	
	public synchronized static void registerType(String type, XSelectorPlugin plugin) {
		checkDefaultsLoaded();
		registerType_internal(type, plugin);
	}
	
	private synchronized static void registerType_internal(String type, XSelectorPlugin plugin) {
		
		// Make sue this plugin knows it's type
		plugin.setType(type);
		
		// Make sure the type list is big enough to add another.
		if (numTypes + 1 > types.length) {
			int newSize = types.length + 10;
			XSelectorPlugin[] newList = new XSelectorPlugin[newSize];
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
	
	/**
	 * Try to recognize the data type of a String.
	 * This method will call ({@link XSelectorPlugin#stringIsRecognised(String)} for each
	 * string handling plugin, until one is found that recognizes the string. 
	 * 
	 * @param string
	 * @return
	 * A string identifying the type of data in <code>string</code>.
	 */
	public static String recogniseType(String string) {
		int type = detectType(string);
		if (type >= 0)
			return types[type].getType();
		return null;
	}

	//-----------------------------------------------------------------------------------------------------
	//	Code related to a specific XD instance.
	
	private Object[] dataForType = null;

	public XD(String data) throws XDException {
		checkDefaultsLoaded();
		init();
		
		if (data.trim().equals(""))
			data = "<empty/>";

		setString(data);
	}
	
	/**
	 * Try to find a document type that understands this object type.
	 * 
	 * @param object
	 * @throws XDException
	 */
	public XD(Object object) throws XDException {
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
			throw new XDException("Could not determine data type");
		
		XSelector selectable = types[typeIndex].objectToSelectable(this, object);
		dataForType[typeIndex] = selectable;
	}

	public XD(InputStream is) throws XDException {
		try {
			String data = loadStringFromReader(is);
			checkDefaultsLoaded();
			init();
			
			if (data.trim().equals(""))
				data = "<empty/>";
	
			setString(data);
		} catch (IOException e) {
			throw new XDException(e);
		}
	}

	/**
	 * Initialize this specific XD object.
	 * (not to be confused with initializing the types above) 
	 */
	private void init() {
		// Create space for each data type
		dataForType = new Object[numTypes];
	}

	public String getString() throws XDException {
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
		throw new XDException("Could not convert " + types[typeToConvertToString].getType() + " to a String");
	}
	
	public void setString(String string) throws XDException {
		
		// Determine the data type
		int typeIndex = detectType(string);
		if (typeIndex < 0)
			throw new XDException("Could not determine data type");
		
		// Clear out all other data, and set this data
		for (int i = 0; i < numTypes && i < dataForType.length; i++) {
			if (i == typeIndex)
				dataForType[i] = string;
			else
				dataForType[i] = null;
		}
	}

	public XSelector getSelector() throws XDException {
		
		// See if we already have data in a selectable format.
		for (int i = 0; i < numTypes && i < dataForType.length; i++) {
			XSelectorPlugin type = types[i];
			Object object = dataForType[i];

			if (object == null)
				continue;
			if (type.getDataFormat() == PluginStyle.SELECTABLE_OBJECT) {
				// Have data, and it's a selectable
				return (XSelector) object;
			}
		}
		
		// We don't have a selectable object already, so we need to create one from any string data we have.
		for (int i = 0; i < numTypes && i < dataForType.length; i++) {
			XSelectorPlugin type = types[i];
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
					XSelectorPlugin type2 = types[i2];
					
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
					return (XSelector) convertedObject;
				}
				
				// We couldn't convert this type of string... try the next.
				
			}
		}
		
		// No conversion could be found
		throw new XDUnknownConversionException("Could not convert to a selectable format");//ZZZZZ check message
	}

	public XSelector getSelector(String type) throws XDException {
		
		// Check the requested type is actually selectable
		int objectTypeIndex = typeIndex(type);
		if (objectTypeIndex < 0)
			throw new XDIncompatibleFormatException("Unknown format '" + type + "'.");
		if (types[objectTypeIndex].getDataFormat() != PluginStyle.SELECTABLE_OBJECT)
			throw new XDIncompatibleFormatException("Format '" + type + "' is not a selectable format."); //ZZZZZ check message
		
		// Do we already have the data in the required format?
		if (dataForType[objectTypeIndex] != null)
			return (XSelector) dataForType[objectTypeIndex];
		
		// We'll need to convert from the string version.
		// Do we have the requested data type in it's string representation?
		XSelectorPlugin objectType = types[objectTypeIndex];		
		String stringTypeName = types[objectTypeIndex].getPrimaryType() + "-string";
		int stringTypeIndex = typeIndex(stringTypeName);
		if (dataForType[stringTypeIndex] != null) {
			
			// We have a string version of the data, so do a conversion to required selectable form.
			String dataInStringRepresentation = (String) dataForType[stringTypeIndex];
			XSelector object = objectType.stringToSelectable(this, dataInStringRepresentation);
			dataForType[objectTypeIndex] = object;
			return object;			
		}
		
		
		// We couldn't convert to this type of object from it's string format,
		// so we'll need to convert from some other data format that we do have.
		//ZZZZZ
		throw new XDUnknownConversionException("Could not convert to '" + type + "' format");
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
	 * @throws XDIncompatibleFormatException 
	 */
	public void invalidateAllSelectorsExcept(Object object) throws XDIncompatibleFormatException {
		for (int i = 0; i < numTypes; i++) {
			Object stringOrObject = dataForType[i];
			if (stringOrObject != object)
				dataForType[i] = null;
		}
	}

	
	//-----------------------------------------------------------------------------------------------------
	//	Convenience methods to implement XDSelector.
	//	These are provided simply to allow the use of data.getString(xpath) instead of data.getSelector().getString(xpath)
	//

	/**
	 * This data type does not provide a list of records, so the {@link #next()} method only returns true once.<p>
	 * To iterate over elements within this object, use {@link #select(String)} or one of the {@link #foreach(String)} methods.
	 */
	@Override
	public Iterator<XSelector> iterator() {
		return null;
	}

	// Only iterate one time - as if there was a list of one record.
	private boolean beenToFirst = false;

	/**
	 * Return the number of records in this selection.
	 * <p>
	 * Actually, this will always return one. Iteration over
	 * this type is provided for consistency, but only accesses the top of the data hierarchy.
	 */
	@Override
	public int size() {
		return 1;
	}

	/**
	 * Return the index of the data element currently being iterated over.
	 * <p>
	 * Actually, this will always return zero (the first element). Iteration over
	 * this type is provided for consistency, but only accesses the top of the data hierarchy.
	 */
	@Override
	public void first() {
		beenToFirst = false;
	}

	@Override
	public boolean hasNext() {
		if (beenToFirst)
			return false;
		return true;
	}

	@Override
	public boolean next() {
		if (beenToFirst)
			return false;
		beenToFirst = true;
		return true;
	}

	@Override
	public String currentName() {
		try {
			return this.getSelector().currentName();
		} catch (XDException e) {
			// Should not be possible.
			// Just in case, we'll throw an exception, even though this method doesn't declare a thrown exception.
			throw new NullPointerException();
			//return "unknown";
		}
	}

	@Override
	public int currentIndex() {
		return 0;
	}

	@Override
	public boolean setCurrentIndex(int index) throws XDException {
		return this.getSelector().setCurrentIndex(index);
	}

	@Override
	public String getString(String xpath) throws XDException {
		return this.getSelector().getString(xpath);
	}

	@Override
	public XSelector select(String xpath) throws XDException {
		return this.getSelector().select(xpath);
	}

	@Override
	public void foreach(String xpath, Object userData, XDCallback callback) throws XDException {
		this.getSelector().foreach(xpath, userData, callback);
	}

	@Override
	public void foreach(String xpath, XDCallback callback) throws XDException {
		this.getSelector().foreach(xpath, callback);
	}

	@Override
	public Iterable<XSelector> foreach(String xpath) throws XDException {
		return this.getSelector().foreach(xpath);
	}
	
	
	//-----------------------------------------------------------------------------------------------------
	//	Stuff that should probably be elsewhere.
	public static final byte BOM_UTF16_1 = (byte) 0xFF;
	public static final byte BOM_UTF16_2 = (byte) 0xFE;
	public static final byte BOM_UTF8_1 = (byte) 0xEF;
	public static final byte BOM_UTF8_2 = (byte) 0xBB;
	public static final byte BOM_UTF8_3 = (byte) 0xBF;


	/**
	 * Read an input stream and return it as a string, checking for Unicode conversion if required.
	 * 
	 * @param inputStream
	 * @return File contents as a String.
	 * @throws IOException
	 */
	public static String loadStringFromReader(InputStream inputStream) throws IOException {
		
		// Read the file contents from a Reader object into a buffer.
		ByteArrayOutputStream writer = new ByteArrayOutputStream();
		byte[] buf = new byte[4 * 4096];
		String encoding = "UTF-8"; // Ascii can be loaded as UTF-8
		for (boolean startOfFile = true;; startOfFile = false) {
			int len = inputStream.read(buf);
			if (len < 0)
				break;

			// Check for byte markers that specify the encoding
			if (startOfFile) {
				if (len >= 2 && buf[0] == BOM_UTF16_1 && buf[1] == BOM_UTF16_2) {
					
					// UTF-16 - The BOM bytes are automatically stripped off during the conversion.
					encoding = "UTF-16";
					writer.write(buf, 0, len);
					continue;
				} else if (len >= 2 && buf[0] == BOM_UTF16_2 && buf[1] == BOM_UTF16_1) {
					
					// UTF-16 - The BOM bytes are automatically stripped off during the conversion.
					encoding = "UTF-16";
					writer.write(buf, 0, len);
					continue;
				} else if (len >= 3 && buf[0] == BOM_UTF8_1 && buf[1] == BOM_UTF8_2 && buf[2] == BOM_UTF8_3) {
					
					// UTF-8 - Strip off the BOM. For some reason java.io.String doesn't do this for UTF-8.
					encoding = "UTF-8";
					writer.write(buf, 3, len - 3);
					continue;
				}
			}
				
			// Not Unicode - Add the bytes to the buffer
			writer.write(buf, 0, len);
		}
		inputStream.close();

		// Convert the byte array to a string, using the appropriate encoding.
		byte[] array = writer.toByteArray();
		String contents = (encoding == null) ? new String(array) : new String(array, encoding);
		return contents;
	}

}
