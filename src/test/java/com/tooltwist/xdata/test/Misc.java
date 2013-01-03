package com.tooltwist.xdata.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.tooltwist.xdata.XDException;
import com.tooltwist.xdata.XDSelector;

public class Misc {

	public static void checkXpathValue(XDSelector selector, String xpath, String expectedValue) throws XDException {
		String value = selector.getString(xpath);
		assertEquals("xpath '" + xpath + "' should return value '" + expectedValue + "'", expectedValue, value);
	}


	public static void checkCountry(XDSelector countries, int cnt) throws XDException {
		switch (cnt) {
		case 0:
			Misc.checkXpathValue(countries, "name", "Australia");
			break;
		case 1:
			Misc.checkXpathValue(countries, "name", "New Zealand");
			break;
		default:
			fail("Iterated too many times on countries (" + cnt + ")");
		}
	}

	public static void checkCity(XDSelector data, int stateNo, int cityNo) throws XDException {
		String city = data.getString(".");
		checkCity(city, stateNo, cityNo);
	}
	
	public static void checkCity(String actualValue, int stateNo, int cityNo) {
		String expected = null;
		
		// NSW
		if (stateNo==0 && cityNo == 0)
			expected = "Sydney";
		if (stateNo==0 && cityNo==1)
			expected = "Wollongong";
		if (stateNo==0 && cityNo==2)
			expected = "Newcastle";
		if (stateNo==0 && cityNo==3)
			expected = "Dubbo";

		// VIC
		if (stateNo==1 && cityNo==0)
			expected = "Melbourne";
		if (stateNo==1 && cityNo==1)
			expected = "Wodonga";
		if (stateNo==1 && cityNo==2)
			expected = "Dandenong";
		if (stateNo==1 && cityNo==3)
			expected = "Wangaratta";

		// NI
		if (stateNo==2 && cityNo==0)
			expected = "Auckland";
		if (stateNo==2 && cityNo==1)
			expected = "Wellington";

		// SI
		if (stateNo==3 && cityNo==0)
			expected = "Christchurch";
		if (stateNo==3 && cityNo==1)
			expected = "Queenstown";
		
		if (expected == null)
			fail("Unknown state/city: " + stateNo + "/" + cityNo + ": returned " + actualValue + ".");

		assertEquals("Incorrect city for " + stateNo + "/" + cityNo, actualValue, expected);
	}

	public static void checkWildcard(int index, String expectedName, String actualName, String expectedValue, String actualValue) {
		assertEquals("Incorrect name for item " + index + ":", expectedName, actualName);
		assertEquals("Incorrect value for item " + index + ":", expectedValue, actualValue);
	}

}
