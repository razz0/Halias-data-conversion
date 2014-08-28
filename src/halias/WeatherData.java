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

import halias.DailyWeather.WindInstance;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

/**
 * Class for handling weather data with HALIAS-observations
 * 
 * Static variables and methods for handling all of the weather data.
 */
public class WeatherData {
	
	private HashMap<String, DailyWeather> dailyWeathers;
	public Set<WindInstance> windInstances = new HashSet<WindInstance>();

	private static final Integer _PRESUNRISE = 0;
	private static final Integer _DAY = 1;
	private static final Integer _POSTSUNSET = 2;

	public static HashMap<Integer, String> windDirections;

	static {
		windDirections = new HashMap<Integer, String>(37);
		
		windDirections.put(0, "N");
		windDirections.put(10, "N");
		windDirections.put(20, "N");
		windDirections.put(30, "NE");
		windDirections.put(40, "NE");
		windDirections.put(50, "NE");
		windDirections.put(60, "NE");
		windDirections.put(70, "E");
		windDirections.put(80, "E");
		windDirections.put(90, "E");
		windDirections.put(100, "E");
		windDirections.put(110, "E");
		windDirections.put(120, "SE");
		windDirections.put(130, "SE");
		windDirections.put(140, "SE");
		windDirections.put(150, "SE");
		windDirections.put(160, "S");
		windDirections.put(170, "S");
		windDirections.put(180, "S");
		windDirections.put(190, "S");
		windDirections.put(200, "S");
		windDirections.put(210, "SW");
		windDirections.put(220, "SW");
		windDirections.put(230, "SW");
		windDirections.put(240, "SW");
		windDirections.put(250, "W");
		windDirections.put(260, "W");
		windDirections.put(270, "W");
		windDirections.put(280, "W");
		windDirections.put(290, "W");
		windDirections.put(300, "NW");
		windDirections.put(310, "NW");
		windDirections.put(320, "NW");
		windDirections.put(330, "NW");
		windDirections.put(340, "N");
		windDirections.put(350, "N");
		windDirections.put(360, "N");
	}
	
	public static String[] possibleWindDirections = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
	
	public class DayLength {
		public Integer sunriseH, sunriseMin;
		public Integer sunsetH, sunsetMin;

		public DayLength(Integer sunriseH, Integer sunriseMin, Integer sunsetH, Integer sunsetMin) {
			this.sunriseH = sunriseH;
			this.sunriseMin = sunriseMin;
			this.sunsetH = sunsetH;
			this.sunsetMin = sunsetMin;
		}
	}
	
	/**
	 * WeatherData constructor.
	 */
	public WeatherData() {
		dailyWeathers = new HashMap<String, DailyWeather>( 15000 );		
	}

	/**
	 * Get all weather data for one day.
	 * 
	 * @param	day_index	getter for {@link DailyWeather}
	 * @return	{@link DailyWeather}
	 */
	public DailyWeather getDailyWeatherData(String day_index) {
		return dailyWeathers.get( day_index );
	}


    /**
     * Get all weather data for all days.
     *
     * @return	{@link HashMap }
     */
    public HashMap<String, DailyWeather> getDailyWeathers() {
        return dailyWeathers;
    }


    /**
	 * Read weather data from files to class variable 'dailyWeathers'.
	 * 
	 * @param	weather_file_name	path to FMI's observation file
	 * @param	rain_file_name		path to FMI's rainfall observation file
	 */
	public void readWeatherCSV( String weather_file_name, String rain_file_name ) {
		InputStream		fis;
		BufferedReader	br;
		String			line;
		String[]		strArr;
		Integer			i = 0;
		
		String 			indx;
		Double 			temp, pres;
		Integer 		wind, clouds, humi;
		String 			windDir;
		
		DailyWeather	thisWeather;
		WindInstance	thisWind = null;

		try {
			fis = new FileInputStream( weather_file_name );
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			try {
				while ((line = br.readLine()) != null) {
					
					strArr=line.split(";");

					if ( i < 16) {
						// Skip header lines
						i++;
						continue;
					}
					
					if ((strArr[1].length()) == 1 )
						strArr[1] = "0" + strArr[1];
					if ((strArr[2].length()) == 1 )
						strArr[2] = "0" + strArr[2];
					
					indx = strArr[0] + "-" + strArr[1] + "-" + strArr[2];
					
					// Get sunrise and sunset times
					Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
					cal.set( Integer.parseInt(strArr[0]), Integer.parseInt(strArr[1]) - 1, Integer.parseInt(strArr[2]));
					
					DayLength dayLen = getDayLength(cal);
					
					if ( !dailyWeathers.containsKey( indx )) {
						dailyWeathers.put(indx, new DailyWeather());
					}

					thisWeather = dailyWeathers.get( indx );
					
					/* Air pressure */
					if ( strArr.length > 9 && strArr[9].length() > 0 ) {
						pres = Double.parseDouble( strArr[9] );
						
						thisWeather.pressureSum += pres;
						thisWeather.pressureN += 1;
					} else
						pres = null;
					
					/* Humidity */
					if ( strArr.length > 5 && strArr[ 5 ].length() > 0) {
						humi = Integer.parseInt( strArr[ 5 ] );
						
						thisWeather.humiditySum += humi;
						thisWeather.humidityN += 1;
					} else
						humi = null;
					
					/* Cloud cover */
					if ( strArr.length > 10 && strArr[ 10 ].length() > 0 ) {
						clouds = Integer.parseInt( strArr[ 10 ] );
						
						thisWeather.cloudCoverDaySum += clouds;
						thisWeather.cloudCoverDayN += 1;
					} else
						clouds = null;
					
					if ( strArr.length > 3 && strArr[ 3 ].length() > 0 ) {
						Integer hour = Integer.parseInt( strArr[ 3 ] );
						
						Integer timeOfDay;

						// Check if we have a pre-sunrise observation
						if (hour <= dayLen.sunriseH ) {
							timeOfDay = _PRESUNRISE;								
						// Check if we have a post-sunset observation
						} else if (hour > dayLen.sunsetH || (hour == dayLen.sunsetH && dayLen.sunsetMin == 0)) {
							timeOfDay = _POSTSUNSET;								
						} else {
							// We have a daytime observation
							timeOfDay = _DAY;								
						}
						
						/* Day and night temperature */
						if (( strArr[ 4 ].length()) > 0 ) {
							temp = Double.parseDouble( strArr[ 4 ] );
							
							// Check if we have a daytime observation (after or exactly at sunrise, before or exactly at sunset)
							if (timeOfDay == _DAY) {
								thisWeather.tempDaySum += temp;
								thisWeather.tempDayN += 1;
							}
						} else
							temp = null;
						
						/* Day and night wind speeds */
						if ( strArr.length > 7 && strArr[7].length() > 0) {
							wind = Integer.parseInt( strArr[7] );
						} else
							wind = null;

						/* Wind Directions */
						if ( strArr.length > 6 && strArr[6].length() > 0 ) {
							// Convert wind angle to cardinal or half-cardinal direction
							windDir = WeatherData.windDirections.get( Integer.parseInt( strArr[6] ));
						} else
							windDir = null;

						if (wind != null && windDir != null) {
							thisWind = new WindInstance(wind, windDir);
							windInstances.add(thisWind);
						} else
							thisWind = null;

						if (timeOfDay == _PRESUNRISE) {
							thisWeather.windsPreSunrise.add(thisWind);
						} else if (timeOfDay == _POSTSUNSET) {
							thisWeather.windsPostSunset.add(thisWind);
						} else {
							thisWeather.windsDay.add(thisWind);
						}
						
						/* Memorize single observations */
                        hour = ( hour / 3 );

                        thisWeather.weatherObservation[ hour ].temperature = temp;
                        thisWeather.weatherObservation[ hour ].humidity = humi;
                        thisWeather.weatherObservation[ hour ].pressure = pres;
                        thisWeather.weatherObservation[ hour ].cloudCover = clouds;
                        if (thisWind != null)
                            thisWeather.weatherObservation[ hour ].wind = thisWind;
					}
					i++;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		i = 0;
		Double rain;
		
		/* Get daily rainfall */
		try {
			fis = new FileInputStream(rain_file_name);
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			try {
				while ((line = br.readLine()) != null) {
					strArr=line.split(";");

					if ( i > 7) {
						if (( strArr[1].length() )== 1 )
							strArr[1] = "0" + strArr[1];
						if (( strArr[2].length() )== 1 )
							strArr[2] = "0" + strArr[2];
						
						indx = strArr[ 0 ] + "-" + strArr[ 1 ] + "-" + strArr[ 2 ]; 

						if ( !dailyWeathers.containsKey( indx )) {
							dailyWeathers.put(indx, new DailyWeather());
						}
						
						/* Rainfall */
						if ( strArr.length > 3 && strArr[ 3 ].length() > 0 ) {
							rain = Double.parseDouble( strArr[ 3 ] );
							
							dailyWeathers.get( indx ).rainfall = rain;
						}
					}
					i++;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}	
	}

    /**
         *
         * @param date date for which to generate sunrise and sunset times
         * @return sunrise and sunset times in UTC (!!)
         */
	public DayLength getDayLength(Calendar date){
		Location hanko = new Location(59.81021528, 22.89485922);
    	SunriseSunsetCalculator separi = new SunriseSunsetCalculator(hanko, "UTC");
    	
    	String sunRise = separi.getOfficialSunriseForDate( date );
    	String sunSet = separi.getOfficialSunsetForDate( date );
		
		Integer sunriseH = Integer.parseInt( sunRise.split(":")[ 0 ] );
		Integer sunriseMIN = Integer.parseInt( sunRise.split(":")[ 1 ] );
		
		Integer sunsetH = Integer.parseInt( sunSet.split(":")[ 0 ] );
		Integer sunsetMIN = Integer.parseInt( sunSet.split(":")[ 1 ] );
		
		//System.out.println( "------ " + sunriseH + ":" + sunriseMIN + " -------" + sunsetH + ":" + sunsetMIN );
		
		return new DayLength(sunriseH, sunriseMIN, sunsetH, sunsetMIN);
		
	}
}
