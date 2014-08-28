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

import java.util.ArrayList;
import java.util.List;

/**
 * Class for handling a single day's weather data of a single place of observation.
 */
public class DailyWeather {
	
	public Double tempDaySum;
	public Integer tempDayN;
	//public Double tempNightSum;
	//public Integer tempNightN;
	public List<WindInstance> windsDay, windsPreSunrise, windsPostSunset;
	public Double pressureSum, pressureN, rainfall;
	public Integer cloudCoverDaySum, cloudCoverDayN;
	public Integer cloudCoverNightSum, cloudCoverNightN;
	public Integer humiditySum, humidityN;
	public ConcurrentObservations[] weatherObservation;		// 0 = 00:00, 1 = 03:00, 2 = 06:00, 3 = 09:00, 4 = 12:00, ...
	public MorningWeather morningStandardWeather;

	public static class WindInstance {
		public Integer speed;		
		public String dir;		
		public WindInstance(Integer spd, String dir) {
			this.speed = spd;
			this.dir = dir;
		}
		public String toString() {
			return "wind" + this.dir + this.speed.toString();
		}
	}
	
	/**
	 * Class of weather observations at a time instant.
	 */
	public class ConcurrentObservations {
		
		public Double temperature, pressure;
		public Integer cloudCover, humidity;
		public WindInstance wind;

		public ConcurrentObservations(Double temp, Double pres, Integer wSpd, Integer cloud, Integer humi, String wDir) {
			this.temperature = temp;
			this.pressure = pres;
			this.cloudCover = cloud;
			this.humidity = humi;
			this.wind = new WindInstance(wSpd, wDir);
		}

		public ConcurrentObservations(Double temp, Double pres, Integer cloud, Integer humi) {
			this.temperature = temp;
			this.pressure = pres;
			this.cloudCover = cloud;
			this.humidity = humi;
		}
		
		public ConcurrentObservations() {
			this(null, null, null, null);
		}
	}

	/**
	 * Standardized observation time weather.
	 */
	public class MorningWeather {
		public Double temperature, pressure;
		public Double cloudCover, humidity;
//		public ArrayList<String> windDirections;
		public ArrayList<WindInstance> winds;
		
		public MorningWeather() {
			winds = new ArrayList<WindInstance>();
		}
	}
	
	/**
	 * Initialize values.
	 */
	public DailyWeather() {
		
		tempDaySum = 0.0;
		tempDayN = 0;
		windsDay = new ArrayList<WindInstance>();
		windsPreSunrise = new ArrayList<WindInstance>();
		windsPostSunset = new ArrayList<WindInstance>();
		rainfall = null;
		humiditySum = 0;
		humidityN = 0;
		pressureSum = 0.0;
		pressureN = 0.0;
		cloudCoverDaySum = 0;
		cloudCoverDayN = 0;
		weatherObservation = new ConcurrentObservations[ 8 ];
		for (Integer i=0; i < weatherObservation.length; i++) {
			weatherObservation[ i ] = new ConcurrentObservations();
		}
		morningStandardWeather = new MorningWeather();
	}

	/**
	 * 	 *	Calculate average of morning observations by integrating over minutes of the 4 hour period. 
	 *	Values between measurement points are expected to follow linear interpolation of closest known measurements.
	 * 	
	 * @param	measurements	Reindexed weather observations, starting from the first eligible observation.
	 * @param	minutes0		Minutes offset from first measurement (0-179).
	 * 
	 * @return	average of values according to minutes offset from first measurement.
	 */
	public Double _calculateMeasurementAverage( Double[] measurements, Integer minutes0 )
	{
		Double tempIntegral = 0.0;
		Double tempStart, tempEnd;
		
		// Integrate backwards from index0+1 because index0 may not be in our scope.
		tempEnd = measurements[1];
		tempStart = tempEnd + ( measurements[0] - tempEnd ) / 180.0 * (180-minutes0);
		
		tempIntegral += ((tempStart + tempEnd) / 2) * (180-minutes0);
		
		if ( minutes0 > 120 ) 
		{
			// 2 weather observations in standardized observation time
			Integer minutes1 = minutes0 - 120;

			tempIntegral += ( measurements[1] + measurements[2] ) / 2 * 180;

			tempStart = measurements[2];
			tempEnd = tempStart + (measurements[3] - tempStart ) / 180.0 * minutes1;
			tempIntegral += ((tempStart + tempEnd) / 2) * minutes1;
		}
		else 
		{
			// 1 weather observation in standardized observation time
			Integer minutes1 = minutes0 + 60;
			
			tempStart = measurements[1];
			tempEnd = tempStart + (measurements[2] - tempStart ) / 180.0 * minutes1;
			tempIntegral += ((tempStart + tempEnd) / 2) * minutes1;
		}
		
		return tempIntegral / 240;
	}

	
	public Double[] _checkAndReindexMeasurements(Integer index0, Integer minutes0, Double[] measurements) {
		int hour = index0 * 3 + minutes0 / 60;
		
		if (	(index0 > 0 || measurements[0] != null) &&
				(index0 > 1 || measurements[1] != null) &&
				measurements[2] != null &&
				(hour < 2 || measurements[3] != null) &&
				(hour < 5 || measurements[4] != null)) 
		{
			Integer numOfMeasurements;
			if (index0 < 2)
				numOfMeasurements = 4;
			else
				numOfMeasurements = 3;
			
			Double[] eligibleMeasurements = new Double[numOfMeasurements];
			eligibleMeasurements[0] = measurements[index0];
			eligibleMeasurements[1] = measurements[index0 + 1];
			eligibleMeasurements[2] = measurements[index0 + 2];
			
			if (numOfMeasurements == 4) {
				eligibleMeasurements[3] = measurements[index0 + 3];
			}
			return eligibleMeasurements;
		}
		else
		{
			return null;
		}
	}
	

	/**
	 * Calculate average weather measurements of arbitrary 4 hour period beginning from 03:00 to 11:00.
	 * Used for calculating weather for standardized observation time.
	 * 
	 * @param	startTimeHour		starting time hour, must be something from 3 to 11.
	 * @param	startTimeMinute		starting time minutes (0-59).
	 * 
	 * @return	Averages of given 4 hour period of morning
	 */
	public MorningWeather calculateMorningWeather( Integer startTimeHour, Integer startTimeMinute ) 
	{
		// Validate arguments
		if ((startTimeHour < 0 || startTimeHour > 8) ||
			(startTimeMinute < 0 || startTimeMinute > 59) ||
			(startTimeHour == 8 && startTimeMinute != 0))
			return null;
		
		MorningWeather averagedValues = new MorningWeather();
		
		Integer index0 = startTimeHour / 3;									// Starting observation index
		Integer minutes0 = ( startTimeHour % 3 ) * 60 + startTimeMinute;	// Minutes from index0 to observation start
		
		Double[] measurements = new Double[this.weatherObservation.length];
		Double[] eligibleMeasurements;
		
		// Temperature
		for (Integer x = 0; x < this.weatherObservation.length; x++) {
			measurements[x] = this.weatherObservation[x].temperature;
		}

		eligibleMeasurements = _checkAndReindexMeasurements(index0, minutes0, measurements);
		if (eligibleMeasurements != null)
			averagedValues.temperature = _calculateMeasurementAverage(eligibleMeasurements, minutes0);
		
		// Pressure
		for (Integer x = 0; x < this.weatherObservation.length; x++)
			measurements[x] = this.weatherObservation[x].pressure;
		
		eligibleMeasurements = _checkAndReindexMeasurements(index0, minutes0, measurements);
		if (eligibleMeasurements != null)
			averagedValues.pressure = _calculateMeasurementAverage(eligibleMeasurements, minutes0);
		
		// Winds		
		if (minutes0 == 0 && this.weatherObservation[index0].wind != null) {
			//System.out.println(this.weatherObservation[index0].wind.toString());
			averagedValues.winds.add(this.weatherObservation[index0].wind);
		}
		if (this.weatherObservation[index0 + 1].wind != null)
			//System.out.println(this.weatherObservation[index0 + 1].wind.toString());
			averagedValues.winds.add(this.weatherObservation[index0 + 1].wind);

		if (minutes0 >= 120 && this.weatherObservation[index0 + 2].wind != null) {
			//System.out.println(this.weatherObservation[index0 + 2].wind.toString());
			averagedValues.winds.add(this.weatherObservation[index0 + 2].wind);
		}

		// Cloud cover
		for (Integer x = 0; x < this.weatherObservation.length; x++) {
			if (this.weatherObservation[x].cloudCover != null)
				measurements[x] = (double) this.weatherObservation[x].cloudCover;
			else
				measurements[x] = null;
		}
		eligibleMeasurements = _checkAndReindexMeasurements(index0, minutes0, measurements);
		if (eligibleMeasurements != null)
			averagedValues.cloudCover = _calculateMeasurementAverage(eligibleMeasurements, minutes0);
		
		// Humidity
		for (Integer x = 0; x < this.weatherObservation.length; x++) {
			if (this.weatherObservation[x].humidity != null)
				measurements[x] = (double) this.weatherObservation[x].humidity;
			else
				measurements[x] = null;
		}
		eligibleMeasurements = _checkAndReindexMeasurements(index0, minutes0, measurements);
		if (eligibleMeasurements != null)
			averagedValues.humidity = _calculateMeasurementAverage(eligibleMeasurements, minutes0);
		
		return averagedValues;
	}
}
