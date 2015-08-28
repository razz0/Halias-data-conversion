#!/usr/bin/python
# -*- coding: UTF-8 -*-
"""
Halias observation data converter. Generates interphase RDF files to be processed with an additional Java converter.  

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
"""
from collections import defaultdict
import csv
from time import gmtime, strftime
import distutils.version

from rdflib import *
import rdflib

import halias_helpers as hh
# import ast

import argparse

parser = argparse.ArgumentParser(description='Halias observation generator')
parser.add_argument('-d', action='store_true', help='dry run (don\'t write to disk)')
args = parser.parse_args()

DRYRUN = args.d

if DRYRUN:
    print('Doing a dry run (not writing to disk)')

print('Using RDFLib version: ' + repr(distutils.version.StrictVersion(rdflib.__version__)))
print('Halias observation generator. Use -h for help.')
print('-- Starting observation generation (' + strftime("%Y-%m-%d %H:%M:%S", gmtime()) + ') --')

INPUT_FILE_DIRECTORY = '../data/InputFiles/'
OUTPUT_FILE_DIRECTORY = '../data/PythonOutput/'

# TAXON_INPUT_FILE = INPUT_FILE_DIRECTORY + 'lintuset_HALIAS.ttl'
TAXON_INPUT_FILE = INPUT_FILE_DIRECTORY + 'avio.ttl'

######################################################################################################################

# TAXON PROCESSING

taxon_abbreviations, uris, taxon_ontology = hh.createTaxonAbbreviations(TAXON_INPUT_FILE,
                                                                        INPUT_FILE_DIRECTORY + 'lisataksonit_avioon.ttl',
                                                                        INPUT_FILE_DIRECTORY + "avio_ripustukset_halias.ttl",
                                                                        INPUT_FILE_DIRECTORY + "suomen_lintulajit.txt",
                                                                        INPUT_FILE_DIRECTORY + "halias_taksonit.txt")

print('-- Calculating taxon occurences ' + ' (' + strftime("%Y-%m-%d %H:%M:%S", gmtime()) + ') --')

species_counts = defaultdict(int)
csvReader = csv.reader(open(INPUT_FILE_DIRECTORY + 'HALIAS_Kokodata_20120424_AY.csv', 'rt'), delimiter=';')

for row in csvReader:
    taxon = row[0].lower()
    species_counts[taxon] += 1

print('-- Updating taxon ontologies ' + ' (' + strftime("%Y-%m-%d %H:%M:%S", gmtime()) + ') --')

taxon_ontology.bind("bio", "http://www.yso.fi/onto/bio/")
taxon_ontology.bind("xsd", "http://www.w3.org/2001/XMLSchema#")
taxon_ontology.bind("taxmeon", "http://www.yso.fi/onto/taxmeon/")
taxon_ontology.bind("dgu-intervals", "http://reference.data.gov.uk/def/intervals/")
taxon_ontology.bind("qb", "http://purl.org/linked-data/cube#")
taxon_ontology.bind("dwc", "http://rs.tdwg.org/dwc/terms/")
taxon_ontology.bind("halias-schema", "http://ldf.fi/schema/halias/")
taxon_ontology.bind("halias", "http://ldf.fi/halias/observations/birds/")

# Remove short species-name-labels for those that have a complete taxon name
taxa = taxon_ontology.subjects(RDF.type, hh.nsTaxMeOn["TaxonInChecklist"])
for taxon in taxa:
    labels = taxon_ontology.objects(taxon, RDFS.label)
    for label in labels:
        if not label.language:
            if next(taxon_ontology.objects(taxon, hh.nsTaxMeOn["completeTaxonName"]), False):
                taxon_ontology.remove((taxon, RDFS.label, None))

# Add long labels and dwc:scientificName
for (s, o) in taxon_ontology.subject_objects(hh.nsTaxMeOn["completeTaxonName"]):
    taxon_ontology.add((s, RDFS.label, o))
    taxon_ontology.add((s, hh.nsDWC["scientificName"], o))

# Remove taxmeon:completeTaxonName as it is not present in TaxMeOn
taxon_ontology.remove((None, hh.nsTaxMeOn["completeTaxonName"], None))

# Remove old abbreviations so we get no duplicates with different casing, they are already read into memory.
taxon_ontology.remove((None, hh.nsHaliasSchema["abbreviation"], None))

# Remove hasStatuslessVernacularName as it is no longer present in TaxMeOn
taxon_ontology.remove((None, hh.nsTaxMeOn["hasStatuslessVernacularName"], None))

# Get custom subclassof structure also as partofhighertaxon
for (child, parent) in taxon_ontology.subject_objects(RDFS.subClassOf):
    taxon_ontology.add((child, hh.nsTaxMeOn["isPartOfHigherTaxon"], parent))

# Remove class structure (comes also from avio_ripustukset_halias.ttl)
taxon_ontology.remove((None, RDF.type, hh.nsOWL['Class']))
taxon_ontology.remove((None, RDFS.subClassOf, None))

for species_abbreviation, taxon_name in list(taxon_abbreviations.items()):
    taxon_ontology.add(
        (hh.nsHaliasTaxa[uris[taxon_name]], hh.nsHaliasSchema["abbreviation"], Literal(species_abbreviation)))

if not DRYRUN:
    taxon_ontology.serialize(format="turtle", destination=OUTPUT_FILE_DIRECTORY + "halias_taxa_full.ttl")

# Remove all statements of unused taxa: hasNameStatus, taxonGeneral, ...
taxa = taxon_ontology.subjects(RDF.type, hh.nsTaxMeOn["TaxonInChecklist"])
for taxon in taxa:

    # If we get abbreviations for this taxon or it's descendants, consider it as relevant
    descendants = taxon_ontology.transitive_subjects(hh.nsTaxMeOn["isPartOfHigherTaxon"], taxon)
    abbrs = []

    for des in descendants:
        abbrs = abbrs + list(taxon_ontology.objects(des, hh.nsHaliasSchema["abbreviation"]))

    if not abbrs:
        # This taxon is not needed, remove everything

        generals = taxon_ontology.objects(taxon, hh.nsTaxMeOn["refersToTaxon"])
        for g in generals:
            taxon_ontology.remove((g, None, None))

        vernaculars = taxon_ontology.objects(taxon, hh.nsTaxMeOn["hasVernacularName"])
        for v in vernaculars:
            taxon_ontology.remove((v, None, None))

        statuses = taxon_ontology.objects(taxon, hh.nsTaxMeOn["hasNameStatus"])
        for s in statuses:
            taxon_ontology.remove((s, None, None))

        taxon_ontology.remove((taxon, None, None))
    else:
        # Add rarity for species
        types = taxon_ontology.objects(taxon, RDF.type)
        if hh.nsRanks['Species'] in types:
            abbreviations = taxon_ontology.objects(taxon, hh.nsHaliasSchema["abbreviation"])
            for abb in abbreviations:
                if species_counts[str(abb)] > 300:
                    taxon_ontology.add((taxon, hh.nsHaliasSchema["rarity"], hh.nsHaliasSchema["common"]))
                else:
                    taxon_ontology.add((taxon, hh.nsHaliasSchema["rarity"], hh.nsHaliasSchema["rare"]))

if not DRYRUN:
    taxon_ontology.serialize(format="turtle", destination=OUTPUT_FILE_DIRECTORY + "halias_taxa_v2.ttl")

csvReader = csv.reader(open(INPUT_FILE_DIRECTORY + 'HALIAS_Kokodata_20120424_AY.csv', 'rt'), delimiter=';')

seko = Graph()

used_uris = []
validator = hh.Validator(seko)

print('-- Generating RDF observation data (' + strftime("%Y-%m-%d %H:%M:%S", gmtime()) + ') --')

n = 0
i = 0

# MAIN PROCESSING LOOP
for row in csvReader:
    i += 1

    if (i % 100000) == 0:
        if not DRYRUN:
            hh.writeObservationsRDF(OUTPUT_FILE_DIRECTORY + 'HALIAS' + str(n) + '.rdf', seko)
        seko = Graph()
        n += 1

    taxon = row[0].strip().lower()

    if (taxon == 'laji'):
        # Skip 2 separate header rows in data
        continue

    if row[2] == '2+3':
        row[2] = "5"

    if not taxon in taxon_abbreviations:
        # Unknown taxon renders this row useless 
        validator.validation_error('Unknown taxon: %r', (taxon), [])
        continue

    count_local = hh.parse_count(row[2])
    count_migration = hh.parse_count(row[3])
    count_additional = hh.parse_count(row[4])

    if (count_local or count_migration or count_additional):

        date = row[1].split('/')
        month = date[0]
        day = date[1]
        year = date[2]

        if int(year) > 2008:
            # Skip these (year 2009) as we have standardized migration counts only up to 2008.
            # Otherwise the different scopes would have to be accounted for in result normalization for visualizations.
            continue

        if len(month) == 1:
            month = "0" + month
        if len(day) == 1:
            day = "0" + day

        dateStr2 = year + "-" + month + "-" + day
        uri = "H" + year + month + day + taxon

        if uri in used_uris:
            # Handle the problem that there are double species + date combinations in original data.
            # Because of Data Cube restriction "IC-12. No duplicate observations", duplicate observations will be skipped.

            # uri_index = 2
            # new_uri = uri
            # while new_uri in used_uris:
            #     new_uri = "H" + year + month + day + taxon + "_" + str(uri_index)
            #     uri_index += 1

            # Add error to URIs
            # faulty_uris = [hh.nsHalias[ uri ], hh.nsHalias[ new_uri ]]
            faulty_uris = [hh.nsHalias[uri]]
            validator.validation_error('Date+taxon already exists for %r', (uri), faulty_uris)

            continue  # Skip creating this

            # uri = new_uri
        else:
            used_uris.append(uri)

        seko.add((hh.nsHalias[uri], RDF.type, hh.nsDataCube["Observation"]))
        seko.add((hh.nsHalias[uri], hh.nsDataCube["dataSet"], hh.nsHaliasSchema["haliasDataSet"]))

        validator.validate_date(dateStr2)

        seko.add((hh.nsHalias[uri], hh.nsHaliasSchema["refTime"], Literal(dateStr2, datatype=hh.nsXSD.date)))
        seko.add(
            (hh.nsHalias[uri], hh.nsHaliasSchema["observedSpecies"], hh.nsHaliasTaxa[uris[taxon_abbreviations[taxon]]]))

        # Use 0 for additional area if empty in original data
        seko.add((hh.nsHalias[uri], hh.nsHaliasSchema["countAdditionalArea"], Literal(count_additional or 0)))

        if month in hh.SEASON:
            seko.add((hh.nsHalias[uri], hh.nsHaliasSchema["season"], hh.nsHaliasSchema[hh.SEASON[month]]))

        if (count_local or count_local == 0) and (row[5] == 'FALSE'):
            seko.add((hh.nsHalias[uri], hh.nsHaliasSchema["countLocal"], Literal(str(count_local))))
        if (count_migration or count_migration == 0):
            seko.add((hh.nsHalias[uri], hh.nsHaliasSchema["countMigration"], Literal(str(count_migration))))
    else:
        pass

if not DRYRUN:
    # Write last file
    hh.writeObservationsRDF(OUTPUT_FILE_DIRECTORY + 'HALIAS' + str(n) + '.rdf', seko)

print('-- Observation generation ready (' + strftime("%Y-%m-%d %H:%M:%S", gmtime()) + ') --')
print('-- Parsed %r rows --' % (i))
if validator.validation_errors:
    print('VALIDATION ERRORS: %r' % (validator.validation_errors))
