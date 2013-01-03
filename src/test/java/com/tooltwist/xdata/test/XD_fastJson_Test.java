package com.tooltwist.xdata.test;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tooltwist.fastJson.FastJson;
import com.tooltwist.fastJson.FastJsonNodes;
import com.tooltwist.xdata.XD;
import com.tooltwist.xdata.XDException;
import com.tooltwist.xdata.XDSelector;


public class XD_fastJson_Test extends StandardTestCases {

	@Override
	protected String INPUT_DATA() {
		return TestData.country_state_city_json;
	}

	@Override
	protected String DATA_TYPE() {
		return "json-fast";
	}

	@Override
	protected Class<?> EXPECTED_SELECTOR_CLASS() {
		return FastJson.class;
	}

	@Override
	protected Class<?> EXPECTED_ITERATOR_CLASS() {
		return FastJsonNodes.class;
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
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

//	private String getJsonString() {
//		String json = "{"
//				+ "\"fname\" : \"Fred\", "
//				+ "\"surname\" : \"Smith\", "
//				+ "\"surname\" : \"bquote\\\\.quote\\/.tab\\t.nl\\n.cr\\r.\", "
//				+ "\"boolean\" : false, "
//				+ "\"boolean\" : null, "
//				+ "\"boolean\" : true, "
//				+ "\"object\" : { \"name1\": \"val1\", \"name2\": \"val2\" } "
////					+ "\"array\" : [ \"val1\", \"val\", \"val3\" ] "
//				+ "}";
//		return json;
//	}
	
	//--------------------------------------------------------------------------------------------------------------------
	// Tests


	@Test
	public void showDebug() throws XDException {
		
		// Create object with list of country/state/city
		XD data = new XD(INPUT_DATA());
		XDSelector selector = data.getSelector(DATA_TYPE());

		// Display debug
		FastJson fastJson = (FastJson) selector;
		fastJson.debugDump();
	}

	@Test
	public void invalidJson() {
		
		try {
			// Create an XD with invalid JSON, but make sure it is recognized as JSON.
			String invalidJson = "{ \"name\" : \"Phil\"";
			XD data = new XD(invalidJson);
			
			// Asking for the specific selector will force parsing, which should cause an exception to be thrown.
			data.getSelector("json-fast");
			
			// Should not be able to get here.
			fail("Did not throw an exception with invalid JSON");
		} catch (XDException e) {
			
			// Check it's the expected exception.
			String msg = e.toString();
			if (msg.contains("JSON parse error")) {
				// As expected. This is good.
			} else {
				fail("Unexpected exception: " + e.toString());
			}
		}
	}

}
