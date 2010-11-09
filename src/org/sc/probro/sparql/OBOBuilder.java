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
package org.sc.probro.sparql;

import java.util.*;
import java.io.*;

import org.eclipse.jetty.util.log.Log;
import org.sc.obo.OBOOntology;
import org.sc.obo.OBOParser;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

public class OBOBuilder {

	private OBOSparql oboSparql;
	
	public OBOBuilder(OBOSparql sp) { 
		oboSparql = sp;
	}
	
	public OBOOntology buildOntology(String oboName) throws IOException { 
		
		OBOParser parser = new OBOParser();
		
		parser.parse(new BufferedReader(new StringReader(loadOBO(oboName))));
		
		return parser.getOntology();
	}
	
	private BindingTable<RDFNode> query(String graphURI, String head, String body, Prefixes prefs) throws IOException {
		StringBuilder sb = new StringBuilder();
		for(String prefix : prefs.getPrefixes()) {
			if(body.contains(prefix)) { 
				if(sb.length() > 0) { sb.append("\n"); }
				sb.append(prefs.getSparqlPrefixStatement(prefix));
			}
		}
		String query = String.format("%s\nselect %s where { graph <%s> { %s } }",
				sb.toString(),
				head, graphURI, body);
		
		Log.info(String.format("OBO Sparql Query: %s", query));

		BindingTable<RDFNode> tbl = oboSparql.query(query);
		
		Log.info(String.format("OBO Sparql Endpoint: %d rows", tbl.size()));

		return tbl;
	}
	
	public String asOBOId(String uri, String prefix) { 
		if(uri != null && uri.startsWith(prefix)) { 
			uri = uri.substring(prefix.length(), uri.length());
		}
		return uri != null ? uri.replace("_", ":") : null;
	}
	
	public String loadOBO(String oboName) throws IOException {
		StringWriter w = new StringWriter();
		PrintWriter pw = new PrintWriter(w);

		String oboURI = OBOSparql.createGraphURI(oboName);

		Set<String> ids = new TreeSet<String>();
		Map<String,String> names = new TreeMap<String,String>();
		Map<String,String> defs = new TreeMap<String,String>();

		Map<String,Set<String>> isa = new TreeMap<String,Set<String>>();
		Map<String,Set<String>> synonyms = new TreeMap<String,Set<String>>();
		Map<String,Map<String,Set<String>>> relationships = new TreeMap<String,Map<String,Set<String>>>();
		
		Prefixes prefs = new Prefixes(Prefixes.DEFAULT);
		//prefs.addPrefix("oboInOwl", "http://www.geneontology.org/formats/oboInOwl");
		
		String oboURIPrefix = null;
		
		BindingTable<RDFNode> tbl = query(oboURI, 
				"?termid ?termname ?def ?comment",
				"?termid a owl:Class ; rdfs:label ?termname . " 
				+ "optional { ?termid obo:hasDefinition [ rdfs:label ?def ] } " 
				+ "optional { ?termid rdfs:comment ?comment }"
				,
				prefs);
				
		for(int i = 0; i < tbl.size(); i++) { 
		
			RDFNode[] row = tbl.getResult(i);
			
			String id = row[0].as(Resource.class).getURI();
			
			if(oboURIPrefix == null) { 
				int fragment = id.indexOf("#");
				oboURIPrefix = id.substring(0, fragment) + "#";
			}
			id = asOBOId(id, oboURIPrefix);
			
			String name = row[1].as(Literal.class).getString();
			String def = null;
			String comment = null;
			
			if(row[2] != null) { 
				def = row[2].as(Literal.class).getString();
			}
			
			ids.add(id);
			names.put(id, name);
			if(def != null) { 
				defs.put(id, def);
			}
			
			isa.put(id, new TreeSet<String>());
			synonyms.put(id, new TreeSet<String>()); 
			relationships.put(id, new TreeMap<String,Set<String>>()); 
		}
		
		tbl = query(oboURI,
				"?termid ?parentid",
				"?termid a owl:Class ; rdfs:subClassOf ?parentid",
				prefs);

		for(int i = 0; i < tbl.size(); i++) { 
			RDFNode[] row = tbl.getResult(i);
			String termid = asOBOId(row[0].as(Resource.class).getURI(), oboURIPrefix);
			String parentid = asOBOId(row[1].as(Resource.class).getURI(), oboURIPrefix);
			
			if(termid != null && parentid != null && ids.contains(termid) && ids.contains(parentid)) { 
				addToMap(termid, parentid, isa);
			}
		}
		
		for(String id : ids) { 
			pw.println("\n[Term]");
			pw.println(String.format("id: %s", id));
			
			if(names.containsKey(id)) { 
				pw.println(String.format("name: %s", names.get(id)));
			}
			
			if(defs.containsKey(id)) { 
				pw.println(String.format("def: %s", defs.get(id)));
			}
			
			for(String syn : synonyms.get(id)) { 
				pw.println(String.format("synonym: %s RELATED []", syn));
			}
			
			for(String term : isa.get(id)) {
				String tag = names.containsKey(term) ? 
						String.format("%s ! %s", term, names.get(term)) : term;
				pw.println(String.format("is_a: %s", tag));
			}
			
			for(String rel : relationships.get(id).keySet()) {
				for(String term : relationships.get(id).get(rel)) { 
					String tag = names.containsKey(term) ? 
							String.format("%s ! %s", term, names.get(term)) : term;
					pw.println(String.format("relationship: %s %s", rel, tag));
				}
			}
		}
		
		return w.toString();
	}	
	
	private static void addToMap(String key, String value, Map<String,Set<String>> map) { 
		if(!map.containsKey(key)) { map.put(key, new TreeSet<String>()); }
		map.get(key).add(value);
	}
}
