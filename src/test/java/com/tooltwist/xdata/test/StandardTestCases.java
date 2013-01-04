package com.tooltwist.xdata.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.tooltwist.xdata.XD;
import com.tooltwist.xdata.XDException;
import com.tooltwist.xdata.XDCallback;
import com.tooltwist.xdata.XSelector;

public abstract class StandardTestCases {
	
	protected abstract String INPUT_DATA();
	protected abstract String DATA_TYPE();
	protected abstract Class<?> EXPECTED_SELECTOR_CLASS();
	protected abstract Class<?> EXPECTED_ITERATOR_CLASS();

	@Test
	public void conversion() throws XDException {
		
		// Create object with list of country/state/city
		XD data = new XD(INPUT_DATA());
		
		// Check the returned selector
		XSelector selector = data.getSelector(DATA_TYPE());
		if (selector == null) {
			fail("No object returned");
		} else if ( !EXPECTED_SELECTOR_CLASS().isInstance(selector)) {
			fail("Expected selector to be " + EXPECTED_SELECTOR_CLASS().getName());
		}
		
		// Get the object again.
		selector = data.getSelector(DATA_TYPE());
		if (selector == null) {
			fail("No object returned");
		} else if ( !EXPECTED_SELECTOR_CLASS().isInstance(selector)) {
			fail("Expected selector to be " + EXPECTED_SELECTOR_CLASS().getName());
		}
	}

	
	@Test
	public final void absolute_data_access() throws XDException {

		// Create object with list of country/state/city
		XD data = new XD(INPUT_DATA());
		XSelector selector = data.getSelector(DATA_TYPE());

		// Check the return type
		if ( !EXPECTED_SELECTOR_CLASS().isInstance(selector))
			fail("Expected selector to be " + EXPECTED_SELECTOR_CLASS().getName());
		
		Misc.checkXpathValue(selector, "/*/description", "Description");
		Misc.checkXpathValue(selector, "/*/false", "false");
		Misc.checkXpathValue(selector, "/*/null", "null");
		Misc.checkXpathValue(selector, "/*/true", "true");
		Misc.checkXpathValue(selector, "/*/zero", "0");
		Misc.checkXpathValue(selector, "/*/number", "12345");
		Misc.checkXpathValue(selector, "/*/negative", "-321");
		Misc.checkXpathValue(selector, "/*/float", "123.456");
		Misc.checkXpathValue(selector, "/*/huge", "123.456e789");
		Misc.checkXpathValue(selector, "/*/miniscule", "123.456e-789");

		Misc.checkXpathValue(selector, "/data/description", "Description");
		Misc.checkXpathValue(selector, "/data/false", "false");
		Misc.checkXpathValue(selector, "/data/null", "null");
		Misc.checkXpathValue(selector, "/data/true", "true");
		Misc.checkXpathValue(selector, "/data/zero", "0");
		Misc.checkXpathValue(selector, "/data/number", "12345");
		Misc.checkXpathValue(selector, "/data/negative", "-321");
		Misc.checkXpathValue(selector, "/data/float", "123.456");
		Misc.checkXpathValue(selector, "/data/huge", "123.456e789");
		Misc.checkXpathValue(selector, "/data/miniscule", "123.456e-789");

		// Select first element only in a list, when there are multiple matches.
		Misc.checkXpathValue(selector, "/*/country/name", "Australia");
		Misc.checkXpathValue(selector, "/data/country/name", "Australia");
		
		// Check top level looping
		assertTrue(selector.hasNext());
		selector.next();
		Misc.checkXpathValue(selector, "/*/country/name", "Australia");
		Misc.checkXpathValue(selector, "/data/country/name", "Australia");
		assertFalse(selector.hasNext());
		
		// Check re-looping
		selector.first();
		assertTrue(selector.hasNext());
		selector.next();
		Misc.checkXpathValue(selector, "/*/country/name", "Australia");
		Misc.checkXpathValue(selector, "/data/country/name", "Australia");
		assertFalse(selector.hasNext());
	}
	
	@Test
	public void relative_data_access() throws XDException {

		// Create object with list of country/state/city
		XSelector selector = new XD(INPUT_DATA()).getSelector(DATA_TYPE());
		XSelector data = selector.select("/data");

		// Check the return type
		if ( !EXPECTED_ITERATOR_CLASS().isInstance(data)) {
			fail("Expected selector to be " + EXPECTED_ITERATOR_CLASS().getName());
		}
		
		// Before calling data.next(), the values should be accessible.
		Misc.checkXpathValue(data, "./description", "Description");
		Misc.checkXpathValue(data, "./false", "false");
		Misc.checkXpathValue(data, "./null", "null");
		Misc.checkXpathValue(data, "./true", "true");
		Misc.checkXpathValue(data, "./zero", "0");
		Misc.checkXpathValue(data, "./number", "12345");
		Misc.checkXpathValue(data, "./negative", "-321");
		Misc.checkXpathValue(data, "./float", "123.456");
		Misc.checkXpathValue(data, "./huge", "123.456e789");
		Misc.checkXpathValue(data, "./miniscule", "123.456e-789");

		Misc.checkXpathValue(data, "description", "Description");
		Misc.checkXpathValue(data, "false", "false");
		Misc.checkXpathValue(data, "null", "null");
		Misc.checkXpathValue(data, "true", "true");
		Misc.checkXpathValue(data, "zero", "0");
		Misc.checkXpathValue(data, "number", "12345");
		Misc.checkXpathValue(data, "negative", "-321");
		Misc.checkXpathValue(data, "float", "123.456");
		Misc.checkXpathValue(data, "huge", "123.456e789");
		Misc.checkXpathValue(data, "miniscule", "123.456e-789");
		
		// Select first element only, when there are multiple matches.
		Misc.checkXpathValue(data, "./country/name", "Australia");
		Misc.checkXpathValue(data, "country/name", "Australia");
		
		// Check iterating on this node (should only be one loop)
		assertTrue(data.hasNext());
		data.next();
		Misc.checkXpathValue(data, "./description", "Description");
		Misc.checkXpathValue(data, "description", "Description");
		Misc.checkXpathValue(data, "./country/name", "Australia");
		Misc.checkXpathValue(data, "country/name", "Australia");
		assertFalse(data.hasNext());
		
		// Check re-looping
		data.first();
		assertTrue(data.hasNext());
		data.next();
		Misc.checkXpathValue(data, "./description", "Description");
		Misc.checkXpathValue(data, "description", "Description");
		Misc.checkXpathValue(data, "./country/name", "Australia");
		Misc.checkXpathValue(data, "country/name", "Australia");
		assertFalse("data.hasNext() should return false", data.hasNext());
		assertFalse("data.next() should return false", data.next());
		assertFalse("data.next() should return false", data.next());
		
		// Check we can't read values after running off the end.
		assertEquals("Expected data.string() to return null when called after end of list", null, data.getString("description"));
	}

	@Test
	public final void index_returning_field() throws XDException {

		// Create object with list of country/state/city
		XSelector data = new XD(INPUT_DATA()).getSelector(DATA_TYPE());
		
		// Accessing single fields
		Misc.checkXpathValue(data, "/*/country[1]/name", "Australia");
		Misc.checkXpathValue(data, "/*/country[2]/name", "New Zealand");
		Misc.checkXpathValue(data, "/*/country[1]/state[1]/abbreviation", "NSW");
		Misc.checkXpathValue(data, "/*/country[2]/state[2]/abbreviation", "SI");
	}

	@Test
	public final void index_returning_list() throws XDException {

		// Create object with list of country/state/city
		XSelector data = new XD(INPUT_DATA()).getSelector(DATA_TYPE());
		
		// Check selecting state/city via country
		XSelector cities = data.select("/*/country[1]/state[1]/city");
		assertEquals("Expected countries.size() to be 4", 4, cities.size());

		cities = data.select("/*/country[1]/state[2]/city");
		assertEquals("Expected countries.size() to be 4", 4, cities.size());

		cities = data.select("/*/country[2]/state[1]/city");
		assertEquals("Expected countries.size() to be 2", 2, cities.size());

		cities = data.select("/*/country[2]/state[2]/city");
		assertEquals("Expected countries.size() to be 2", 2, cities.size());

		// Selecting state/city - all countries at once
		cities = data.select("/*/country/state[1]/city");
		assertEquals("Expected countries.size() to be 6", 6, cities.size());
		
		cities = data.select("/*/country/state[2]/city");
		assertEquals("Expected countries.size() to be 6", 6, cities.size());
		
		cities = data.select("/*/country/state[3]/city");
		assertEquals("Expected countries.size() to be 0", 0, cities.size());
		
		cities = data.select("/*/country/state[4]/city");
		assertEquals("Expected countries.size() to be 0", 0, cities.size());		
	}

	@Test
	public void traditional_iteration() throws XDException {
		
		// Create object with list of country/state/city
		XSelector selector = new XD(INPUT_DATA()).getSelector(DATA_TYPE());
		
		// Select the records
		XSelector countries = selector.select("/*/country");
		if (EXPECTED_ITERATOR_CLASS().isInstance(countries)) {
			// As expected
		} else {
			fail("selector.select did not return " + EXPECTED_ITERATOR_CLASS().getName() + " object");
		}

		// Check the number of records.
		assertEquals("Expected countries.size() to be 2", 2, countries.size());
		
		// Iterate through the list
		int count = 0;
		for ( ; countries.next(); count++) {
			Misc.checkCountry(countries, count);
			
			// Check the currentIndex method works.
			int currentIndex = countries.currentIndex();
			assertEquals("Incorrect value from countries.currentIndex().", count, currentIndex);
		}
		assertEquals("Should be 2 countries", 2, count);
		
		// Check the first() method works, and we can iterate through the list again.
		countries.first();
		count = 0;
		for ( ; countries.next(); count++) {
			Misc.checkCountry(countries, count);
		}
		assertEquals("Should be 2 countries when re-looping", 2, count);
	}

	@Test
	public void traditional_iteration_with_subselect() throws XDException {
		// Create object with list of country/state/city
		XSelector selector = new XD(INPUT_DATA()).getSelector(DATA_TYPE());

		// Iterate through a list of states
		XSelector states = selector.select("/*/country/state");
		int countState = 0;
		for ( ; states.next(); countState++) {
//			String stateName = states.getString("name");
			
			// Get the cities in this state.
			XSelector cities = states.select("./city");
			if (EXPECTED_ITERATOR_CLASS().isInstance(cities)) {
				// As expected
			} else {
				fail("states.select did not return " + EXPECTED_ITERATOR_CLASS().getName() + " object");
			}
			
			// Check the right states were returned.
			int countCity = 0;
			for ( ; cities.next(); countCity++) {
//				String cityName = cities.getString(".");
				Misc.checkCity(cities, countState, countCity);
				
				// Check the currentIndex method works.
				int currentIndex = cities.currentIndex();
				assertEquals("Incorrect value from cities.currentIndex().", countCity, currentIndex);
			}
			
			// Check the number of cities selected
			int numCities = cities.size();
			switch (countState) {
			case 0: // NSW
				assertEquals("Expected 4 cities", 4, numCities);
				break;
			case 1: // VIC
				assertEquals("Expected 4 cities", 4, numCities);
				break;
			case 2: // NI
				assertEquals("Expected 2 cities", 2, numCities);
				break;
			case 3: // SI
				assertEquals("Expected 2 cities", 2, numCities);
				break;
			}
		}
		
		// Check the number of states selected
		if (countState != 4)
			fail("Should be 4 states");
	}

	@Test
	public void callback_iteration() throws XDException {
		
		// Create object with list of country/state/city
		XSelector selector = new XD(INPUT_DATA()).getSelector(DATA_TYPE());
		
		// Prepare a user defined object (we'll use it for counting).
		class Counter {
			public int count = 0;
		};
		Counter counter = new Counter();
		
		// Iterate through the records.
		selector.foreach("/*/country", counter, new XDCallback() {

			@Override
			public void next(XSelector item, int index, Object myData) throws XDException {
				Counter counter = (Counter) myData;
				Misc.checkCountry(item, index);

				// Check the currentIndex method works.
				int currentIndex = item.currentIndex();
				assertEquals("Incorrect value from item.currentIndex().", counter.count, currentIndex);
				
				counter.count++;
			}
		});
		
		// Check the number of countries
		assertEquals("Should be 2 countries", 2, counter.count);
	}

	@Test
	public void callback_iteration_with_subselect() throws XDException {
		
		// Create object with list of country/state/city
		XSelector selector = new XD(INPUT_DATA()).getSelector(DATA_TYPE());
		
		// Prepare a user defined object (we'll use it for counting).
		class Counter {
			public int countStates = 0;
			public int countCities = 0;
		};
		Counter counter = new Counter();
		
		// Iterate through the records.
		selector.foreach("/*/country/state", counter, new XDCallback() {

			@Override
			public void next(XSelector state, int index, Object myData) throws XDException {
				Counter counter = (Counter) myData;
//				String stateName = state.getString("name");

				counter.countCities = 0;
				state.foreach("./city", counter, new XDCallback() {

					@Override
					public void next(XSelector city, int index, Object myData) throws XDException {
						Counter counter = (Counter) myData;
//						String cityName = city.getString(".");
						Misc.checkCity(city, counter.countStates, counter.countCities);

						// Check the currentIndex method works.
						int currentIndex = city.currentIndex();
						assertEquals("Incorrect value from city.currentIndex().", counter.countCities, currentIndex);

						counter.countCities++;
					}
				});
				
				// Check the number of cities in this state
				switch (counter.countStates) {
				case 0: // NSW
					assertEquals("Expected 4 cities", 4, counter.countCities);
					break;
				case 1: // VIC
					assertEquals("Expected 4 cities", 4, counter.countCities);
					break;
				case 2: // NI
					assertEquals("Expected 2 cities", 2, counter.countCities);
					break;
				case 3: // SI
					assertEquals("Expected 2 cities", 2, counter.countCities);
					break;
				}

				counter.countStates++;
			}
		});
		
		// Check the number of states
		assertEquals("Should be 4 states", 4, counter.countStates);
	}
	
	@Test
	public void java_iteration() throws XDException {
		
		// Create object with list of country/state/city
		XSelector selector = new XD(INPUT_DATA()).getSelector(DATA_TYPE());

		// Check the countries
		int count = 0;
		for (XSelector countries : selector.foreach("/*/country")) {
			Misc.checkCountry(countries, count);

			// Check the currentIndex method works.
			int currentIndex = countries.currentIndex();
			assertEquals("Incorrect value from countries.currentIndex().", count, currentIndex);
			
			count++;
		}
		
		// Check the number of countries
		assertEquals("Should be 2 countries", 2, count);
	}
	
	@Test
	public void java_iteration_with_subselect() throws XDException {
		
		// Create object with list of country/state/city
		XSelector selector = new XD(INPUT_DATA()).getSelector(DATA_TYPE());

		// Iterate through the states
		int countState = 0;
		for (XSelector state : selector.foreach("/*/country/state")) {
//			String stateName = state.getString("name");

			// Get the cities in this state.
			int countCity = 0;
			for (XSelector city : state.foreach("city")) {
//				String cityName = city.getString(".");
				Misc.checkCity(city, countState, countCity);
				

				// Check the currentIndex method works.
				int currentIndex = city.currentIndex();
				assertEquals("Incorrect value from city.currentIndex().", countCity, currentIndex);

				countCity++;
			}

			// Check there were the right number of cities
			switch (countState) {
			case 0: // NSW
				assertEquals("Expected 4 cities", 4, countCity);
				break;
			case 1: // VIC
				assertEquals("Expected 4 cities", 4, countCity);
				break;
			case 2: // NI
				assertEquals("Expected 2 cities", 2, countCity);
				break;
			case 3: // SI
				assertEquals("Expected 2 cities", 2, countCity);
				break;
			}
			
			countState++;
		}
		
		// Check there were the right number of states
		assertEquals("Should be 4 states", 4, countState);
	}
	
	// Check * returns variable field types
	
	@Test
	public void wildcard_data_access() throws XDException {

		// Create object with list of country/state/city
		XSelector selector = new XD(INPUT_DATA()).getSelector(DATA_TYPE());
		XSelector fields = selector.select("/data/*");

		// Check the return type
		if ( !EXPECTED_ITERATOR_CLASS().isInstance(fields)) {
			fail("Expected selector to be " + EXPECTED_ITERATOR_CLASS().getName());
		}
		
		// Loop through the fields.
		for (int i = 0; i <= 10; i++) {
			assertTrue(fields.next());
			String fieldName = fields.currentName();
			String value = fields.getString(".");
			switch (i) {
			case 0:	Misc.checkWildcard(i, "description", fieldName, "Description", value); break;
			case 1:	Misc.checkWildcard(i, "descriptionAltLang", fieldName, "Ze description", value); break;
			case 2:	Misc.checkWildcard(i, "false", fieldName, "false", value); break;
			case 3:	Misc.checkWildcard(i, "null", fieldName, "null", value); break;
			case 4:	Misc.checkWildcard(i, "true", fieldName, "true", value); break;
			case 5:	Misc.checkWildcard(i, "zero", fieldName, "0", value); break;
			case 6:	Misc.checkWildcard(i, "number", fieldName, "12345", value); break;
			case 7:	Misc.checkWildcard(i, "negative", fieldName, "-321", value); break;
			case 8:	Misc.checkWildcard(i, "float", fieldName, "123.456", value); break;
			case 9:	Misc.checkWildcard(i, "huge", fieldName, "123.456e789", value); break;
			case 10:Misc.checkWildcard(i, "miniscule", fieldName, "123.456e-789", value); break;
			}
		}

		// Test the same, but with a sub-select.
		XSelector data = selector.select("/data");
		data.next();
		fields = data.select("./*");
		for (int i = 0; i <= 10; i++) {
			assertTrue(fields.next());
			String fieldName = fields.currentName();
			String value = fields.getString(".");
			switch (i) {
			case 0:	Misc.checkWildcard(i, "description", fieldName, "Description", value); break;
			case 1:	Misc.checkWildcard(i, "descriptionAltLang", fieldName, "Ze description", value); break;
			case 2:	Misc.checkWildcard(i, "false", fieldName, "false", value); break;
			case 3:	Misc.checkWildcard(i, "null", fieldName, "null", value); break;
			case 4:	Misc.checkWildcard(i, "true", fieldName, "true", value); break;
			case 5:	Misc.checkWildcard(i, "zero", fieldName, "0", value); break;
			case 6:	Misc.checkWildcard(i, "number", fieldName, "12345", value); break;
			case 7:	Misc.checkWildcard(i, "negative", fieldName, "-321", value); break;
			case 8:	Misc.checkWildcard(i, "float", fieldName, "123.456", value); break;
			case 9:	Misc.checkWildcard(i, "huge", fieldName, "123.456e789", value); break;
			case 10:Misc.checkWildcard(i, "miniscule", fieldName, "123.456e-789", value); break;
			}
		}
	}
	
	// Check .currentIndex()
	
	// Check .currentName()
	
	// Check Unicode handling in names and values
	
	// Check escape characters in names and values
	
	// Check selecting specific occurrances
	
	// Check non-xpath selectors. e.g. x.y and a[1].b
	
	// Numeric values
	
	// setCurrentIndex()
	
	// To Document - known inconsistencies:
	//    xml-dom, xml-fast: selection of a parent node (DOM returns spaces around children, FAST returns contents of children)
	
	
	// Read from file

	
//	@Test
//	public final void read_from_file() throws XDException {
//
//		// Create object with list of country/state/city
//		InputStream inputStream = new FileInputStream("data/suburbDescription.json");
//		XD data = new XD(inputStream);
//		XDSelector selector = data.getSelector(DATA_TYPE());
//
//		// Check the return type
//		if ( !EXPECTED_SELECTOR_CLASS().isInstance(selector))
//			fail("Expected selector to be " + EXPECTED_SELECTOR_CLASS().getName());
//	}	

}
