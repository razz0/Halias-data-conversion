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

import halias.DailyWeather.MorningWeather;
import halias.DailyWeather.WindInstance;
import halias.WeatherData.DayLength;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * Class for processing RDF files provided by Python conversion program.
 * Adds weather data, standardized observations and other stuff. All Graph processing involved is done within this class.
 */
public class HaliasDataProcessor {
	
	final static String PYTHON_OUPUT_DIRECTORY = "/home/mkoho/HALIAS/PythonOutput/";
	final static String DEFAULT_INPUT_DIRECTORY = "/home/mkoho/HALIAS/InputFiles/";
	final static String DATACUBE_ONTOLOGY_FILE = "/home/mkoho/HALIAS/InputFiles/halias.ttl";
	final static String WEATHER_DATA_DIRECTORY = "/home/mkoho/HALIAS/RussaroWeather/";
	final static String OUTPUT_DIRECTORY = "/home/mkoho/HALIAS/JavaOutput/";
	
	final static String NS_QB = "http://purl.org/linked-data/cube#";
	final static String NS_TAXMEON = "http://www.yso.fi/onto/taxmeon/";
	final static String NS_DWC = "http://rs.tdwg.org/dwc/terms/";
	final static String NS_OWL = "http://www.w3.org/2002/07/owl#";
	final static String NS_BIO = "http://www.yso.fi/onto/bio/";
	final static String NS_BIRD_CHARACTERISTICS = "http://ldf.fi/halias/bird-characteristics/";
	final static String NS_HALIAS_OBSERVATIONS = "http://ldf.fi/halias/observations/birds/";
    final static String NS_HALIAS_WEATHER = "http://ldf.fi/halias/observations/weather/";
    final static String NS_RUSSAROCUBE = "http://ldf.fi/russaro/";
	final static String NS_WINDS = "http://ldf.fi/halias/observations/winds/";
	final static String NS_HALIAS_SCHEMA = "http://ldf.fi/schema/halias/";
	final static String NS_DGU_INTERVALS = "http://reference.data.gov.uk/def/intervals/";
	final static String NS_SKOS = "http://www.w3.org/2004/02/skos/core#";

    final static String NS_SDMX_A = "http://purl.org/linked-data/sdmx/2009/attribute#";

    public Model observationOntology;
	public Model speciesCharacteristicsOntology;
	public Model characteristicsOntology;
	public Model taxonOntology;
	public Model haliasSchema;
	public Model windInstances;
    public Model rWC;       // Russarö Original Weather Cube
    public Model hWC;       // Halias Weather Cube

//    HashMap<String, String> conservationStatuses;
//	HashMap<String, String> directiveSpecies;
	HashMap<String, String> speciesAbbreviations;
	HashMap<String, Integer> standardizedObservations;
	//Set<String> observationlessDays;

	// These could be used for validating from vakio-file, but seems that the file has the same duplicates...
	// Set newLocalObservations;
	// Set newMigrationObservations;
	
	WeatherData weatherRussaro;
	
	HaliasValidator validator;

	public HaliasDataProcessor() {
		observationOntology = ModelFactory.createDefaultModel();
		speciesCharacteristicsOntology = ModelFactory.createDefaultModel();
		characteristicsOntology = ModelFactory.createDefaultModel();
		taxonOntology = ModelFactory.createDefaultModel();
		haliasSchema = ModelFactory.createDefaultModel();
		windInstances = ModelFactory.createDefaultModel();
        rWC = ModelFactory.createDefaultModel();
        hWC = ModelFactory.createDefaultModel();
//		conservationStatuses = new HashMap<String, String>( 90 );
//		directiveSpecies = new HashMap<String, String>( 64 );
		speciesAbbreviations = new HashMap<String, String>( 90 );
		standardizedObservations = new HashMap<String, Integer>( 450000 );

		weatherRussaro = new WeatherData();
		validator = new HaliasValidator();
		
		// Create a set of all days and later pop observation days out		
//		observationlessDays = new TreeSet();
//		
//		LocalDate startDate = new LocalDate(ISODateTimeFormat.dateTimeParser().parseDateTime("1979-01-01"));
//		LocalDate endDate = new LocalDate(ISODateTimeFormat.dateTimeParser().parseDateTime("2008-12-31"));
//		for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1))
//		{
//		    observationlessDays.add(date.toString());
//		}
	}

	
	/** 
	 * Read all taxonomy and related data to graphs 
	 */
	public void readTaxonOntologies() {
		//taxonOntology.read("file:" + DEFAULT_INPUT_DIRECTORY + "lintuset_HALIAS.ttl", "TTL");
		taxonOntology.read("file:" + PYTHON_OUPUT_DIRECTORY + "halias_taxa_v2.ttl", "TTL");
		
		//taxonOntology.read("file:" + DEFAULT_INPUT_DIRECTORY + "lisataksonit.ttl", "TTL");
	}
	
	
	/** 
	 * Read all core ontology data 
	 */
	public void readCoreOntologies() {
		speciesCharacteristicsOntology.read("file:" + DEFAULT_INPUT_DIRECTORY + "tuntomerkit.ttl", "TTL");
		characteristicsOntology.read("file:" + DEFAULT_INPUT_DIRECTORY + "tuntomerkki_facet_halias.ttl", "TTL");
		
		//haliasSchema.read("file:" + DEFAULT_INPUT_DIRECTORY + "skeema_halias.ttl", "TTL");
		haliasSchema.read("file:" + DEFAULT_INPUT_DIRECTORY + "skeema_saa.ttl", "TTL");
		haliasSchema.read("file:" + DATACUBE_ONTOLOGY_FILE, "TTL");
	}
	
	
	/** 
	 * Add species characteristics to taxon ontology 
	 */
	public void addSpeciesCharacteristics() {
		
//		ResIterator iter = observationOntology.listResourcesWithProperty(RDF.type, observationOntology.createResource(NS_QB + "Observation"));
//		
//		while (iter.hasNext()) {
//		    Resource r = iter.nextResource();
//		    
//		    Resource id = (Resource) observationOntology.listObjectsOfProperty(r, observationOntology.createProperty(NS_HALIAS_SCHEMA, "observedSpecies")).next();
//			
//		    //Resource new_id = taxonOntology.createResource( NAMESPACE_BIO + id.getLocalName());
//		    //System.out.println(new_id);
//
//		    NodeIterator iterTuntom = speciesCharacteristicsOntology.listObjectsOfProperty(id, RDF.type);
//
//			while (iterTuntom.hasNext()) {
//				RDFNode tunto = iterTuntom.next();
//				
//				observationOntology.add(r, observationOntology.createProperty(NS_HALIAS_SCHEMA, "hasCharacteristic"), tunto);
//			}
//		}
		
		
		ResIterator iter = taxonOntology.listResourcesWithProperty( RDF.type, taxonOntology.createProperty(NS_TAXMEON, "TaxonInChecklist" ));
		
		while (iter.hasNext()) {
		    Resource r = iter.nextResource();

		    //Resource id = (Resource) observationOntology.listObjectsOfProperty(r, observationOntology.createProperty(NS_HALIAS_SCHEMA, "observedSpecies")).next();
			
		    //Resource new_id = taxonOntology.createResource( NAMESPACE_BIO + id.getLocalName());
		    //System.out.println(new_id);

		    NodeIterator iterTuntom = speciesCharacteristicsOntology.listObjectsOfProperty(r, RDF.type);

			while (iterTuntom.hasNext()) {
				RDFNode tunto = iterTuntom.next();
				
				taxonOntology.add(r, taxonOntology.createProperty(NS_HALIAS_SCHEMA, "hasCharacteristic"), tunto);
			}
		}
	}


	/** 
	 * Put vernacular name labels to taxa
	 */
	public void labelTaxons() {
		
		ResIterator iter = taxonOntology.listResourcesWithProperty( RDF.type, taxonOntology.createProperty(NS_TAXMEON, "TaxonInChecklist" ));
		
		while (iter.hasNext()) {
		    Resource id = iter.nextResource();
			ResIterator verns = taxonOntology.listResourcesWithProperty( taxonOntology.createProperty(NS_TAXMEON, "inverse_of_hasVernacularName"), id );

			/*if ( verns.toList().size() == 0 ) {
				verns = lintusetit.listResourcesWithProperty( lintusetit.createProperty(NAMESPACE_TAXMEON, "inverse_of_hasVernacularName"), id );
			}*/
			while ( verns.hasNext()) {
			    Resource vern = verns.nextResource();
			    RDFNode vernStr = taxonOntology.listObjectsOfProperty( vern, RDFS.label ).next();
			    taxonOntology.add( id, RDFS.label, taxonOntology.createLiteral( vernStr.asLiteral().getString(), vernStr.asLiteral().getLanguage()));
			}
        }
	}

	
	/** 
	 * Put vernacular name labels to observations 
	 */
	public void addLabelsToObservations() {
		ResIterator iter = observationOntology.listResourcesWithProperty( RDF.type, observationOntology.createResource(NS_QB + "Observation" ));
		
		RDFNode date;
		String dateStr;
		ResIterator verns;
		NodeIterator verns2;
		
		while (iter.hasNext()) {
		    Resource r = iter.nextResource();

		    Resource id = (Resource) observationOntology.listObjectsOfProperty(r, observationOntology.createProperty(NS_HALIAS_SCHEMA, "observedSpecies")).next();
			verns = taxonOntology.listResourcesWithProperty( taxonOntology.createProperty(NS_TAXMEON, "inverse_of_hasVernacularName"), id );

		    date = observationOntology.listObjectsOfProperty(r, observationOntology.createProperty(NS_HALIAS_SCHEMA, "refTime")).next();
		    dateStr = date.asLiteral().getString();		    

			if ( verns.hasNext() ) {
				
				if (observationOntology.getProperty(r, RDFS.label) != null)
                    observationOntology.remove( observationOntology.getProperty(r, RDFS.label));
				
			    while ( verns.hasNext()) {
				    Resource vern = verns.nextResource();
				    RDFNode vernStr = taxonOntology.listObjectsOfProperty( vern, RDFS.label ).next();
				    observationOntology.add( r, RDFS.label, taxonOntology.createLiteral( vernStr.asLiteral().getString() + ", " + dateStr, vernStr.asLiteral().getLanguage()));
				}
			} else {
				
				verns2 = taxonOntology.listObjectsOfProperty( id, RDFS.label );

				if ( verns2.hasNext() && observationOntology.getProperty(r, RDFS.label) != null)
                    observationOntology.remove( observationOntology.getProperty(r, RDFS.label) );
				
			    while ( verns2.hasNext()) {
			    	RDFNode vernStr = verns2.nextNode();
				    observationOntology.add( r, RDFS.label, taxonOntology.createLiteral( vernStr.asLiteral().getString() + ", " + dateStr, vernStr.asLiteral().getLanguage()));
				}
			}
		}
	}

	
	/** 
	 * Finalize characteristics ontology with SKOS:inScheme properties
	 */
	public void finalizeCharacteristicsOntology() {

		Resource conceptScheme = characteristicsOntology.listResourcesWithProperty(RDF.type, characteristicsOntology.createResource(NS_SKOS + "ConceptScheme")).next();
		
		ResIterator iter = characteristicsOntology.listResourcesWithProperty(RDF.type, characteristicsOntology.createResource(NS_SKOS + "Concept"));
		
		while (iter.hasNext()) {
		    Resource r = iter.nextResource();
		    
		    NodeIterator topConcept = characteristicsOntology.listObjectsOfProperty(r, characteristicsOntology.createProperty(NS_SKOS, "topConceptOf"));
			
		    if (!topConcept.hasNext()) {
		    	characteristicsOntology.add(r, characteristicsOntology.createProperty(NS_SKOS, "inScheme"), conceptScheme);
			}
		}
	}

	
	/** 
	 * Write observation ontology to file 
	 */
	public void writeFile( Model model, String file_name, String format ) {
		FileOutputStream fos;
		try {
			//fos = new FileOutputStream("HALIAS_FULL.ttl"); // write the file in any format e.g. TTL
			fos = new FileOutputStream( file_name ); // write the file in any format e.g. TTL
			model.write( fos, format ); // *TTL = TURTLE
		} catch ( Exception e ) {
			e.printStackTrace();
		} 
		
	}

	
	/** 
	 * Read conservation statuses and directive species and add them to taxon ontology.
	 */
	public void readConservationStatuses() {
		
		InputStream		fis;
		BufferedReader	br;
		String			line;
		String[]		strArr;

		try {
			fis = new FileInputStream(DEFAULT_INPUT_DIRECTORY + "suomen_uhanalaiset_HALIAS.csv");
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			try {
				while ((line = br.readLine()) != null) {
					strArr=line.split(",");
					String species = strArr[ 0 ].trim().toLowerCase();
					String status = strArr[ 1 ].trim();
					
					// ASSOSIOI URI JA UHANALAISUUS 

					ResIterator vernacularNames = taxonOntology.listResourcesWithProperty( taxonOntology.createProperty(NS_TAXMEON, "vernacularName"), taxonOntology.createLiteral( species ));					

					if (vernacularNames.hasNext()) {
						Resource vernacular = vernacularNames.next();					
						Resource taxon = taxonOntology.listResourcesWithProperty( taxonOntology.createProperty(NS_TAXMEON, "hasVernacularName"), vernacular ).next();
						taxonOntology.add(taxon, taxonOntology.createProperty(NS_HALIAS_SCHEMA, "hasConservationStatus2010"), taxonOntology.createResource(NS_HALIAS_SCHEMA + "conservationStatus" + status ));
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		try {
			fis = new FileInputStream(DEFAULT_INPUT_DIRECTORY + "lintudirektiivin_KAIKKI_lajit.csv");
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			try {
				while ((line = br.readLine()) != null) {
					String species = line.trim();

					// ASSOSIOI URI JA LINTUDIREKTIIVI 

					ResIterator taxon = taxonOntology.listResourcesWithProperty( RDFS.label, species );
					if ( taxon.hasNext() ) {
						taxonOntology.add(taxon.next(), taxonOntology.createProperty(NS_HALIAS_SCHEMA, "isDirectiveSpecies"), taxonOntology.createTypedLiteral( true ));
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	

	
	/** 
	 * Read standardized morning observations to memory 
	 */
	public void readStandardizedObservations() {
		
		InputStream		fis;
		BufferedReader	br;
		String			line;
		String[]		strArr;
		String 			indx;

		try {
			fis = new FileInputStream(DEFAULT_INPUT_DIRECTORY + "HALIAS_Kokodata_VAKIOT_2008asti.csv");
			br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			try {
				Integer i = 0;
				
				while ((line = br.readLine()) != null) {
					i++;
					if ( i == 1 )
						continue;
					
					strArr=line.split(";");

					// LISÄÄ VAKIOHAVAINNOT MUISTIIN 

					//Resource taxonURI = lintusetit.createProperty( lyhenteet.get( strArr[ 0 ] ));
					//model.add( taxonURI, model.createProperty(BASE_URI_PREFIX, "standardized_observation"), lintusetit.createProperty( lyhenteet.get( strArr[ 0 ] )) );

					//System.out.println( line );
					if ( (strArr.length > 4) && (strArr[ 4 ].length() > 0 )) {
						String[] temp = strArr[ 1 ].split("/");
						
						if (( temp[1].length() )== 1 )
							temp[1] = "0" + temp[1];
						if (( temp[0].length() )== 1 )
							temp[0] = "0" + temp[0];
						
						indx = temp[ 2 ] + "-" + temp[ 0 ] + "-" + temp[ 1 ];
						
						String taxon = strArr[ 0 ].trim().toLowerCase();
						Integer vakio = Integer.parseInt( strArr[ 4 ] );
						
						ResIterator iter = taxonOntology.listResourcesWithProperty( taxonOntology.createProperty(NS_HALIAS_SCHEMA, "abbreviation" ), taxon );
						
						if ( iter.hasNext() ) {
							RDFNode taxonNode = iter.next();
							//observationOntology.remove( observationOntology.getProperty(r,  observationOntology.createProperty(NS_HALIAS_SCHEMA, "countLocal")) );
							//observationOntology.add(r, observationOntology.createProperty(NS_HALIAS_SCHEMA, "countLocal"), observationOntology.createTypedLiteral( Integer.parseInt( loc )));
							
							standardizedObservations.put( indx + ";" + taxonNode.toString(), vakio);
							//System.out.println( indx + ";" + taxonNode.toString() );
							
							if (iter.hasNext()) {
								throw new RuntimeException("We should not have duplicate abbreviations.");
							}
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	}	


	/**
	 * Add standardized bird observation counts to graph and add literal types to bird counts.
	 */
	public void processBirdCounts() {
		
		ResIterator iter = observationOntology.listResourcesWithProperty(RDF.type, observationOntology.createResource(NS_QB + "Observation"));
		String dateString;
		
		String loc, mig, add;
		
		Integer total;
		
		// Iterate through all observations
		while (iter.hasNext()) {
		    
			Resource r = iter.nextResource();
		    RDFNode date = observationOntology.listObjectsOfProperty(r, observationOntology.createProperty(NS_HALIAS_SCHEMA, "refTime")).next();
			
		    dateString = date.asLiteral().getString();
		    
		    /** Add XSD type to bird counts */
		    NodeIterator local = observationOntology.listObjectsOfProperty(r, observationOntology.createProperty(NS_HALIAS_SCHEMA, "countLocal"));
		    NodeIterator migra = observationOntology.listObjectsOfProperty(r, observationOntology.createProperty(NS_HALIAS_SCHEMA, "countMigration"));
		    NodeIterator addit = observationOntology.listObjectsOfProperty(r, observationOntology.createProperty(NS_HALIAS_SCHEMA, "countAdditionalArea"));
		    
		    loc = "";
		    mig = "";
		    add = "";
		    
		    total = 0;
		    
			if ( local.hasNext() ) {
				loc = local.next().toString();
				if ( loc.charAt( 0 ) == '+' )
					loc = loc.substring(1);
				observationOntology.remove( observationOntology.getProperty(r,  observationOntology.createProperty(NS_HALIAS_SCHEMA, "countLocal")) );
				observationOntology.add(r, observationOntology.createProperty(NS_HALIAS_SCHEMA, "countLocal"), observationOntology.createTypedLiteral( Integer.parseInt( loc )));
				total += Integer.parseInt( loc );
			} else {
                observationOntology.add(r, observationOntology.createProperty(NS_HALIAS_SCHEMA, "countLocal"), observationOntology.createTypedLiteral( new Integer(0)));
            }
			if ( migra.hasNext() ) {
				mig = migra.next().toString();
				if ( mig.charAt( 0 ) == '+' )
					mig = mig.substring(1);
				observationOntology.remove( observationOntology.getProperty(r,  observationOntology.createProperty(NS_HALIAS_SCHEMA, "countMigration")) );
				observationOntology.add(r, observationOntology.createProperty(NS_HALIAS_SCHEMA, "countMigration"), observationOntology.createTypedLiteral( Integer.parseInt( mig )));
				total += Integer.parseInt( mig );
			} else {
                observationOntology.add(r, observationOntology.createProperty(NS_HALIAS_SCHEMA, "countMigration"), observationOntology.createTypedLiteral( new Integer(0)));
            }
			if ( addit.hasNext() ) {
				add = addit.next().toString().split("\\^\\^")[0];
				if ( add.charAt( 0 ) == '+' )
					add = add.substring(1);
				observationOntology.remove( observationOntology.getProperty(r,  observationOntology.createProperty(NS_HALIAS_SCHEMA, "countAdditionalArea")) );
				observationOntology.add(r, observationOntology.createProperty(NS_HALIAS_SCHEMA, "countAdditionalArea"), observationOntology.createTypedLiteral( Integer.parseInt( add )));
				total += Integer.parseInt( add );
			} else {
                observationOntology.add(r, observationOntology.createProperty(NS_HALIAS_SCHEMA, "countAdditionalArea"), observationOntology.createTypedLiteral( new Integer(0)));
            }
			observationOntology.add(r, observationOntology.createProperty(NS_HALIAS_SCHEMA, "countTotal"), observationOntology.createTypedLiteral( total ));
			
			/** ADD OBSERVED COUNT FOR STANDARDIZED OBSERVATION */
		    
		    RDFNode taxon = observationOntology.listObjectsOfProperty(r, observationOntology.createProperty(NS_HALIAS_SCHEMA, "observedSpecies")).next();
			
			//System.out.println( dateString + ";" + taxon.toString() );			
		    Integer vakio = standardizedObservations.get( dateString + ";" + taxon.toString() );

		    if ( vakio != null && vakio >= 0 ) {
		    	//String taxonString = keke.getPropertyResourceValue(RDFS.label).toString();
		    	String taxonString = "";
		    	
		    	for (NodeIterator i = taxonOntology.listObjectsOfProperty(taxon.asResource(), RDFS.label); i.hasNext();) {
					RDFNode item = i.next();
					if (item.asLiteral().getLanguage().equals(""))
						taxonString = item.asLiteral().toString();
					else if (item.asLiteral().getLanguage().equals("fi") && taxonString.equals(""))
						taxonString = item.asLiteral().toString();
				}
		    	//String taxonString = taxonOntology.listObjectsOfProperty(keke, RDFS.label);
				String v_error = validator.validateMigrationCounts(dateString, taxonString, mig, vakio);
                if (v_error.length() > 0) {
                    observationOntology.add(r, observationOntology.createProperty(NS_SDMX_A, "nonsamplingErr"), observationOntology.createLiteral(v_error));
                }
		    	observationOntology.add(r, observationOntology.createProperty(NS_HALIAS_SCHEMA, "countStandardizedMigration"), observationOntology.createTypedLiteral( vakio ));
		    } else {
                observationOntology.add(r, observationOntology.createProperty(NS_HALIAS_SCHEMA, "countStandardizedMigration"), observationOntology.createTypedLiteral( new Integer( 0 )));
            }
		    
		    /** Take date away from observationless days set */
		    // observationlessDays.remove(dateString);

		}
	}
		
		
	/**
	 * Add date and week and month numbers to observations. Also add information about Halias observation days to aggregated weather data graph.
	 */
	public void addDateInformation() {
		
		ResIterator iter = observationOntology.listResourcesWithProperty(RDF.type, observationOntology.createResource(NS_QB + "Observation"));
		Integer year, month, day;
		String indx;
		DailyWeather weather;
		
		DecimalFormat df = new DecimalFormat("#.0");
		
		while (iter.hasNext()) {
		    
			Resource r = iter.nextResource();
		    RDFNode date = observationOntology.listObjectsOfProperty(r, observationOntology.createProperty(NS_HALIAS_SCHEMA, "refTime")).next();
			
		    indx = date.asLiteral().getString();
		    /* Parse date */
		    year = Integer.parseInt( indx.split("-")[ 0 ] );
		    month = Integer.parseInt( indx.split("-")[ 1 ] );
		    day = Integer.parseInt( indx.split("-")[ 2 ] );


            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.set( year, month - 1, day );

//				DayLength dayLen = weatherRussaro.getDayLength(cal);
//
            /* Add week and month as dgu-intervals */
            //RDFNode week = observationOntology.listObjectsOfProperty(r, observationOntology.createProperty(NAMESPACE_DGU_INTERVALS, "ordinalWeekOfYear")).next();
            //observationOntology.remove( observationOntology.getProperty(r,  observationOntology.createProperty(NAMESPACE_DGU_INTERVALS, "ordinalWeekOfYear")) );

            observationOntology.add(r, observationOntology.createProperty( NS_HALIAS_SCHEMA, "weekOfYear" ), observationOntology.createTypedLiteral( (byte) cal.get(java.util.Calendar.WEEK_OF_YEAR )));
            observationOntology.add(r, observationOntology.createProperty( NS_HALIAS_SCHEMA, "monthOfYear" ), observationOntology.createTypedLiteral( month.byteValue() ));

            hWC.add(hWC.createResource(NS_HALIAS_WEATHER + indx.replaceAll("\\-", "")), hWC.createProperty(NS_HALIAS_SCHEMA, "haliasObservationDay"), hWC.createTypedLiteral(true));
		}
	}

	/**
	 * Create wind instances and save them to file.
	 */
	public void createWinds() {
		
    	for (Iterator<WindInstance> i = weatherRussaro.windInstances.iterator(); i.hasNext();) {
    		WindInstance wind = i.next();
    		Resource s = windInstances.createResource(NS_WINDS + wind);
    		Property p = RDF.type;
    		RDFNode o = windInstances.createResource(NS_HALIAS_SCHEMA + "WindObservation");
    		windInstances.add(s, p, o);

    		p = windInstances.createProperty(NS_HALIAS_SCHEMA, "windSpeed");
    		o = windInstances.createTypedLiteral(wind.speed);
    		windInstances.add(s, p, o);

    		p = windInstances.createProperty(NS_HALIAS_SCHEMA, "windDirection");
    		o = windInstances.createResource(NS_HALIAS_SCHEMA + "windDirection" + wind.dir);
    		windInstances.add(s, p, o);
    	}

        // Create instance for unknown wind
        Resource s = windInstances.createResource(NS_WINDS + "windUnknown");
        RDFNode o = windInstances.createResource(NS_HALIAS_SCHEMA + "WindObservation");
        windInstances.add(s, RDF.type, o);

        windInstances.add(s, RDFS.label, windInstances.createLiteral("Unknown wind", "en"));

        windInstances.setNsPrefix("hs", NS_HALIAS_SCHEMA);
    	windInstances.setNsPrefix("winds", NS_WINDS);
    	windInstances.setNsPrefix("xsd", XSD.getURI());
    	writeFile( windInstances, OUTPUT_DIRECTORY + "halias_wind_ontology.ttl", "TTL" );
	}


    /**
     * Get season resource from month number
     */
    private Resource getSeason(Integer month) {

        Resource season = null;
        if (month == 12 || month == 1 || month == 2)
            season = observationOntology.createResource(NS_HALIAS_SCHEMA + "winter");
        else if (month == 3 || month == 4 || month == 5)
            season = observationOntology.createResource(NS_HALIAS_SCHEMA + "spring");
        else if (month == 6 || month == 7 || month == 8)
            season = observationOntology.createResource(NS_HALIAS_SCHEMA + "summer");
        else if (month == 9 || month == 10 || month == 11)
            season = observationOntology.createResource(NS_HALIAS_SCHEMA + "autumn");

        return season;
    }


    /**
     * Create instances of daily aggregated weather observations and save them to file.
     */
    public void createHaliasWeatherCube() {
        Integer year, month, day;

        for (Map.Entry<String, DailyWeather> entry : weatherRussaro.getDailyWeathers().entrySet()) {
            DailyWeather weather = entry.getValue();
            String index = entry.getKey();

            year = Integer.parseInt( index.split("-")[ 0 ] );
            month = Integer.parseInt( index.split("-")[ 1 ] );
            day = Integer.parseInt( index.split("-")[ 2 ] );

            TimeZone tz = TimeZone.getTimeZone("UTC");
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            df.setTimeZone(tz);

            Calendar cal = Calendar.getInstance(tz);
            cal.set(year, month - 1, day);

            String dateISO = df.format(cal.getTime());

            DayLength dayLen = weatherRussaro.getDayLength(cal);

            // Pad with leading zeros
            String riseH = dayLen.sunriseH.toString();
            riseH = ("00" + riseH).substring(riseH.length());
            String riseM = dayLen.sunriseMin.toString();
            riseM = ("00" + riseM).substring(riseM.length());

            String setH = dayLen.sunsetH.toString();
            setH = ("00" + setH).substring(setH.length());
            String setM = dayLen.sunsetMin.toString();
            setM = ("00" + setM).substring(setM.length());

            String sunriseString = riseH + ":" + riseM + ":00";
            String sunsetString = setH + ":" + setM + ":00";

            Resource dayResource = hWC.createResource(NS_HALIAS_WEATHER + index.replaceAll("\\-", ""));

            hWC.add(dayResource, RDF.type, hWC.createResource(NS_QB + "Observation"));
            hWC.add(dayResource, hWC.createProperty(NS_QB, "dataSet"), hWC.createResource(NS_HALIAS_SCHEMA + "weatherDataset"));

            // ADD DATE AND CALENDAR INFORMATION

            hWC.add(dayResource, hWC.createProperty(NS_HALIAS_SCHEMA, "weekOfYear"), hWC.createTypedLiteral( (byte) cal.get(java.util.Calendar.WEEK_OF_YEAR )));
            hWC.add(dayResource, hWC.createProperty(NS_HALIAS_SCHEMA, "monthOfYear"), hWC.createTypedLiteral( month.byteValue() ));

            hWC.add(dayResource, hWC.createProperty(NS_HALIAS_SCHEMA, "refTime"), hWC.createTypedLiteral(dateISO, XSD.date.getURI()));

            hWC.add(dayResource, hWC.createProperty(NS_HALIAS_SCHEMA, "sunriseTime"), hWC.createTypedLiteral(sunriseString, XSD.time.getURI()));
            hWC.add(dayResource, hWC.createProperty(NS_HALIAS_SCHEMA, "sunsetTime"), hWC.createTypedLiteral(sunsetString, XSD.time.getURI()));

            hWC.add(dayResource, hWC.createProperty(NS_HALIAS_SCHEMA, "season"), getSeason(month));

            // ADD WEATHER DATA FOR STANDARD OBSERVATION TIME

            MorningWeather morningAverages = weather.calculateMorningWeather(dayLen.sunriseH, dayLen.sunriseMin);

            Property p;
            RDFNode o;

            if (morningAverages != null) {

                p = hWC.createProperty(NS_HALIAS_SCHEMA, "standardTemperature");
                if (morningAverages.temperature != null) {
                    Double standardTemperature = (double) Math.round( morningAverages.temperature );
                    o = hWC.createTypedLiteral( standardTemperature );
                } else {
                    o = hWC.createTypedLiteral( Double.NaN );
                }
                hWC.add(dayResource, p, o);

                if (morningAverages.winds != null) {
                    for (Iterator<WindInstance> i = morningAverages.winds.iterator(); i.hasNext(); ) {
                        WindInstance wind = i.next();
                        hWC.add(dayResource, hWC.createProperty(NS_HALIAS_SCHEMA, "standardWind"), hWC.createProperty(NS_WINDS + wind));
                    }
                } else {
                    hWC.add(dayResource, hWC.createProperty(NS_HALIAS_SCHEMA, "standardWind"), hWC.createProperty(NS_WINDS + "windUnknown"));
                }

                p = hWC.createProperty(NS_HALIAS_SCHEMA, "standardCloudCover");
                if (morningAverages.cloudCover != null) {
                    Double standardCloudCover = (double) Math.round( morningAverages.cloudCover );
                    o = hWC.createTypedLiteral( standardCloudCover );
                } else {
                    o = hWC.createTypedLiteral(Double.NaN);
                }
                hWC.add(dayResource, p, o);
            }

            // ADD DAY'S WEATHER DATA

            p = hWC.createProperty(NS_HALIAS_SCHEMA, "temperatureDay");
            if ( weather.tempDayN > 0 ) {
                Double tempDay = (double) Math.round(((double) weather.tempDaySum ) / weather.tempDayN );
                o = hWC.createTypedLiteral( tempDay );
            } else {
                o = hWC.createTypedLiteral(Double.NaN);
            }
            hWC.add(dayResource, p, o);

            if ( weather.humidityN > 0 ) {
                Double humidity = (double) Math.round(((double) weather.humiditySum ) / weather.humidityN );
                hWC.addLiteral(dayResource, hWC.createProperty(NS_HALIAS_SCHEMA, "humidity"), hWC.createTypedLiteral( humidity ));
            } else {
                hWC.addLiteral(dayResource, hWC.createProperty(NS_HALIAS_SCHEMA, "humidity"), hWC.createTypedLiteral(Double.NaN));
            }

            if ( weather.pressureN > 0 ) {
                Double pressure = (double) Math.round(((double) weather.pressureSum ) / weather.pressureN );
                hWC.addLiteral(dayResource, hWC.createProperty(NS_HALIAS_SCHEMA, "airPressure"), hWC.createTypedLiteral( pressure ));
            } else {
                hWC.addLiteral(dayResource, hWC.createProperty(NS_HALIAS_SCHEMA, "airPressure"), hWC.createTypedLiteral(Double.NaN));
            }

            if ( weather.cloudCoverDayN > 0 ) {
                Double clouds = (double) Math.round(((double) weather.cloudCoverDaySum ) / weather.cloudCoverDayN );
                hWC.addLiteral(dayResource, hWC.createProperty(NS_HALIAS_SCHEMA, "cloudCover"), hWC.createTypedLiteral( clouds ));
            } else {
                hWC.addLiteral(dayResource, hWC.createProperty(NS_HALIAS_SCHEMA, "cloudCover"), hWC.createTypedLiteral( Double.NaN ));
            }

            // WINDS

            if ( weather.windsPreSunrise.size() > 0 ) {
                for (Iterator<WindInstance> i = weather.windsPreSunrise.iterator(); i.hasNext(); ) {
                    WindInstance item = i.next();
                    hWC.add(dayResource, hWC.createProperty(NS_HALIAS_SCHEMA, "windPreSunrise"), hWC.createResource(NS_WINDS + item));
                }
            } else {
                hWC.add(dayResource, hWC.createProperty(NS_HALIAS_SCHEMA, "windPreSunrise"), hWC.createProperty(NS_WINDS + "windUnknown"));
            }
            if ( weather.windsPostSunset.size() > 0 ) {
                for (Iterator<WindInstance> i = weather.windsPostSunset.iterator(); i.hasNext(); ) {
                    WindInstance item = i.next();
                    hWC.add(dayResource, hWC.createProperty(NS_HALIAS_SCHEMA, "windPostSunset"), hWC.createResource(NS_WINDS + item));
                }
            } else {
                hWC.add(dayResource, hWC.createProperty(NS_HALIAS_SCHEMA, "windPostSunset"), hWC.createProperty(NS_WINDS + "windUnknown"));
            }

            if ( weather.windsDay.size() > 0 ) {
                for (Iterator<WindInstance> i = weather.windsDay.listIterator(); i.hasNext(); ) {
                    WindInstance item = i.next();
                    if (item != null) {
                        String item_str = item.toString();
                        hWC.add(dayResource, hWC.createProperty(NS_HALIAS_SCHEMA, "windDay"), hWC.createResource(NS_WINDS + item_str));
                    }
                }
            } else {
                hWC.add(dayResource, hWC.createProperty(NS_HALIAS_SCHEMA, "windDay"), hWC.createProperty(NS_WINDS + "windUnknown"));
            }

            // RAIN
            if ( weather.rainfall != null ) {
                hWC.addLiteral(dayResource, hWC.createProperty(NS_HALIAS_SCHEMA, "rainfall"), hWC.createTypedLiteral((double) Math.round(weather.rainfall)));
            } else {
                hWC.addLiteral(dayResource, hWC.createProperty(NS_HALIAS_SCHEMA, "rainfall"), hWC.createTypedLiteral(Double.NaN));
            }

            // HALIAS OBSERVATION DAY

            if (!hWC.listObjectsOfProperty(dayResource, hWC.createProperty(NS_HALIAS_SCHEMA, "haliasObservationDay")).hasNext()) {
                hWC.addLiteral(dayResource, hWC.createProperty(NS_HALIAS_SCHEMA, "haliasObservationDay"), hWC.createTypedLiteral(false));
            }

        }

        hWC.setNsPrefix("hs", NS_HALIAS_SCHEMA);
        hWC.setNsPrefix("hw", NS_HALIAS_WEATHER);
        hWC.setNsPrefix("winds", NS_WINDS);
        hWC.setNsPrefix("qb", NS_QB);
        hWC.setNsPrefix("xsd", XSD.getURI());
        hWC.setNsPrefix("rdfs", RDFS.getURI());
        writeFile(hWC, OUTPUT_DIRECTORY + "halias_weather_cube.ttl", "TTL" );
    }


    /**
     * Create instances of Russarö weather observations and save them to file.
     */
    public void createRussaroCube() {

        for (Map.Entry<String, DailyWeather> entry : weatherRussaro.getDailyWeathers().entrySet()) {
            DailyWeather weather = entry.getValue();
            String index = entry.getKey();

            // ADD ALL WEATHER OBSERVATIONS FOR THIS DAY

            for (Integer i=0; i < weather.weatherObservation.length; i++) {
                DailyWeather.ConcurrentObservations observations = weather.weatherObservation[i];

                String hour = Integer.toString(i * 3);
                if (hour.length() == 1)
                    hour = '0' + hour;
                Resource s = rWC.createResource(NS_RUSSAROCUBE + index.replaceAll("\\-", "") + "_" + hour);
                RDFNode o = rWC.createResource(NS_QB + "Observation");
                rWC.add(s, RDF.type, o);

                Property p = rWC.createProperty(NS_HALIAS_SCHEMA, "observationTime");
                o = rWC.createTypedLiteral(index + "T" + hour + ":00:00" + "Z", XSD.dateTime.getURI());
                rWC.add(s, p, o);

                rWC.add(s, rWC.createProperty(NS_QB, "dataSet"), rWC.createResource(NS_HALIAS_SCHEMA + "russaroDataset"));

                p = rWC.createProperty(NS_HALIAS_SCHEMA, "temperature");
                o = rWC.createTypedLiteral(observations.temperature == null ? Double.NaN : observations.temperature);
                rWC.add(s, p, o);

                p = rWC.createProperty(NS_HALIAS_SCHEMA, "airPressure");
                o = rWC.createTypedLiteral(observations.pressure == null ? Double.NaN : observations.pressure);
                rWC.add(s, p, o);

                p = rWC.createProperty(NS_HALIAS_SCHEMA, "cloudCover");
                o = rWC.createTypedLiteral(observations.cloudCover == null ? Double.NaN : observations.cloudCover);
                rWC.add(s, p, o);

                p = rWC.createProperty(NS_HALIAS_SCHEMA, "humidity");
                o = rWC.createTypedLiteral(observations.humidity == null ? Double.NaN : observations.humidity);
                rWC.add(s, p, o);

                p = rWC.createProperty(NS_HALIAS_SCHEMA, "wind");
                o = rWC.createProperty(observations.wind == null ? NS_WINDS + "windUnknown" : NS_WINDS + observations.wind);
                rWC.add(s, p, o);
            }
        }

        rWC.setNsPrefix("hs", NS_HALIAS_SCHEMA);
        rWC.setNsPrefix("r", NS_RUSSAROCUBE);
        rWC.setNsPrefix("winds", NS_WINDS);
        rWC.setNsPrefix("qb", NS_QB);
        rWC.setNsPrefix("xsd", XSD.getURI());
        writeFile(rWC, OUTPUT_DIRECTORY + "russaro_weather_cube.ttl", "TTL" );
    }

    /**
	 * Do all necessary processing.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println( "- STARTING HALIAS PROCESSING -" );
		HaliasDataProcessor hc = new HaliasDataProcessor();

		hc.readTaxonOntologies();
		hc.readCoreOntologies();
		hc.readConservationStatuses();
		hc.weatherRussaro.readWeatherCSV(WEATHER_DATA_DIRECTORY + "Russaro.csv", WEATHER_DATA_DIRECTORY + "RussaroSademaara.csv");
		
		hc.readStandardizedObservations();		
		hc.labelTaxons();
		
		if (!Arrays.asList(args).contains("-skip"))
		{
			// Convert observation files
			for ( Integer i = 0; i < 5; i++ ) {
				System.out.println( "- PROCESSING #" + i + "... -" );
				hc.observationOntology.read("file:" + PYTHON_OUPUT_DIRECTORY + "HALIAS" + i + ".rdf", "RDF/XML");

				hc.observationOntology.setNsPrefix("xsd", XSD.getURI());
				hc.observationOntology.setNsPrefix("dgui", NS_DGU_INTERVALS);
				hc.observationOntology.setNsPrefix("halias", NS_HALIAS_OBSERVATIONS);
				hc.observationOntology.removeNsPrefix( "halias-schema" );
				hc.observationOntology.setNsPrefix("hs", NS_HALIAS_SCHEMA);
				hc.observationOntology.setNsPrefix("winds", NS_WINDS);
                hc.observationOntology.setNsPrefix("bio", NS_BIO);
                hc.observationOntology.setNsPrefix("rdfs", RDFS.getURI());
                hc.observationOntology.setNsPrefix("sdmx-a", "http://purl.org/linked-data/sdmx/2009/attribute");

				System.out.println( "------ add labels" );
				hc.addLabelsToObservations();
				System.out.println( "------ process counts" );
				hc.processBirdCounts();
				System.out.println( "------ add weather data" );
				hc.addDateInformation();

				System.out.println( "------ write to file" );
				hc.writeFile( hc.observationOntology, OUTPUT_DIRECTORY + "HALIAS" + i + "_full.ttl", "TTL" );
				hc.observationOntology = ModelFactory.createDefaultModel();
				System.out.println();
			}
		} else {
            System.out.println( "--- SKIPPING DATA PROCESSING ---" );
        }
		
		System.out.println( "- Add characteristics to taxa -" );
		hc.addSpeciesCharacteristics();
		
		System.out.println( "- WRITING WINDS... -" );
		hc.createWinds();

        System.out.println( "- WRITING WEATHER CUBES... -" );
        hc.createHaliasWeatherCube();
        hc.createRussaroCube();

        System.out.println( "- WRITING SCHEMA... -" );
		
		// Remove authors because of their malformed URIs. This is due to Jena bug fixed in 2.11.1.
		hc.taxonOntology.removeAll(null, RDF.type, hc.taxonOntology.createResource(NS_TAXMEON + "Author"));
		hc.taxonOntology.removeAll(null, hc.taxonOntology.createProperty(NS_TAXMEON + "abbreviation"), null);
		hc.taxonOntology.removeAll(null, hc.taxonOntology.createProperty(NS_TAXMEON + "lastName"), null);
		hc.taxonOntology.removeAll(null, DC.creator, null);
		
		hc.taxonOntology.removeNsPrefix( "halias-taxa" );
		hc.taxonOntology.removeNsPrefix( "halias-schema" );
		hc.taxonOntology.setNsPrefix("hs", NS_HALIAS_SCHEMA);
		hc.taxonOntology.setNsPrefix("bc", NS_BIRD_CHARACTERISTICS);
		hc.writeFile( hc.taxonOntology, OUTPUT_DIRECTORY + "halias_taxon_ontology.ttl", "TTL" );

		hc.finalizeCharacteristicsOntology();		
		hc.writeFile( hc.characteristicsOntology, OUTPUT_DIRECTORY + "halias_characteristics_ontology.ttl", "TTL" );

		hc.haliasSchema.removeNsPrefix( "" );
		hc.haliasSchema.removeNsPrefix( "halias-schema" );
		hc.haliasSchema.setNsPrefix("hs", NS_HALIAS_SCHEMA);
		hc.haliasSchema.removeAll(hc.haliasSchema.createResource(NS_HALIAS_SCHEMA), hc.haliasSchema.createProperty(NS_OWL, "imports"), null);
		hc.haliasSchema.removeAll(hc.haliasSchema.createResource(NS_HALIAS_SCHEMA), hc.haliasSchema.createProperty(NS_OWL, "versionInfo"), null);
		hc.writeFile( hc.haliasSchema, OUTPUT_DIRECTORY + "halias_schema.ttl", "TTL" );

		System.out.println( "- DONE -" );
	}
}
