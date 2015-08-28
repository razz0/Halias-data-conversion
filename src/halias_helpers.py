#!/usr/bin/python
# -*- coding: UTF-8 -*-
"""
Some helpers for Halias data generation.

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

from time import gmtime, strftime
import re

from rdflib import *
import iso8601

SEASON = {}
SEASON['01'] = 'winter'
SEASON['02'] = 'winter'
SEASON['03'] = 'spring'
SEASON['04'] = 'spring'
SEASON['05'] = 'spring'
SEASON['06'] = 'summer'
SEASON['07'] = 'summer'
SEASON['08'] = 'summer'
SEASON['09'] = 'autumn'
SEASON['10'] = 'autumn'
SEASON['11'] = 'autumn'
SEASON['12'] = 'winter'

nsTaxMeOn = Namespace("http://www.yso.fi/onto/taxmeon/")
nsBio = Namespace("http://www.yso.fi/onto/bio/")
nsRanks = Namespace("http://www.yso.fi/onto/taxonomic-ranks/")
nsHh = Namespace("http://www.hatikka.fi/havainnot/")
nsXSD = Namespace("http://www.w3.org/2001/XMLSchema#")
nsDGUIntervals = Namespace("http://reference.data.gov.uk/def/intervals/")
nsDataCube = Namespace("http://purl.org/linked-data/cube#")
nsDWC = Namespace("http://rs.tdwg.org/dwc/terms/")
nsOWL = Namespace("http://www.w3.org/2002/07/owl#")
nsSDMX_A = Namespace("http://purl.org/linked-data/sdmx/2009/attribute#")

nsHalias = Namespace("http://ldf.fi/halias/observations/birds/")
nsHaliasSchema = Namespace("http://ldf.fi/schema/halias/")
nsHaliasTaxa = Namespace("http://www.yso.fi/onto/bio/")


class Validator:
    def __init__(self, observation_graph):
        self.validation_errors = 0
        self.graph = observation_graph

    def validate_date(self, date):
        try:
            iso8601.parse_date(date)
        except (iso8601.ParseError, ValueError):
            self.validation_error('Invalid date: %r', date)

    def validation_error(self, error_text, error_id_tuple, uris, predicate=nsSDMX_A["nonsamplingErr"]):
        """
        Create a validation error message.
        """
        error_string = 'VALIDATION ERROR: ' + error_text % error_id_tuple
        print(error_string)
        self.validation_errors += 1

        # Add validation errors to graph
        for uri in uris:
            self.graph.add((uri, predicate, Literal(error_string)))


def parse_count(cell_value):
    """
    Parse Halias data cell value to integer.
    """
    if cell_value.startswith('"') and cell_value.endswith('"'):
        cell_value = cell_value[1:-1]

    if len(cell_value) > 0:
        cell_value = int(cell_value)
    else:
        cell_value = None

    return cell_value


def abbreviateSpecies(species_name, abbreviation_type=1):
    """
    Create taxon name abbreviation for a species
    """

    genus_short = species_name.split()[0][:3]

    # print species_name

    if abbreviation_type == 1:
        species_short = species_name.split()[1][:3]
    elif abbreviation_type == 2:
        species_short = species_name.split()[1][-3:]
    else:
        raise ValueError("invalid abbreviation type")

    return genus_short + species_short


def createTaxonAbbreviations(input_file, extra_input_file, extra_input_file2, accepted_taxa_file,
                             accepted_abbreviation_file, input_file_format='n3'):
    """
    Generate taxon abbreviations from taxon ontology in input_file and extra_input_file.
    """

    print('-- Reading taxon ontologies --')
    print(input_file)
    print(extra_input_file)

    taxon_ontology = Graph()
    taxon_ontology.parse(input_file, format=input_file_format)

    taxon_ontology.parse(extra_input_file, format='n3')

    taxon_ontology.parse(extra_input_file2, format='n3')

    # Read accepted taxon names into a list
    with open(accepted_taxa_file, "r") as myfile:
        accepted_taxa = myfile.readlines()

    # Filter out lines that are not taxon names
    accepted_taxa = [x for x in accepted_taxa if re.match("^\s{2}\w{6}\s.*", x)]
    # Strip out everything but the taxon name
    accepted_taxa = [x.split("  ")[3].lower() for x in accepted_taxa]
    # Add also genera into accepted
    accepted_taxa.extend(list(set([x.split()[0] for x in accepted_taxa])))

    # Read accepted abbreviations into a list and cast to lowercase
    with open(accepted_abbreviation_file, "r") as myfile:
        accepted_abbreviations = myfile.readlines()

    accepted_abbreviations = [x.lower().replace('\n', '') for x in accepted_abbreviations]

    print('-- Generating taxon abbreviations --')

    genera = taxon_ontology.subjects(RDF.type, nsRanks["Genus"])
    genus_names = []
    uris = {}

    for genus_name in genera:
        genus_local_uri = str(genus_name.toPython()).split('/')[-1]
        taxon_names = taxon_ontology.objects(genus_name, RDFS.label)

        for taxon_name in taxon_names:
            taxon = str(taxon_name.toPython()).lower()
            genus_names.append(taxon)
            uris[taxon] = genus_local_uri

    species_nodes = taxon_ontology.subjects(RDF.type, nsRanks["Species"])
    species_names = []

    for species_node in species_nodes:
        species_local_uri = str(species_node.toPython()).split('/')[-1]
        # taxon_names = taxon_ontology.objects(species_node, RDFS.label)
        taxon_names = taxon_ontology.objects(species_node, nsTaxMeOn['completeTaxonName'])

        for taxon_name in taxon_names:
            taxon = str(taxon_name.toPython()).lower()
            species_names.append(taxon)
            uris[taxon] = species_local_uri

    taxon_abbreviations = {}
    conflicts = {}

    # Abbreviate genera
    for genus_name in genus_names:

        # Take into account both commonly used spellings
        genus_abbreviations = [genus_name[:3] + "sp", genus_name[:3] + " sp"]

        for genus_abbreviation in genus_abbreviations:

            # process only accepted taxons
            if genus_name in accepted_taxa:
                if genus_abbreviation not in conflicts:
                    # Use abbreviation normally

                    if genus_abbreviation not in taxon_abbreviations:
                        # No conflict for this genus name (yet)
                        taxon_abbreviations[genus_abbreviation] = genus_name
                    else:
                        # This genus name is already in abbreviations, use full names for both genera
                        original_genus = taxon_abbreviations.pop(genus_abbreviation)

                        taxon_abbreviations[original_genus] = original_genus
                        taxon_abbreviations[genus_name] = genus_name

                        conflicts[genus_abbreviation] = True
                else:
                    # Use full genus name
                    taxon_abbreviations[genus_name] = genus_name

    # Abbreviate species
    for species_name in species_names:
        species_abbreviation = abbreviateSpecies(species_name, 1)

        if species_name in accepted_taxa:
            if (species_abbreviation not in taxon_abbreviations):
                taxon_abbreviations[species_abbreviation] = species_name
            else:
                # Conflicting name, create type 2 abbreviations for both species
                original_species = taxon_abbreviations.pop(species_abbreviation)
                taxon_abbreviations[abbreviateSpecies(original_species, 2)] = original_species
                taxon_abbreviations[abbreviateSpecies(species_name, 2)] = species_name

    # Parse custom abbreviations
    for (resource, abbreviation) in taxon_ontology.subject_objects(nsHaliasSchema['abbreviation']):
        taxon_labels = taxon_ontology.objects(resource, RDFS.label)
        taxon_label = next(taxon_labels).lower()  # Just take some label

        # for abbreviation_literal in abbreviation:
        taxon_abbreviation = str(abbreviation).lower()
        local_uri = str(resource).split('/')[-1]

        taxon_abbreviations[taxon_abbreviation] = taxon_label
        uris[taxon_label] = local_uri
        # print taxon_label

        # print "Found custom abbreviation %r for taxon %r with local URI %r" % (taxon_abbreviation, taxon_label, uris[ taxon_label ] )

    # Filter out abbreviations that are not used in Halias-data.  WHY ?

    # keys = filter(lambda x: x in accepted_abbreviations, taxon_abbreviations.keys())

    # import pprint
    # pprint.pprint( set(taxon_abbreviations.keys()).difference(set(keys)))

    # taxon_abbreviations = dict(( key, taxon_abbreviations[key] ) for key in keys )
    # taxon_abbreviations = dict(( key, taxon_abbreviations[key] ) for key in keys )

    return taxon_abbreviations, uris, taxon_ontology


def writeObservationsRDF(fileName, graph_instance):
    print('-- Writing observation file ' + str(fileName) + ' (' + strftime("%Y-%m-%d %H:%M:%S", gmtime()) + ') --')

    # graph_instance.namespace_manager.reset()
    # graph_instance.bind("dc", "http://http://purl.org/dc/elements/1.1/")
    # graph_instance.bind("foaf", "http://xmlns.com/foaf/0.1/")
    graph_instance.bind("bio", "http://www.yso.fi/onto/bio/")
    graph_instance.bind("xsd", "http://www.w3.org/2001/XMLSchema#")
    # graph_instance.bind("taxmeon", "http://www.yso.fi/onto/taxmeon/")
    # graph_instance.bind("bio-meta", "http://www.yso.fi/onto/bio-meta/")
    # graph_instance.bind("envirofi", "http://www.yso.fi/onto/envirofi/")
    # graph_instance.bind("misidentifications", "http://www.yso.fi/onto/envirofi/")
    graph_instance.bind("dgu-intervals", "http://reference.data.gov.uk/def/intervals/")
    graph_instance.bind("qb", "http://purl.org/linked-data/cube#")
    graph_instance.bind("dwc", "http://rs.tdwg.org/dwc/terms/")

    graph_instance.bind("sdmx-a", str(nsSDMX_A))

    graph_instance.bind("halias-schema", "http://ldf.fi/schema/halias/")
    graph_instance.bind("halias", "http://ldf.fi/halias/observations/birds/")
    # graph_instance.bind("halias-taxa", "http://ldf.fi/halias/taxa/")

    graph_instance.serialize(format="pretty-xml", destination=fileName)
