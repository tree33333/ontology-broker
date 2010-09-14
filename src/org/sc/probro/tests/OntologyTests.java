package org.sc.probro.tests;

import static org.junit.Assert.*;

import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

public class OntologyTests {
	
	@org.junit.Test 
	public void testGetOntologies() throws IOException { 
		String server = ServerTests.server;

		String ontologiesURL = server+"/ontologies";
		JSONObject obj = ServerTests.httpGET_JSONObject(ontologiesURL);
		assertTrue(
				String.format("Received null response from %s", ontologiesURL),
				obj != null);
		
		
	}
}
