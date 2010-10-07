package org.sc.probro.sparql;

import java.util.*;
import java.util.regex.*;

/**
 * Utility class for URI-rewriting based on string prefixes and the 
 * XML/RDF namespace conventions.  
 * 
 * @author tdanford
 */
public class Prefixes {
	
	public static Prefixes BASE, DEFAULT;
	
	static {
		BASE = new Prefixes();		
		BASE.addPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		BASE.addPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		BASE.addPrefix("owl", "http://www.w3.org/2002/07/owl#");
		BASE.addPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
		
		DEFAULT = new Prefixes(BASE);
		DEFAULT.addPrefix("skos", "http://www.w3.org/2008/05/skos#");
		DEFAULT.addPrefix("foaf", "http://xmlns.com/foaf/0.1/");
		DEFAULT.addPrefix("dc", "http://purl.org/dc/elements/1.1/");
		DEFAULT.addPrefix("dct", "http://purl.org/dc/terms/");
		DEFAULT.addPrefix("sioc", "http://rdfs.org/sioc/ns#");
		
		DEFAULT.addPrefix("ro", "http://www.obofoundry.org/ro/ro.owl#");
		DEFAULT.addPrefix("fma", "http://purl.org/obo/owl/FMA#"); 
		DEFAULT.addPrefix("obo", "http://www.geneontology.org/formats/oboInOwl#");
		//DEFAULT.addPrefix("oboInOwl", "http://www.geneontology.org/formats/oboInOwl#");
		DEFAULT.addPrefix("sc", "http://purl.org/science/owl/sciencecommons/");
		DEFAULT.addPrefix("mesh", "http://purl.org/commons/record/mesh/");
		DEFAULT.addPrefix("go", "http://purl.org/obo/owl/GO#");

		DEFAULT.addPrefix("swan", "http://swan.mindinformatics.org/ontology/1.0/20070313/core.owl#");
		DEFAULT.addPrefix("swancollect", "http://swan.mindinformatics.org/ontology/1.0/20070313/collections.owl#");
		DEFAULT.addPrefix("swanadmin", "http://swan.mindinformatics.org/ontology/1.0/20070313/admin.owl#");

		DEFAULT.addPrefix("swan2", "http://swan.mindinformatics.org/ontologies/1.2/swan-commons/");
		DEFAULT.addPrefix("qual", "http://swan.mindinformatics.org/ontologies/1.2/qualifiers/");
		DEFAULT.addPrefix("rsqual", "http://swan.mindinformatics.org/ontologies/1.2/rsqualifiers/");
		DEFAULT.addPrefix("cite", "http://swan.mindinformatics.org/ontologies/1.2/citations/");
		DEFAULT.addPrefix("lses", "http://swan.mindinformatics.org/ontologies/1.2/lses/");
		DEFAULT.addPrefix("dr", "http://swan.mindinformatics.org/ontologies/1.2/discourse-relationships/");
		DEFAULT.addPrefix("de", "http://swan.mindinformatics.org/ontologies/1.2/discourse-elements/");
		DEFAULT.addPrefix("re", "http://swan.mindinformatics.org/ontologies/1.2/reification/");
		DEFAULT.addPrefix("agents", "http://swan.mindinformatics.org/ontologies/1.2/agents/");
		DEFAULT.addPrefix("collect", "http://swan.mindinformatics.org/ontologies/1.2/collections/");
		DEFAULT.addPrefix("pav", "http://swan.mindinformatics.org/ontologies/1.2/pav/");
		DEFAULT.addPrefix("who", "http://swan.mindinformatics.org/whoweare.html#");
	}

	private Map<String,String> prefixMap;
	
	public Prefixes() { 
		prefixMap = new TreeMap<String,String>();
	}
	
	public Prefixes(Prefixes prefs) { 
		prefixMap = new TreeMap<String,String>(prefs.prefixMap);
	}
	
	public void addPrefix(String p, String fix) {
		for(String key : prefixMap.keySet()) { 
			String prefix = prefixMap.get(key);
			if(prefix.startsWith(fix) || fix.startsWith(prefix)) { 
				throw new IllegalArgumentException(String.format(
						"%s matches prefix of %s (%s)", 
						p, key, prefix));
			}
		}
		prefixMap.put(p, fix);
	}
	
	public String getSparqlPrefixStatement(String key) { 
		return String.format("prefix %s: <%s>", key, prefixMap.get(key));
	}
	
	public String getSparqlPrefixStatements(String... keys) { 
		StringBuilder sb = new StringBuilder();
		for(String key : keys) {  
			sb.append(getSparqlPrefixStatement(key));
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public String getSparqlPrefixStanza() { 
		StringBuilder sb = new StringBuilder();
		for(String key : prefixMap.keySet()) { 
			sb.append(getSparqlPrefixStatement(key));
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public String findPrefix(String uristring) { 
		for(String key : prefixMap.keySet()) { 
			if(uristring.startsWith(prefixMap.get(key))) { 
				return key;
			}
		}
		return null;
	}
	
	public boolean hasPrefix(String pref) { 
		return prefixMap.containsKey(pref);
	}
	

	public String getPrefix(String string) {
		return prefixMap.get(string);
	}
	
	private static Pattern bracketed = Pattern.compile("^<([^>]+>)>$");
	private static Pattern prefixed = Pattern.compile("^([^:]+):(.*)$");
	
	public String expand(String contraction) { 
		Matcher m = bracketed.matcher(contraction);
		if(m.matches()) { 
			return m.group(1);
		} else { 
			m = prefixed.matcher(contraction);
			if(m.matches()) { 
				String prefix = m.group(1);
				if(prefixMap.containsKey(prefix)) { 
					return String.format("%s%s", prefixMap.get(prefix), m.group(2));
				}
			} 
		}
		return contraction;
	}
	
	public String contract(String uristring) { 
		String prefix = findPrefix(uristring);
		if(prefix == null) { 
			//return String.format("<%s>", uristring);
			return uristring;
		} else { 
			String expanded = prefixMap.get(prefix);
			String suffix = uristring.substring(expanded.length(), uristring.length());
			return String.format("%s:%s", prefix, suffix);
		}
	}

	public void setPrefix(String key, String prefix) {
		if(prefixMap.containsKey(key)) { 
			prefixMap.remove(key);
			addPrefix(key, prefix);
		} else  { 
			throw new IllegalArgumentException("key");
		}
	}

	public Set<String> getPrefixes() {
		return prefixMap.keySet();
	}
}
