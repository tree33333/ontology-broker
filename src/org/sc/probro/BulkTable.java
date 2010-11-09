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
package org.sc.probro;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.*;

import org.eclipse.jetty.util.log.Log;
import org.sc.probro.exceptions.BadRequestException;

public class BulkTable {

	public Ontology ontology;
	public ArrayList<String> columns;
	public ArrayList<BulkLine> lines;
	
	private Map<String,String> columnToMetadata;
	
	public BulkTable(Ontology ont) {
		ontology = ont;
		columns = new ArrayList<String>();
		columnToMetadata = new TreeMap<String,String>();
		
		columns.add("Tracking ID");
		columns.add("name");
		for(OntologyField field : ont.fields) { 
			columns.add(field.name);
			columnToMetadata.put(field.name, field.metadata_key);
		}
		
		lines = new ArrayList<BulkLine>();
	}
	
	public BulkTable(Ontology ont, String str) { 
		this(ont);
		
		BufferedReader br = new BufferedReader(new StringReader(str));
		String line;
		try {
			br.readLine(); // Remove header.
			while((line = br.readLine()) != null) { 
				addLine(new BulkLine(line));
			}
		} catch (IOException e) {
			throw new IllegalArgumentException(str);
		}
	}
	
	public void printTable(PrintWriter writer) { 
		writer.println(combineStrings(columns, "\t"));
		for(BulkLine line : lines) { 
			writer.println(combineStrings(line.values, "\t"));
		}
	}

	public void addLine(BulkLine line) { 
		lines.add(line);
	}

	public void addLine(String rawLine) { 
		String[] array = rawLine.split("\t");
		BulkLine line = new BulkLine(array);
		addLine(line);
	}
	
	public void addRequest(Request req) throws BadRequestException { 
		if(!req.ontology.equals(ontology)) {
			throw new BadRequestException(String.format("%s is not ontology %s", req.ontology.ontology_name, ontology.ontology_name));
		}
		
		String[] array = new String[columns.size()];
		for(int i = 0; i < array.length; i++) { array[i] = ""; }
		
		array[0] = req.id;
		array[1] = req.search_text;
		
		Map<String,Set<String>> metadata = req.createMetadataMap();
		Log.info(String.format("Metadata Map: %s", metadata.toString()));

		for(int i = 2; i < columns.size(); i++) {
			String metadataKey = columnToMetadata.get(columns.get(i));
			if(metadataKey != null) { 
				array[i] = combineStrings(metadata.get(metadataKey), "|");
				Log.info(String.format("%s, %s=%s", columns.get(i), metadataKey, array[i]));
			}
		}
		
		addLine(new BulkLine(array));
	}
	
	private static String combineStrings(Collection<String> strs, String sep) {
		if(strs == null) { return ""; }
		StringBuilder sb = new StringBuilder();
		if(strs != null) { 
			for(String str : strs) { 
				if(sb.length() > 0) { sb.append(sep); }
				sb.append(str);
			}
		}
		return sb.toString();
	}
	
	public class BulkLine { 

		public ArrayList<String> values;
		
		public BulkLine(String first, String second, String... vals) {
			values = new ArrayList<String>();
			values.add(first);
			values.add(second);
			values.addAll(Arrays.asList(vals));
			if(values.size() < columns.size()) { 
				throw new IllegalArgumentException(String.valueOf(values));
			}
		}
		
		public BulkLine(String[] array) { 
			values = new ArrayList<String>(Arrays.asList(array));
		}
		
		public BulkLine(String str) { 
			this(str.split("\t"));
		}
	}

	public Request getRequest(int i) {
		// TODO Auto-generated method stub
		return null;
	}
}

