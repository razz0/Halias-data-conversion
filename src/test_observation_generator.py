#!/usr/bin/python
# -*- coding: UTF-8 -*-
"""
Unit tests for Halias data conversion (observation_generator.py).

Run with "nosetests test_observation_generator.py".

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

import unittest

import halias_helpers

INPUT_FILE_DIRECTORY = '/common/home/mkoho/HALIAS/InputFiles/'
TAXON_INPUT_FILE = INPUT_FILE_DIRECTORY + 'avio.ttl'


class SanityTest(unittest.TestCase):
    """
    Sanity check
    """

    def test_assert(self):
        self.assert_(1 + 1 == 2)


class HaliasHelpersTest(unittest.TestCase):
    """
    Unit tests for Halias helpers (halias_helpers.py).
    """

    def test_abbreviateSpecies(self):
        abbrevation = halias_helpers.abbreviateSpecies('Phylloscopus collybita', 1)
        self.assert_(abbrevation == 'Phycol')

    def test_abbreviateSpecies2(self):
        abbrevation = halias_helpers.abbreviateSpecies('Phylloscopus trochilus', 2)
        self.assert_(abbrevation == 'Phylus')

    def test_abbreviateSpecies3(self):
        self.assertRaises(ValueError, halias_helpers.abbreviateSpecies, 'Phylloscopus trochilus', 42)

    def test_createTaxonAbbreviations(self):
        taxon_abbreviations, uris, taxon_ontology = halias_helpers.createTaxonAbbreviations(
            "python_test_fixtures/test_avio.ttl",
            "python_test_fixtures/test_extra_taxa.ttl",
            "python_test_fixtures/test_extra_taxa.ttl",
            INPUT_FILE_DIRECTORY + "suomen_lintulajit.txt",
            INPUT_FILE_DIRECTORY + "halias_taksonit.txt")

        self.assert_("bom/st" in taxon_abbreviations)
        self.assert_("bomstu" in taxon_abbreviations)
        self.assert_("egralb" in taxon_abbreviations)
        self.assert_("anapla" in taxon_abbreviations)

        self.assert_("ardalb" not in taxon_abbreviations)
        # self.assert_("ard sp" not in taxon_abbreviations)

        self.assert_(taxon_abbreviations["bom/st"] in uris)
        self.assert_(taxon_abbreviations["bomstu"] in uris)

        self.assert_(uris[taxon_abbreviations["bom/st"]] == "bomstu")
        self.assert_(uris[taxon_abbreviations["bomstu"]] == "bomstu")

    def test_createTaxonAbbreviationsFull(self):
        taxon_abbreviations, uris, taxon_ontology = halias_helpers.createTaxonAbbreviations(TAXON_INPUT_FILE,
                                                                                            INPUT_FILE_DIRECTORY + 'lisataksonit_avioon.ttl',
                                                                                            INPUT_FILE_DIRECTORY + "avio_ripustukset_halias.ttl",
                                                                                            INPUT_FILE_DIRECTORY + "suomen_lintulajit.txt",
                                                                                            INPUT_FILE_DIRECTORY + "halias_taksonit.txt")

        # pprint.pprint(taxon_abbreviations)

        # Custom abbreviations
        self.assert_("bom/st" in taxon_abbreviations)
        self.assert_("bomstu" in taxon_abbreviations)
        self.assert_("egralb" in taxon_abbreviations)

        # Generated abbreviations
        self.assert_("anapla" in taxon_abbreviations)
        self.assert_("tadtad" in taxon_abbreviations)
        self.assert_("embele" in taxon_abbreviations)
        self.assert_("parsp" in taxon_abbreviations)
        self.assert_("nycsca" in taxon_abbreviations)
        self.assert_("stecas" in taxon_abbreviations)
        self.assert_("oensp" in taxon_abbreviations)

        self.assert_("ardalb" not in taxon_abbreviations)
        self.assert_("ard sp" not in taxon_abbreviations)

        self.assert_(taxon_abbreviations["bom/st"] in uris)
        self.assert_(taxon_abbreviations["bomstu"] in uris)

        self.assert_(uris[taxon_abbreviations["bom/st"]] == "bomstu")
        self.assert_(uris[taxon_abbreviations["bomstu"]] == "bomstu")
