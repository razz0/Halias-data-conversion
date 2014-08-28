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

public class HaliasValidator {
	public Integer validationErrors;
	
	public String validationError(String error_text, String dateString, String species) {
		String error_string = "VALIDATION ERROR: " + error_text;
        System.out.println(error_string + ". " + dateString + " - " + species );

        return error_string;
	}
	
	public String validateMigrationCounts(String dateString, String species, String migration, Integer standardized_migration) {
		Integer mig;
		if (migration == "") {
			mig = 0; // We assume missing == 0 for validation
		} else {
			try {
				mig = Integer.parseInt(migration);
			} catch (Exception e) {
				return validationError("Unable to parse migration count: \"" + migration + "\"", dateString, species);
			}
		}
		if (standardized_migration > mig) {
            return validationError("Standardized migration count (" + standardized_migration +
					") more than total migration count (" + mig + ")", dateString, species);
		}
        return "";
	}
}
