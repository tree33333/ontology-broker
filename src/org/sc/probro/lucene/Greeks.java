/*
	Copyright 2010 Massachusetts General Hospital

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	    http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/
package org.sc.probro.lucene;

import java.util.*;

public class Greeks {

	private static Set<String> names;
	
	static { 
		names = new TreeSet<String>();
		names.add("alpha");
		names.add("beta");
		names.add("gamma");
		names.add("delta");
		names.add("epsilon");
		names.add("zeta");
		names.add("eta");
		names.add("theta");
		names.add("iota");
		names.add("kappa");
		names.add("lambda");
		names.add("mu");
		names.add("nu");
		names.add("xi");
		names.add("omicron");
		names.add("pi");
		names.add("rho");
		names.add("sigma");
		names.add("tau");
		names.add("upsilon");
		names.add("phi");
		names.add("chi");
		names.add("psi");
		names.add("omega");
	}
	
	public static boolean isGreekName(String n) { 
		return names.contains(n.toLowerCase());
	}
}
