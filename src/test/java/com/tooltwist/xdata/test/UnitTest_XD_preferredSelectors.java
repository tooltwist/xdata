package com.tooltwist.xdata.test;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.fail;

import com.tooltwist.fastJson.FastJson;
import com.tooltwist.fastXml.FastXml;
import com.tooltwist.xdata.XD;
import com.tooltwist.xdata.XDException;
import com.tooltwist.xdata.XDSelector;


public class UnitTest_XD_preferredSelectors {
	
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
	public final void preferred_xml_selector() throws XDException {
		
		check(TestData.country_state_city_xml, FastXml.class);
	}

	
	@Test
	public final void preferred_json_selector() throws XDException {
		check(TestData.country_state_city_json, FastJson.class);
	}

	public final void check(String string, Class<?> expectedClass) throws XDException {

		XD data = new XD(string);
		XDSelector selector = data.getSelector();
		if ( !expectedClass.isInstance(selector))
			fail("Expected selector to be " + expectedClass.getName());
	}

}
