package com.tooltwist.xdata.test;

import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tooltwist.fastXml.FastXml;
import com.tooltwist.fastXml.FastXmlNodes;
import com.tooltwist.xdata.XD;
import com.tooltwist.xdata.XDException;


public class XD_fastXml_Test extends StandardTestCases {

	@Override
	protected String INPUT_DATA() {
		return TestData.country_state_city_xml;
	}

	@Override
	protected String DATA_TYPE() {
		return "xml-fast";
	}

	@Override
	protected Class<?> EXPECTED_SELECTOR_CLASS() {
		return FastXml.class;
	}

	@Override
	protected Class<?> EXPECTED_ITERATOR_CLASS() {
		return FastXmlNodes.class;
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
	
	@Test
	public void invalidXml() {
		
		try {
			// Create an XD with invalid XML, but make sure it is recognizable as XML.
			String invalidXml = "<hello>This is invalid<hello>";
			XD data = new XD(invalidXml);
			
			// Asking for the specific selector will force parsing, which should cause an exception to be thrown.
			data.getSelector("xml-fast");
			
			// Should not be able to get here.
			fail("Did not throw an exception with invalid XML");
		} catch (XDException e) {
			
			// Check it's the expected exception.
			String msg = e.toString();
			if (msg.contains("XML parse error")) {
				// As expected. This is good.
			} else {
				fail("Unexpected exception: " + e.toString());
			}
		}
	}
}
