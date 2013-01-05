package com.tooltwist.xdata.test;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tooltwist.fastJson.FastJson;


public class FastJsonTest {
	
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

	@Test
	public void strings_and_literals() {
		try {
			
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
			new FastJson(json);

		} catch (Exception e) {
			fail(e.toString());
		}
	}

	@Test
	public void nested_object() {
		try {
			
			String json = "{"
					+ "\"object\" : { \"name1\": \"val1\", \"name2\"   : \"val2\" } "
//						+ "\"array\" : [ \"val1\", \"val\", \"val3\" ] "
					+ "}";
			new FastJson(json);

		} catch (Exception e) {
			fail(e.toString());
		}
	}

	@Test
	public void nested_array() {
		try {

			String json = "{"
					+ "\"array\" : [ \"val1\", \"val\", \"val3\" ] "
					+ "}";
			new FastJson(json);

		} catch (Exception e) {
			fail(e.toString());
		}
	}

	@Test
	public void top_level_array() {
		try {

			String json = "["
					+ " \"val1\", \"val\", \"val3\" "
					+ "]";
			new FastJson(json);

		} catch (Exception e) {
			fail(e.toString());
		}
	}

	@Test
	public void numbers() {
		try {

			String json = "{"
					+ "\"zero\" : 0, "
					+ "\"number\" : 12345, "
					+ "\"negative\" : -321, "
					+ "\"float\" : 123.456, "
					+ "\"float_negative\" : -123.456, "
					+ "\"huge\" : 123.456e789, "
					+ "\"huge_negative\" : -123.456e789, "
					+ "\"miniscule\" : 123.456e-789, "
					+ "\"miniscule_negative\" : -123.456e-789 "
					+ "}";
			new FastJson(json);

		} catch (Exception e) {
			fail(e.toString());
		}
	}

}