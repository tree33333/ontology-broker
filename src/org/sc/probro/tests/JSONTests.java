package org.sc.probro.tests;

import java.io.*;

import static org.junit.Assert.*;

import org.json.JSONException;
import org.json.JSONObject;

import tdanford.json.schema.JSONObjectType;
import tdanford.json.schema.JSONType;
import tdanford.json.schema.SchemaEnv;
import tdanford.json.schema.SchemaException;

import java.io.IOException;
import java.util.*;

public class JSONTests {
	
	@org.junit.Test 
	public void testJSONSchemas() throws IOException {
		BrokerSchemaEnv env = new BrokerSchemaEnv();
		
		testContains(env.getMetadataType(), new MetadataExample());
		testContains(env.getRequestType(), new RequestExample());
		testContains(env.getUserType(), new UserExample());
		testContains(env.getOntologyType(), new OntologyExample());
		testContains(env.getLinkType(), new LinkExample());
	}
	
	public void testContains(JSONType type, Object value) { 
		assertTrue(type.explain(value), type.contains(value));
	}
}

class BrokerSchemaEnv extends SchemaEnv { 
	public BrokerSchemaEnv() { 
		super(new File("docs/json-schemas/"));
	}
	
	public JSONType getRequestType() { return lookupType("Request"); } 
	public JSONType getMetadataType() { return lookupType("Metadata"); } 
	public JSONType getUserType() { return lookupType("User"); }
	public JSONType getOntologyType() { return lookupType("Ontology"); }
	public JSONType getSearchResultType() { return lookupType("SearchResult"); }
	public JSONType getLinkType() { return lookupType("Link"); }
}

class JSONFile extends JSONObject { 
	public JSONFile(File f) throws IOException { 
		super(new FileReader(f));
	}
}

class UserExample extends JSONObject { 
	public UserExample() { 
		try {
			put("user_id", 1);
			put("user_name", "Test Name");
		} catch (JSONException e) {
			throw new IllegalStateException(e);
		}
	}
}

class OntologyExample extends JSONObject { 
	public OntologyExample() { 
		try {
			put("ontology_id", 10);
			put("name", "Test Name");
		} catch (JSONException e) {
			throw new IllegalStateException(e);
		}
	}
}

class RequestExample extends JSONObject {
	
	public RequestExample() { 
		super();
		try {
			put("search_text", "foo");
			put("context", "bar blah blah blah");
			put("provenance", "http://example.com/blah");
			put("date_submitted", "2010-09-01");
			put("status", 200);
			put("ontology_id", "grok");
			put("provisional_id", "quux");
			
			put("created_by", 10);
			
			append("metadata", new MetadataExample(this, "akey", "bvalue"));
			append("metadata", new MetadataExample(this, "ckey", "dvalue"));
		} catch (JSONException e) {
			throw new IllegalStateException(e);
		}
	}
}

class LinkExample extends JSONObject { 
	
	public LinkExample() { 
		this("http://ashby.csail.mit.edu:8080/user/1/");
	}
	
	public LinkExample(String href) { 
		try {
			put("href", href);
		} catch (JSONException e) {
			throw new IllegalStateException(e);
		}
	}
}

class MetadataExample extends JSONObject {
	
	public MetadataExample() { 
		this(null, "foo", "bar");
	}
	
	public MetadataExample(RequestExample req, String key, String value) { 
		super();
		try {
			put("metadata_key", key);
			put("metadata_value", value);
			put("created_on", "2010-09-01");
			put("created_by", new LinkExample("http://user.org/"));
			
		} catch (JSONException e) {
			throw new IllegalStateException(e);
		}
	}
}

