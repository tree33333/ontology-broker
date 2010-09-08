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
