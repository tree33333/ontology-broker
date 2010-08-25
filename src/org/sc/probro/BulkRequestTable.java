package org.sc.probro;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import org.json.*;
import org.sc.probro.data.Metadata;
import org.sc.probro.data.Request;

public class BulkRequestTable {
	
	public static Numbering<String> STATUS_NUMBERING = 
		new Numbering<String>(
				"REDUNDANT", "FULFILLED",
				"PENDING", "INCOMPLETE",
				"ERROR", "ESCALATE"
				);

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
	
	public static String[] metadataKeys = new String[] {
		null,
		"db", 
		null,
		"uniprot",
		"coordinates",
		"modification",
		"taxon",
		"organism",
	};
	
	private static Pattern requestIDPattern = Pattern.compile("http://.*/request/(\\d+)/?.*$");
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
	
	public Request getRequest(int line) { return lines.get(line).getRequest(); }
	
	public int getNewStatus(int line) { return lines.get(line).newStatus(); }
	
	public Collection<Metadata> getMetadata(int line) { return lines.get(line).getMetadata(); }
	
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
		private ArrayList<String> extra;
		
		public BulkRequestLine() { 
			data = new String[headers.length];
			extra = new ArrayList<String>();
			for(int i = 0; i < data.length; i++) { data[i] = ""; }
		}
		
		public BulkRequestLine(Request req, Collection<Metadata> mds) { 
			this();
		
			throw new UnsupportedOperationException("implement me");
		}

		public BulkRequestLine(String line) {
			this();
			String[] array = line.split("\t");
			if(array.length < headers.length) { 
				throw new IllegalArgumentException(line);
			}
			
			for(int i = 0; i < data.length; i++) {
				data[i] = array[i];
			}
			
			for(int i = data.length; i < array.length; i++) { 
				extra.add(array[i]);
			}
		}
		
		public int newStatus() { 
			return STATUS_NUMBERING.number(extra.get(0));
		}
		
		public Integer requestID() { 
			Matcher m = requestIDPattern.matcher(data[0]);
			if(m.matches()) { 
				return Integer.parseInt(m.group(0));
			} else { 
				return -1;
			}
		}
		
		public Request getRequest() { 
			Request req = new Request();
			req.request_id = requestID();
			req.search_text = data[2];
			return req;
		}
		
		public Collection<Metadata> getMetadata() { 
			LinkedList<Metadata> mds = new LinkedList<Metadata>();
			
			for(int i = 0; i < metadataKeys.length; i++) { 
				if(metadataKeys[i] != null && data[i] != null && data[i].length() > 0) { 
					String[] valueArray = data[i].split("|");
					Integer reqID = requestID();
					for(int j = 0; j < valueArray.length; j++) {
						if(valueArray[j].length() > 0) { 
							Metadata md = new Metadata();
							md.request_id = reqID;
							md.metadata_key = metadataKeys[i];
							md.metadata_value = valueArray[j];
							mds.add(md);
						}
					}
				}
			}
			
			return mds;
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

	public int getNumRows() {
		return lines.size();
	}

}

