package com.tooltwist.xdata.test;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tooltwist.fastJson.FastJson;
import com.tooltwist.fastXml.FastXml;
import com.tooltwist.xdata.XD;
import com.tooltwist.xdata.XDException;
import com.tooltwist.xdata.XDIncompatibleFormatException;


public class XD_Test {
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
//		// This will force the config to be loaded
//		ToolTwist.getMode();
//		
//		// Call something, to load the configs.
//		credentials = XpcLogin.login(DinaaUtilUrls.USER_TYPE, "administrator", "administrator");
//		String xml = "<input/>";
//		XData input = new XData(xml);
//		try {
//			XDS.call(credentials, "a.b.c", "select", input);
//		} catch (XpcException e) {
//			// This is expected
//		}
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	private String getJsonString() {
		String json = "{"
				+ "\"fname\" : \"Fred\", "
				+ "\"surname\" : \"Smith\", "
				+ "\"surname\" : \"bquote\\\\.quote\\/.tab\\t.nl\\n.cr\\r.\", "
				+ "\"boolean\" : false, "
				+ "\"boolean\" : null, "
				+ "\"boolean\" : true, "
				+ "\"object\" : { \"name1\": \"val1\", \"name2\": \"val2\" } "
//					+ "\"array\" : [ \"val1\", \"val\", \"val3\" ] "
				+ "}";
		return json;
	}

//	private String getJsonString2() {
//		String json = "["
//				+ "  {"
//				+ "    \"country\" : \"Fred\", "
//				+ "      \"states\" : ["
//				+ "        \"state\" : \"New South Wales\", "
//				+ "\"boolean\" : false, "
//				+ "\"boolean\" : null, "
//				+ "\"boolean\" : true, "
//				+ "\"object\" : { \"name1\": \"val1\", \"name2\": \"val2\" } "
////					+ "\"array\" : [ \"val1\", \"val\", \"val3\" ] "
//				+ "}";
//		return json;
//	}

	private String getXmlString() {
		String json = " <record>"
				+ "  <fname>Fred</fname>"
				+ "  <surname>Smith</surname>"
				+ "  <boolean>false</boolean>"
				+ "  <escapes>---&amp;---&lt;---</escapes>"
				// Should have cdata
				+ "  <object>"
				+ "    <name1>val1</name1>"
				+ "    <name2>val2</name2>"
				+ "  </object>"
				+ "</record>";
		return json;
	}

//	private String getXmlString2() {
//		String json = "<locations>"
//				+ "  <country>"
//				+ "    <name>Australia</name>"
//				+ "    <state>"
//				+ "      <abbreviation>NSW</abbreviation>"
//				+ "      <name>New South Wales</name>"
//				+ "      <city>Sydney</city>"
//				+ "      <city>Wollongong</city>"
//				+ "      <city>Newcastle</city>"
//				+ "      <city>Dubbo</city>"
//				+ "    </state>"
//				+ "    <state>"
//				+ "      <abbreviation>VIC</abbreviation>"
//				+ "      <name>Victoria</name>"
//				+ "      <city>Melbourne</city>"
//				+ "      <city>Wodonga</city>"
//				+ "      <city>Dandenong</city>"
//				+ "      <city>Wangaratta</city>"
//				+ "    </state>"
//				+ "  </country>"
//				+ "  <country>"
//				+ "    <name>New Zealand</name>"
//				+ "    <state>"
//				+ "      <abbreviation>NI</abbreviation>"
//				+ "      <name>North Island</name>"
//				+ "      <city>Auckland</city>"
//				+ "      <city>Wellington</city>"
//				+ "    </state>"
//				+ "    <state>"
//				+ "      <abbreviation>SI</abbreviation>"
//				+ "      <name>South Island</name>"
//				+ "      <city>Christchurch</city>"
//				+ "      <city>Queenstown</city>"
//				+ "    </state>"
//				+ "  </country>"
//				+ "</locations>";
//		return json;
//	}
	
	//--------------------------------------------------------------------------------------------------------------------
	// Tests

	@Test
	public void constructor_badString() {
		try {
			String in = ")(*&@#%&^@";
	//		XD data = 
					new XD(in);
	//		String string = data.getString();		
	//		assertSame("String changed", json, string);
			fail("Constructor should have failed to recognise garbage data");
		} catch (XDException e) {
			// Expected exception
			assertSame(e.getMessage(), "Could not determine data type");
			return;
		}
	}

	@Test
	public void constructor_jsonString() throws XDException {
		String json = getJsonString();
		XD data = new XD(json);
		String string = data.getString();		
		assertSame("String changed", json, string);
	}

	@Test
	public void constructor_xmlString() throws XDException {
		String xml = getXmlString();
		XD data = new XD(xml);
		String string = data.getString();		
		assertSame("String changed", xml, string);
	}

	@Test
	public void default_json_selector() throws XDException {
		String json = getJsonString();
		XD data = new XD(json);
		
		// Check the returned selector
		Object object = data.getSelector();
		if (object == null) {
			fail("No object returned");
		} else if (object instanceof FastJson) {
			// This is what we expect. All is well if we get here.
		} else {
			fail("Expected an object of type " + FastJson.class.getName());
		}
		
		// Get the object again.
		object = data.getSelector();
		if (object == null) {
			fail("No object returned");
		} else if (object instanceof FastJson) {
			// This is what we expect. All is well if we get here.
		} else {
			fail("Expected an object of type " + FastJson.class.getName());
		}
	}

	@Test
	public void default_xml_selector() throws XDException {
		String xml = getXmlString();
		XD data = new XD(xml);
		
		// Check the returned selector
		Object object = data.getSelector();
		if (object == null) {
			fail("No object returned");
		} else if (object instanceof FastXml) {
			// This is what we expect. All is well if we get here.
		} else {
			fail("Expected an object of type " + FastXml.class.getName());
		}
		
		// Get the object again.
		object = data.getSelector();
		if (object == null) {
			fail("No object returned");
		} else if (object instanceof FastXml) {
			// This is what we expect. All is well if we get here.
		} else {
			fail("Expected an object of type " + FastXml.class.getName());
		}
	}

	@Test
	public void json_badConversions() throws XDException {

		// Attempt to convert to string format
		String newType = "json-string";
		try {
			String json = getJsonString();
			XD data = new XD(json);
			data.getSelector(newType);
			fail("Should not be able to convert from json to '" + newType + "'.");
		} catch (XDIncompatibleFormatException e) {
			// Expected exception
		}

		// Attempt to convert to unknown format
		newType = "zuxhdgrtrf";
		try {
			String json = getJsonString();
			XD data = new XD(json);
			data.getSelector(newType);
			fail("Should not be able to convert from json to '" + newType + "'.");
		} catch (XDIncompatibleFormatException e) {
			// Expected exception
		}

//		// Attempt to convert from JSON to XML
//		newType = "xml-fast";
//		try {
//			String json = getJsonString();
//			XD data = new XD(json);
//			data.getSelector(newType);
//			fail("Should not be able to convert from json to '" + newType + "'.");
//		} catch (XDUnknownConversionException e) {
//			// Expected exception
//		}
	}

	@Test
	public void xml_to_json() throws XDException {
		System.err.println("Test skipped: xml_to_json");
//			String xml = getXmlString();
//			XD data = new XD(xml);
//			XDSelector selector = data.getSelector("json-fast");
	}

	/**
	 * Check for data list overflow.
	 * The XD object contains a list of data in different formats, initialized in
	 * the constructor. The number of slots in the list is set to the number of data
	 * types. Here we check that adding new types doesn't cause that list to overflow
	 * in subsequent operations.
	 * 
	 * @throws XDException
	 */
//	@Test
//	public void newTypes() throws XDException {
//		String json = getJsonString();
//		XD data = new XD(json);
//		
//		Z Z Z Z Z
//		
//		// Add types
//		
//		// Do stuff
//	}

}
