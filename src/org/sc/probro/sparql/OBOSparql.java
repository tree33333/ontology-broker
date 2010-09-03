package org.sc.probro.sparql;

import java.util.*;
import java.io.*;
import java.net.URL;

import org.sc.probro.BrokerProperties;
import org.xml.sax.SAXException;

public class OBOSparql {
	
	private URL sparqlURL;
	private HTTPSparqlEndpoint endpoint;
	
	public OBOSparql(BrokerProperties ps) { 
		this(ps.getOBOSparqlURL());
	}
	
	public OBOSparql(URL url) { 
		sparqlURL = url;
		endpoint = new HTTPSparqlEndpoint(url);
	}
	
	public BindingTable query(String q) throws IOException {
		try { 
			return endpoint.parseResponse(endpoint.makeQuery(q));
		} catch(SAXException e) { 
			throw new IOException(e);
		}
	}
	
	public static String createGraphURI(String oboName) { 
		return "http://purl.org/science/graph/obo/" + oboName;
	}
}
