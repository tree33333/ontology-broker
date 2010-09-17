package org.sc.probro.servlets.old;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;

import javax.servlet.*;
import javax.servlet.http.*;

import org.eclipse.jetty.util.log.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;
import org.sc.obo.OBOOntology;
import org.sc.obo.OBOStanza;
import org.sc.obo.OBOValue;
import org.sc.probro.BrokerProperties;
import org.sc.probro.data.DBModelException;
import org.sc.probro.data.DBObject;
import org.sc.probro.data.DBObjectModel;
import org.sc.probro.data.OntologyObject;
import org.sc.probro.exceptions.BrokerException;
import org.sc.probro.lucene.IndexCreator;
import org.sc.probro.sparql.*;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;

/**
 * Cleared for DBObjectModel usage.
 * 
 * @deprecated
 * @author tdanford
 */
public class OntologyListServlet extends DBObjectListServlet<OntologyObject> {
	
	private OBOSparql oboSparql;
	private Prefixes prefs;
	private File indexDir;

	public OntologyListServlet(BrokerProperties ps) { 
		super(ps, OntologyObject.class);
		oboSparql = new OBOSparql(ps);
		prefs = Prefixes.DEFAULT;
		indexDir = new File(ps.getLuceneIndex());
	}
	
	public void addOntologyToIndex(Map<String,Set<String>> names, Map<String,Set<String>> defs) throws BrokerException {
		
		File dir = indexDir;
		String type = "ontology";

		try { 
			IndexCreator creator = 
				new IndexCreator(dir);
				//null;
			
			try { 
				for(String id : names.keySet()) { 
					
					Set<String> accs = names.get(id);
					Set<String> descs = defs.get(id);
					
					if(creator != null) { 
						creator.addTerm(id, type, accs, descs);
					} else { 
						Log.info(String.format("INDEX: %s (%s) %s %s", id, type, String.valueOf(accs), String.valueOf(descs)));
					}
				}

			} finally {
				if(creator != null) { 
					try { 
						creator.checkpoint();
						creator.close();
					} catch(IOException e) { 
						throw new BrokerException(e); 
					}
				}
			}

		} catch(IOException e) { 
			throw new BrokerException(e);
		}

	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		try { 
			try {
				StringBuilder sb = new StringBuilder();
				String line;
				BufferedReader reader = request.getReader();
				while((line = reader.readLine()) != null) {
					if(sb.length() > 0) { sb.append("\n"); }
					sb.append(line);
				}
				String finalString = sb.toString();
				Log.info(finalString);
				
				
				JSONObject json = new JSONObject(finalString); 
				String ontologyName = null;
				
				if(json.has("name") && (json.get("name") instanceof String)) {  
					ontologyName = json.getString("name");
				} else { 
					throw new BrokerException(HttpServletResponse.SC_BAD_REQUEST, "No " +
							"String-valued 'name' field given in POSTed JSON");
				}

				String ontologyURI = OBOSparql.createGraphURI(ontologyName);
				
				String query = String.format( 
					"%s\nselect distinct ?x ?p ?v where { " +
					" graph <%s> { " + 
					"?x a owl:Class ; ?p ?v . " +
					"FILTER ( isLiteral(?v) && ( isBlank(?v) || datatype(?v) = xsd:string ) )" +
					"} }",
					prefs.getSparqlPrefixStatement("owl"),
					ontologyURI);

				Map<String,Set<String>> ids = new TreeMap<String,Set<String>>();
				Map<String,Set<String>> descs = new TreeMap<String,Set<String>>();
				
				BindingTable<RDFNode> tbl = oboSparql.query(query);
				for(int i = 0; i < tbl.size(); i++) { 
					String key = tbl.get(i, "x").toString();
					if(!ids.containsKey(key)) { 
						ids.put(key, new TreeSet<String>());
						descs.put(key, new TreeSet<String>());
					}
				}
				
				for(int i = 0; i < tbl.size(); i++) {
					String pred = tbl.get(i, "p").toString();
					String key = tbl.get(i, "x").toString();
					if(NAMES.contains(pred)) { 
						String value = tbl.get(i, "v").as(Literal.class).getString();
						ids.get(key).add(value);
						
					} else if (DEFS.contains(pred)) {
						String value = tbl.get(i, "v").as(Literal.class).getString();
						descs.get(key).add(value);
					}
				}
				
				Log.info(String.format("Loaded %d terms.", ids.size()));

				DBObjectModel model = getDBObjectModel();
				try { 
					OntologyObject template = new OntologyObject();
					template.name = ontologyName;

					if(model.count(OntologyObject.class, template) > 0) { 
						throw new BrokerException(HttpServletResponse.SC_CONFLICT,
								String.format("Ontology '%s' already exists", ontologyName));					
					} else { 
						addOntologyToIndex(ids, descs);
						model.create(OntologyObject.class, template);

						response.setStatus(HttpServletResponse.SC_OK);
						response.sendRedirect("/ontologies");
					}
				} finally { 
					model.close();
				}

			} catch (JSONException e) {
				throw new BrokerException(e);

			} catch (DBModelException e) {
				throw new BrokerException(e);
			}
		} catch(BrokerException e) { 
			handleException(response, e);
		}
	}
	
	public static String[] defURIs = new String[] { 
		"http://ccdb.ucsd.edu/SAO/1.2#definition", //	
		"http://ccdb.ucsd.edu/SAO/1.2#externallySourcedDefinition", //	
		"http://ccdb.ucsd.edu/SAO/1.2#note", //	
		"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#ALT_DEFINITION", //
		"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#DEFINITION", //	DEFINITION
		"http://ontology.neuinfo.org/NIF/Backend/BIRNLex_annotation_properties.owl#birnlexComment", //	
		"http://ontology.neuinfo.org/NIF/Backend/BIRNLex_annotation_properties.owl#birnlexDefinition", //	
		"http://ontology.neuinfo.org/NIF/Backend/OBO_annotation_properties.owl#altDefinition", //	
		"http://www.w3.org/2000/01/rdf-schema#comment", //	
		"http://purl.obolibrary.org/obo/IAO_0000115", //	definition
		"http://purl.org/dc/elements/1.1/description", //	
		"http://www.bootstrep.eu/ontology/GRO#definition", //	
		"http://www.ebi.ac.uk/efo/definition", //	
		"http://www.nbirn.net/birnlex/1.0/OBO_annotation_properties.owl#tempDefinition", //	
		"http://www.nbirn.net/birnlex/1.0/OBO_annotation_properties.owl#externallySourcedDefinition", //	
		"http://ontology.neuinfo.org/NIF/Backend/OBO_annotation_properties.owl#externallySourcedDefinition", //	
		"http://ontology.neuinfo.org/NIF/Backend/OBO_annotation_properties.owl#tempDefinition", //
	};

	public static String[] nameURIs = new String[] {
		"http://purl.obolibrary.org/obo/IAO_0000111",//	editor preferred term
		"http://purl.obolibrary.org/obo/IAO_0000118",//	alternative term
		"http://www.bootstrep.eu/ontology/GRO#synonym",	
		"http://www.ebi.ac.uk/efo/alternative_term",	
		"http://ccdb.ucsd.edu/SAO/1.2#synonym",	
		"http://ccdb.ucsd.edu/SAO/1.2#abbreviation",
		"http://mged.sourceforge.net/ontologies/MGEDOntology.owl#unique_identifier",	
		"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#Chemical_Formula",//	Chemical_Formula
		"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#Display_Name",//	Display_Name
		"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#FULL_SYN",	//FULL_SYN
		"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#Preferred_Name",	//Preferred_Name
		"http://ontology.neuinfo.org/NIF/Backend/OBO_annotation_properties.owl#abbrev",	
		"http://ontology.neuinfo.org/NIF/Backend/OBO_annotation_properties.owl#acronym",	
		"http://www.nbirn.net/birnlex/1.0/OBO_annotation_properties.owl#acronym",	
		"http://www.nbirn.net/birnlex/1.0/OBO_annotation_properties.owl#synonym",	
		"http://www.w3.org/2000/01/rdf-schema#label",	
		"http://www.w3.org/2000/01/rdf-schema#seeAlso",	
		"http://www.w3.org/2004/02/skos/core#altLabel",	
		"http://www.w3.org/2004/02/skos/core#definition",	
		"http://www.w3.org/2004/02/skos/core#prefLabel",
		"http://ontology.neuinfo.org/NIF/Backend/OBO_annotation_properties.owl#synonym",
		"http://www.geneontology.org/formats/oboInOwl#hasURI", //	has_URI
		"http://www.geneontology.org/formats/oboInOwl#hasDbXref", //	has_dbxref
		"http://www.geneontology.org/formats/oboInOwl#hasAlternativeId", //	has_alternative_id
	};
	
	public static Set<String> NAMES, DEFS; 
	 
	static { 
		NAMES = new TreeSet<String>(Arrays.asList(nameURIs));
		DEFS = new TreeSet<String>(Arrays.asList(defURIs));
	}
}
