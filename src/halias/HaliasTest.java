/*
        Copyright (c) 2014 Mikko Koho

        Licensed under the MIT License (MIT).

        Permission is hereby granted, free of charge, to any person obtaining a copy
        of this software and associated documentation files (the "Software"), to deal
        in the Software without restriction, including without limitation the rights
        to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
        copies of the Software, and to permit persons to whom the Software is
        furnished to do so, subject to the following conditions:
        The above copyright notice and this permission notice shall be included in all
        copies or substantial portions of the Software.
        THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
        IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
        FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
        AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
        LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
        OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
        SOFTWARE.
*/

package halias;

import static org.junit.Assert.*;
import halias.DailyWeather.MorningWeather;
import halias.DailyWeather.WindInstance;
import halias.WeatherData.DayLength;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.XSD;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Unit tests for Halias data conversions.
 */
public class HaliasTest {
	
	Double EPSILON = 0.00000000001;

	@Test
	public void testHaliasWind1() {
		@SuppressWarnings("unused")
		HaliasDataProcessor test = new HaliasDataProcessor();
		assertEquals(WeatherData.windDirections.get(70), "E");
	}

	@Test
	public void testHaliasWindCounts() {
		@SuppressWarnings("unused")
		HaliasDataProcessor test = new HaliasDataProcessor();
		
		HashMap<String, Integer> windDirectionCounts = new HashMap<String, Integer>(8);
		
		for (int i = 0; i < WeatherData.possibleWindDirections.length; i++) {
			String windDir = WeatherData.possibleWindDirections[i];
			windDirectionCounts.put(windDir, 0);			
		}
		
		for (String direction : WeatherData.windDirections.values()) {
			Integer directionCount = windDirectionCounts.get(direction);
			directionCount++;
			windDirectionCounts.put(direction, directionCount);
		}

		for (int i = 0; i < WeatherData.possibleWindDirections.length; i++) {
			String windDir = WeatherData.possibleWindDirections[i];
			String failMessage = windDir + " - " + windDirectionCounts.get(windDir).toString();
			if (windDir.length() == 2)
				// Half-cardinal points have 4 occurences
				assertTrue(failMessage, windDirectionCounts.get(windDir) == 4);
			else if (windDir == "N")
				// North has one doubled point (0 and 360)
				assertTrue(failMessage, windDirectionCounts.get(windDir) == 6);
			else
				assertTrue(failMessage, windDirectionCounts.get(windDir) == 5);
		}
	}

	
	@Test
	public void testCalculateMeasurementAverage() {
		
		DailyWeather daily = new DailyWeather();
		
		Double[] measurements = {-180.0, 0.0, 180.0, 360.0 };  
		
		for (Integer minutes0 = 0; minutes0 < 180; minutes0++) {
			Double average = daily._calculateMeasurementAverage(measurements, minutes0, 240);
			Integer expectedAverage = -60 + minutes0;
			String message = "Got: " + Double.toString(average) + ", expecting: " + expectedAverage;
			assertTrue(message, Math.abs(average - expectedAverage) < EPSILON);
		}
	}

    @Test
    public void testCalculateMeasurementAverage2() {

        DailyWeather daily = new DailyWeather();

        Double[] measurements = {-180.0, 0.0, 180.0, 360.0 };

        for (Integer minutes0 = 0; minutes0 < 180; minutes0++) {
            Double average = daily._calculateMeasurementAverage(measurements, minutes0, 120);
            Integer expectedAverage = -120 + minutes0;
            String message = "Got: " + Double.toString(average) + ", expecting: " + expectedAverage;
            assertTrue(message, Math.abs(average - expectedAverage) < EPSILON);
        }
    }

    @Test
	public void testCalculateMorningWeatherAll() {
		
		DailyWeather daily = new DailyWeather();
		daily.weatherObservation[0] = daily.new ConcurrentObservations(3.0, 1000.0, 7, 5, 80, "N");
		daily.weatherObservation[1] = daily.new ConcurrentObservations(0.0, 1000.0, 7, 5, 80, "N");
		daily.weatherObservation[2] = daily.new ConcurrentObservations(0.0, 1000.0, 7, 5, 80, "N");
		daily.weatherObservation[3] = daily.new ConcurrentObservations(5.0, 1000.0, 7, 5, 80, "N");
		daily.weatherObservation[4] = daily.new ConcurrentObservations(10.0, 1000.0, 7, 5, 80, "N");
		
		MorningWeather morning = daily.calculateMorningWeather(02, 00, 6);
		
		Double expectedTemperature = 0.125;
		Double expectedPressure = 1000.0;
		Double expectedCloudCover = 5.0;
		Double expectedHumidity = 80.0;
		
		assertTrue(morning.temperature.toString(), Math.abs(morning.temperature - expectedTemperature) < EPSILON);
		assertTrue(morning.pressure.toString(), Math.abs(morning.pressure - expectedPressure) < EPSILON);
		assertTrue(morning.cloudCover.toString(), Math.abs(morning.cloudCover - expectedCloudCover) < EPSILON);
		assertTrue(morning.humidity.toString(), Math.abs(morning.humidity - expectedHumidity) < EPSILON);
		
		assertTrue(morning.winds.size() == 2);
		assertTrue(morning.winds.get(0).speed == 7);
		assertTrue(morning.winds.get(0).dir == "N");

        // Winter month

        morning = daily.calculateMorningWeather(02, 00, 2);

        assertFalse(morning.temperature.toString(), Math.abs(morning.temperature - expectedTemperature) < EPSILON);
        assertTrue(morning.pressure.toString(), Math.abs(morning.pressure - expectedPressure) < EPSILON);
        assertTrue(morning.cloudCover.toString(), Math.abs(morning.cloudCover - expectedCloudCover) < EPSILON);
        assertTrue(morning.humidity.toString(), Math.abs(morning.humidity - expectedHumidity) < EPSILON);

        assertTrue(morning.winds.size() == 1);
        assertTrue(morning.winds.get(0).speed == 7);
        assertTrue(morning.winds.get(0).dir == "N");
    }

	@Test
	public void testCalculateMorningWeatherTemp2() {
		
		DailyWeather daily = new DailyWeather();
		daily.weatherObservation[0] = daily.new ConcurrentObservations(2.0, 1000.0, 7, 5, 80, "NE");
		daily.weatherObservation[1] = daily.new ConcurrentObservations(1.0, 1000.0, 7, 5, 80, "NE");
		daily.weatherObservation[2] = daily.new ConcurrentObservations(2.0, 1000.0, 7, 5, 80, "NE");
		daily.weatherObservation[3] = daily.new ConcurrentObservations(0.0, 1000.0, 7, 5, 80, "NE");
		daily.weatherObservation[4] = daily.new ConcurrentObservations();
		
		MorningWeather morning = daily.calculateMorningWeather(01, 59, 6);
		
		// This is an approximation
		Double expectedAverage = 1.41625; // ((1 + 0.33 / 2) + 1.5 * 3) / 4;
		
		assertTrue(Math.abs(morning.temperature - expectedAverage) < 0.01);
	}

	@Test
	public void testCalculateMorningWeatherTemp3() {
		
		DailyWeather daily = new DailyWeather();
		daily.weatherObservation[0] = daily.new ConcurrentObservations(-1000.0, 1000.0, 7, 5, 80, "NE");
		daily.weatherObservation[1] = daily.new ConcurrentObservations(-1000.0, 1000.0, 7, 5, 80, "NE");
		daily.weatherObservation[2] = daily.new ConcurrentObservations(-18.0, 1000.0, 7, 5, 80, "NE");
		daily.weatherObservation[3] = daily.new ConcurrentObservations(0.0, 1000.0, 7, 5, 80, "NE");
		daily.weatherObservation[4] = daily.new ConcurrentObservations(90.0, 1000.0, 7, 5, 80, "NE");
		
		MorningWeather morning = daily.calculateMorningWeather(7, 10, 6);
		
		assertNotNull(morning);
		assertNotNull(morning.temperature);
		
		// This is an approximation
		Double expectedAverage = 15.0833;
		
		assertTrue(morning.temperature.toString(), Math.abs(morning.temperature - expectedAverage) < 0.001);
	}

	@Test
	public void testCalculateMorningWeatherTemp4() {
		
		DailyWeather daily = new DailyWeather();
		daily.weatherObservation[0] = daily.new ConcurrentObservations(-1000.0, 1000.0, 7, 5, 80, "NE");
		daily.weatherObservation[1] = daily.new ConcurrentObservations();
		daily.weatherObservation[2] = daily.new ConcurrentObservations(-18.0, 1000.0, 7, 5, 80, "NE");
		daily.weatherObservation[3] = daily.new ConcurrentObservations(0.0, 1000.0, 7, 5, 80, "NE");
		daily.weatherObservation[4] = daily.new ConcurrentObservations(90.0, 1000.0, 7, 5, 80, "NE");
		
		MorningWeather morning = daily.calculateMorningWeather(5, 30, 6);
		
		assertNull(morning.temperature);
	}

	@Test
	public void testCalculateMorningWeatherPressure() {
		
		DailyWeather daily = new DailyWeather();
		daily.weatherObservation[0] = daily.new ConcurrentObservations(0.0, 1000.0, 7, 5, 80, "NE");
		daily.weatherObservation[1] = daily.new ConcurrentObservations(0.0, 900.0, 7, 5, 80, "NE");
		daily.weatherObservation[2] = daily.new ConcurrentObservations(0.0, 1000.0, 7, 5, 80, "NE");
		daily.weatherObservation[3] = daily.new ConcurrentObservations(0.0, 1000.0, 7, 5, 80, "NE");
		daily.weatherObservation[4] = daily.new ConcurrentObservations(0.0, 1000.0, 7, 5, 80, "NE");
		
		MorningWeather morning = daily.calculateMorningWeather(1, 00, 6);
		
		// This is an approximation
		Double expectedAverage = 933.3333333;
		String message = "Got: " + Double.toString(morning.pressure) + ", expecting: " + expectedAverage;
		
		assertTrue(message, Math.abs(morning.pressure - expectedAverage) < 0.01);
	}

	@Test
	public void testCalculateMorningWeatherWindDirs() {
		
		DailyWeather daily = new DailyWeather();
		daily.weatherObservation[0] = daily.new ConcurrentObservations(3.0, 1000.0, 7, 5, 80, "N");
		daily.weatherObservation[1] = daily.new ConcurrentObservations(0.0, 1000.0, 7, 5, 80, "NE");
		daily.weatherObservation[2] = daily.new ConcurrentObservations(0.0, 1000.0, 7, 5, 80, "E");
		daily.weatherObservation[3] = daily.new ConcurrentObservations(5.0, 1000.0, 7, 5, 80, "SE");
		daily.weatherObservation[4] = daily.new ConcurrentObservations(10.0, 1000.0, 7, 5, 80, "S");
		
		MorningWeather morning = daily.calculateMorningWeather(04, 59, 6);
		
		assertTrue(Integer.toString(morning.winds.size()), morning.winds.size() == 1);
		assertTrue(morning.winds.get(0).speed == 7);
		assertTrue(morning.winds.get(0).dir == "E");
	}

	@Test
	public void testCalculateMorningWeatherWindDirs2() {
		
		DailyWeather daily = new DailyWeather();
		daily.weatherObservation[0] = daily.new ConcurrentObservations(3.0, 1000.0, 7, 5, 80, "N");
		daily.weatherObservation[1] = daily.new ConcurrentObservations(0.0, 1000.0, 8, 5, 80, "SE");
		daily.weatherObservation[2] = daily.new ConcurrentObservations(0.0, 1000.0, 6, 5, 80, "E");
		daily.weatherObservation[3] = daily.new ConcurrentObservations(5.0, 1000.0, 7, 5, 80, "NE");
		daily.weatherObservation[4] = daily.new ConcurrentObservations(10.0, 1000.0, 9, 5, 80, "S");
		
		MorningWeather morning = daily.calculateMorningWeather(02, 00, 6);
		
		assertTrue(morning.winds.size() == 2);
		assertTrue(morning.winds.contains(daily.weatherObservation[1].wind));
		assertTrue(morning.winds.contains(daily.weatherObservation[2].wind));
	}

	@Test
	public void testCalculateMorningWeatherCloudCover() {
		
		DailyWeather daily = new DailyWeather();
		daily.weatherObservation[0] = daily.new ConcurrentObservations(3.0, 1000.0, 7, 8, 80, "N");
		daily.weatherObservation[1] = daily.new ConcurrentObservations(0.0, 1000.0, 7, 7, 80, "NE");
		daily.weatherObservation[2] = daily.new ConcurrentObservations(0.0, 1000.0, 7, 2, 80, "E");
		daily.weatherObservation[3] = daily.new ConcurrentObservations(5.0, 1000.0, 7, 1, 80, "SE");
		daily.weatherObservation[4] = daily.new ConcurrentObservations(10.0, 1000.0, 7, 1, 80, "S");
		
		MorningWeather morning = daily.calculateMorningWeather(00, 30, 6);
		
		// This is an approximation
		Double expectedAverage = 6.791666667;
		String message = "Got: " + Double.toString(morning.cloudCover) + ", expecting: " + expectedAverage;
		
		assertTrue(message, Math.abs(morning.cloudCover - expectedAverage) < 0.00001);
	}

	@Test
	public void testCalculateMorningWeatherHumidity() {
		
		DailyWeather daily = new DailyWeather();
		daily.weatherObservation[0] = daily.new ConcurrentObservations(3.0, 1000.0, 7, 8, 80, "N");
		daily.weatherObservation[1] = daily.new ConcurrentObservations(0.0, 1000.0, 7, 7, 70, "NE");
		daily.weatherObservation[2] = daily.new ConcurrentObservations(0.0, 1000.0, 7, 2, 10, "E");
		daily.weatherObservation[3] = daily.new ConcurrentObservations(5.0, 1000.0, 7, 1, 10, "SE");
		daily.weatherObservation[4] = daily.new ConcurrentObservations(10.0, 1000.0, 7, 1, 10, "S");
		
		MorningWeather morning = daily.calculateMorningWeather(02, 30, 6);
		
		// This is a ROUGH approximation
		Double expectedAverage = 40.1;
		String message = "Got: " + Double.toString(morning.humidity) + ", expecting: " + expectedAverage;
		
		assertTrue(message, Math.abs(morning.humidity - expectedAverage) < 0.1);
	}

	@Test
	public void testgetDayLength() {
		
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.set( 2014, 3-1, 24 );		// month is 0-based !!
		
		WeatherData weatherTest = new WeatherData();
		DayLength dayLen = weatherTest.getDayLength(cal);
		
		Integer sunrise = dayLen.sunriseH * 60 + dayLen.sunriseMin;
		Integer sunset = dayLen.sunsetH * 60 + dayLen.sunsetMin;

		Integer exp_sunrise = 4 * 60 + 10;		// Using UTC
		Integer exp_sunset = 16 * 60 + 44;

		// Don't know _exact_ values for Hanko.		
		assertTrue("Got: " + sunrise.toString() + ", expecting: " + exp_sunrise.toString(), Math.abs(sunrise - exp_sunrise) < 20);
		assertTrue("Got: " + sunset.toString() + ", expecting: " + exp_sunset.toString(), Math.abs(sunset - exp_sunset) < 20);
	}

	@Test
	public void testgetDayLengthSummertime() {
		
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.set( 2014, 6-1, 24 );		// month is 0-based !!
		
		WeatherData weatherTest = new WeatherData();
		DayLength dayLen = weatherTest.getDayLength(cal);
		
		Integer sunrise = dayLen.sunriseH * 60 + dayLen.sunriseMin;
		Integer sunset = dayLen.sunsetH * 60 + dayLen.sunsetMin;

		Integer exp_sunrise = 0 * 60 + 55;		// Using UTC
		Integer exp_sunset = 19 * 60 + 50;

		// Don't know _exact_ values for Hanko.		
		assertTrue("Got: " + sunrise.toString() + ", expecting: " + exp_sunrise.toString(), Math.abs(sunrise - exp_sunrise) < 20);
		assertTrue("Got: " + sunset.toString() + ", expecting: " + exp_sunset.toString(), Math.abs(sunset - exp_sunset) < 20);
	}

	@Test
	public void testHaliasDataProcessor() {
		
		final String[] expectedSpecies = {"tylli@fi", "haahka@fi", "punajalkaviklo@fi", "lehtokurppa@fi", "kyhmyjoutsen@fi"};
		final HashMap<String, String> expectedHumi = new HashMap<String, String>( 4 );
		final HashMap<String, String> expectedTempDay = new HashMap<String, String>( 4 );
		
		final String[] expectedWindsDay1 = {"http://ldf.fi/halias/observations/winds/windN3", 
											 "http://ldf.fi/halias/observations/winds/windE4", 
											 "http://ldf.fi/halias/observations/winds/windNE4",
											 "http://ldf.fi/halias/observations/winds/windNE5"};
		final String[] expectedWindsDay2 = {"http://ldf.fi/halias/observations/winds/windNE10",
											 "http://ldf.fi/halias/observations/winds/windN10"};

		final String[] expectedStandardWinds1 = {"http://ldf.fi/halias/observations/winds/windN3", 
		  										  "http://ldf.fi/halias/observations/winds/windE4"};
		final String[] expectedStandardWinds2 = {};
		final String[] expectedStandardWinds3 = {"http://ldf.fi/halias/observations/winds/windNE9", 
												  "http://ldf.fi/halias/observations/winds/windNE10"};

		final HashMap<String, String> expectedStandardTemperature = new HashMap<String, String>( 4 );
		final HashMap<String, String> expectedStandardCloudCover = new HashMap<String, String>( 4 );

		// Calculated by hand from data file
		expectedHumi.put("1979-05-01", "85.0");
		expectedHumi.put("1979-05-02", "74.0");
		expectedHumi.put("1979-05-03", "79.0");
		expectedHumi.put("1979-05-04", "78.0");

		expectedTempDay.put("1979-05-01", "-16.0");
		expectedTempDay.put("1979-05-02", "-16.0");
		expectedTempDay.put("1979-05-03", "-13.0");
		expectedTempDay.put("1979-05-04", "-12.0");
		
		expectedStandardTemperature.put("1979-05-01", "-15.0");
		expectedStandardTemperature.put("1979-05-02", "-18.0");
		expectedStandardTemperature.put("1979-05-03", "-13.0");
		expectedStandardTemperature.put("1979-05-04", "-12.0");

//		expectedStandardTemperature.put("1979-05-01", "-15.0");
//		expectedStandardTemperature.put("1979-05-02", "-18.0");
//		expectedStandardTemperature.put("1979-05-03", "-13.0");
//		expectedStandardTemperature.put("1979-05-04", "-12.0");

		HaliasDataProcessor haliasConverter = new HaliasDataProcessor();

		haliasConverter.readTaxonOntologies();
		haliasConverter.readCoreOntologies();
		haliasConverter.readConservationStatuses();
		haliasConverter.weatherRussaro.readWeatherCSV("../test_fixtures/test_weather.csv", "../test_fixtures/test_rainfall.csv");
		
//		haliasConverter.readStandardizedObservations();
//		
		//haliasConverter.addVernacularNames();
		haliasConverter.labelTaxons();
		
		Model obs = haliasConverter.observationOntology;
		Model taxa = haliasConverter.taxonOntology;

		obs.read("file:" + "../test_fixtures/test_observations.rdf", "RDF/XML");

		obs.setNsPrefix("xsd", XSD.getURI());
		obs.setNsPrefix("dgui", HaliasDataProcessor.NS_DGU_INTERVALS);
		obs.setNsPrefix("halias", HaliasDataProcessor.NS_HALIAS_OBSERVATIONS);
		obs.removeNsPrefix( "halias-schema" );
		obs.setNsPrefix("hs", HaliasDataProcessor.NS_HALIAS_SCHEMA);
		obs.setNsPrefix("bc", HaliasDataProcessor.NS_BIRD_CHARACTERISTICS);

		haliasConverter.addLabelsToObservations();
		haliasConverter.processBirdCounts();
		haliasConverter.addDateInformation();

        haliasConverter.createWinds();
        haliasConverter.createHaliasWeatherCube();
        haliasConverter.createRussaroCube();

		ResIterator iter = obs.listResourcesWithProperty(RDF.type, obs.createResource(HaliasDataProcessor.NS_QB + "Observation"));
		
		while (iter.hasNext()) {
		    Resource r = iter.nextResource();
		    
		    RDFNode date_node = obs.getProperty(r, obs.createProperty(HaliasDataProcessor.NS_HALIAS_SCHEMA, "refTime")).getObject();
            String date = date_node.asLiteral().getString();

		    // CHECK TAXA
		    
		    Resource taxon = (Resource) obs.listObjectsOfProperty(r, obs.createProperty(HaliasDataProcessor.NS_HALIAS_SCHEMA, "observedSpecies")).next();
		    String taxonString = "";
		    
		    for (NodeIterator i = taxa.listObjectsOfProperty(taxon.asResource(), RDFS.label); i.hasNext();) {
				RDFNode item = i.next();
				if (item.asLiteral().getLanguage().equals("fi"))
					taxonString = item.asLiteral().toString();
			}
		    
		    assertTrue(taxonString, Arrays.asList(expectedSpecies).contains(taxonString));

		    // CHECK HUMIDITIES

            Resource weather_resource = haliasConverter.hWC.listResourcesWithProperty(haliasConverter.hWC.createProperty(HaliasDataProcessor.NS_HALIAS_SCHEMA, "refTime"), date_node).next();

            RDFNode humi_node = haliasConverter.hWC.listObjectsOfProperty(weather_resource, obs.createProperty(HaliasDataProcessor.NS_HALIAS_SCHEMA, "humidity")).next();
            System.out.println(humi_node);
            String humi = humi_node.asLiteral().getString();
		    assertTrue(date, expectedHumi.get(date).equals(humi));

		    // CHECK TEMPERATURES

            RDFNode temp_node = haliasConverter.hWC.listObjectsOfProperty(weather_resource, obs.createProperty(HaliasDataProcessor.NS_HALIAS_SCHEMA, "temperatureDay")).next();
		    String temp = temp_node.asLiteral().getString();
		    assertTrue(date, expectedTempDay.get(date).equals(temp));

            RDFNode standard_temp_node = haliasConverter.hWC.listObjectsOfProperty(weather_resource, obs.createProperty(HaliasDataProcessor.NS_HALIAS_SCHEMA, "standardTemperature")).next();
		    assertNotNull("No standardTemperature for " + date, standard_temp_node);

		    String standardTemp = standard_temp_node.asLiteral().getString();
		    assertTrue(date + ": " + standardTemp + " - expected " + expectedStandardTemperature.get(date), expectedStandardTemperature.get(date).equals(standardTemp));
		    
		    // CHECK WINDS

		    for (NodeIterator i = obs.listObjectsOfProperty(r, obs.createProperty(HaliasDataProcessor.NS_HALIAS_SCHEMA, "windDay")); i.hasNext();) {
				RDFNode item = i.next();
		    	//System.out.println(date + " - " + item.toString());
			    if (date.equals("1979-05-01")) {
			    	assertTrue(date + ": " + item.toString(), Arrays.asList(expectedWindsDay1).contains(item.toString()));
			    }
			    if (date.equals("1979-05-02")) {
			    	assertTrue(date, Arrays.asList(expectedWindsDay2).contains(item.toString()));
			    }
			}
		    
		    for (NodeIterator i = obs.listObjectsOfProperty(r, obs.createProperty(HaliasDataProcessor.NS_HALIAS_SCHEMA, "standardWind")); i.hasNext();) {
				RDFNode item = i.next();
//		    	System.out.println(date + " - " + item.toString());
			    if (date.equals("1979-05-01")) {
			    	assertTrue(date, Arrays.asList(expectedStandardWinds1).contains(item.toString()));
			    }
			    if (date.equals("1979-05-02")) {
			    	assertTrue(date + ": " + item.toString(), Arrays.asList(expectedStandardWinds2).contains(item.toString()));
			    }
			}
		}
	}

	
	@Test
	public void testReadWeatherCSV() {
		
		final HashMap<String, String> expectedhumiN = new HashMap<String, String>( 4 );
		
		final String[] _expectedWind1 = {"windNW3", 
										  "windN3", 
										  "windE4", 
										  "windNE4", 
										  "windE4",
										  "windE4",
										  "windNE5",
										  null};		
		final String[] _expectedWind2 = {null, 
										  null, 
										  null, 
										  "windNE10", 
										  null,
										  "windN10",
										  "windNE10",
										  "windNE9"};
		final String[] _expectedWind3 = {"windNE9", 
										  "windNE10", 
										  "windNE9",
										  "windNE9",
										  "windNE8",
										  "windE10",
										  "windE11",
										  "windSE9",
										  };

		
		final HashMap<String, String[]> expectedWinds = new HashMap<String, String[]>( 4 );
		
		expectedWinds.put("1979-05-01", _expectedWind1);
		expectedWinds.put("1979-05-02", _expectedWind2);
		expectedWinds.put("1979-05-03", _expectedWind3);

		expectedhumiN.put("1979-05-01", "7");
		expectedhumiN.put("1979-05-02", "7");
		expectedhumiN.put("1979-05-03", "3");
		expectedhumiN.put("1979-05-04", "8");
		
		HaliasDataProcessor haliasConverter = new HaliasDataProcessor();

		haliasConverter.weatherRussaro.readWeatherCSV("../test_fixtures/test_weather.csv", "../test_fixtures/test_rainfall.csv");

    	for (Map.Entry<String, String> entry: expectedhumiN.entrySet()) {
    		assertEquals(entry.getValue(), haliasConverter.weatherRussaro.getDailyWeatherData(entry.getKey()).humidityN.toString());
    	}
    	
    	for (Map.Entry<String, String[]> entry: expectedWinds.entrySet()) {
    		String[] winds = entry.getValue();
    		
			for(int i=0; i < winds.length; i++ ) {
				
				WindInstance observedWind = null;
				
				if (i == 0)
					try {
						observedWind = haliasConverter.weatherRussaro.getDailyWeatherData(entry.getKey()).windsPreSunrise.get(i);
					} catch (Exception e) {
						// Do nothing
					}
				else if (i <= 6)
					observedWind = haliasConverter.weatherRussaro.getDailyWeatherData(entry.getKey()).windsDay.get(i - 1);
					//observedWind = haliasConverter.weatherRussaro.getDailyWeatherData(entry.getKey()).weatherObservation[i].wind;
				else if (i == 7)
					try {
						observedWind = haliasConverter.weatherRussaro.getDailyWeatherData(entry.getKey()).windsPostSunset.get(0);
					} catch (Exception e) {
						// Do nothing
					}
				
//				System.out.println("i: " + i);
//				System.out.println(Arrays.toString(haliasConverter.weatherRussaro.getDailyWeatherData(entry.getKey()).windsDay.toArray()));
				if (observedWind != null) {
					//System.out.println(winds[i].toString());
					assertEquals(winds[i], observedWind.toString());
				}
				else
					assertNull(winds[i], winds[i]);
			}
    	}
    	
//		assertTrue(message, Math.abs(morning.humidity - expectedAverage) < 0.1);
	}


}

