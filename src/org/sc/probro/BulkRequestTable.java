package org.sc.probro;

import java.util.*;
import java.io.*;

import org.json.*;
import org.sc.probro.data.Metadata;
import org.sc.probro.data.Request;

public class BulkRequestTable {

	public static String[] headers = new String[] { 
		"Tracking ID",
		"DB source",
		"name",
		"UniProtKB Ac",
		"protein coordinates (beginning-end)",
		"residue#, Modification",
		"taxon ID",
		"Organism",
	};
	
	private ArrayList<BulkRequestLine> lines;

	public BulkRequestTable() { 
		lines = new ArrayList<BulkRequestLine>();
	}
	
	public BulkRequestTable(String str) throws IOException { 
		this(new StringReader(str));
	}
	
	public BulkRequestTable(Reader r) throws IOException { 
		this();
		BufferedReader br = new BufferedReader(r);
		String h = br.readLine();
		if(!h.startsWith(headers[0])) { 
			throw new IllegalArgumentException(String.format("Illegal first line: \"%s\"", h));
		}
		
		String line;
		while((line = br.readLine()) != null) { 
			BulkRequestLine bulkline = new BulkRequestLine(line);
			lines.add(bulkline);
		}
	}
	
	public void printTable(PrintWriter writer) { 
		for(int i = 0; i < headers.length; i++) { 
			if(i > 0) { writer.print("\t"); }
			writer.print(headers[i]);
		}
		writer.println();
		
		for(BulkRequestLine line : lines) { 
			writer.println(line.toString());
		}
	}
	
	public static class BulkRequestLine { 
		
		private String[] data;
		
		public BulkRequestLine() { 
			data = new String[headers.length];
			for(int i = 0; i < data.length; i++) { data[i] = ""; }
		}
		
		public BulkRequestLine(Request req, Collection<Metadata> mds) { 
			this();
			
		}
		
		public BulkRequestLine(String line) { 
			data = line.split("\t");
			if(data.length != headers.length) { 
				throw new IllegalArgumentException(line);
			}
		}
		
		public String toString() { 
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < data.length; i++) { 
				if(i > 0) { sb.append("\t"); }
				sb.append(data[i]);
			}
			return sb.toString();
		}
	}

}

