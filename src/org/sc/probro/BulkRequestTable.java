package org.sc.probro;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import org.json.*;
import org.sc.probro.data.MetadataObject;
import org.sc.probro.data.RequestObject;
import org.sc.probro.utils.Numbering;

/**
 * BulkRequestTable represents the <tt><em>BulkRequest</em></tt> and <tt><em>BulkResponse</em></tt> 
 * file formats described in the <a href="http://neurocommons.org/page/Ontological_term_broker">Ontological 
 * Term Broker design document.</a>
 * 
 * It parses sets of <tt>Request</tt> and <tt>Metadata</tt> objects into the tabular format.  
 * 
 * It also parses the <tt><em>BulkResponse</em></tt> submission from the ontology maintainer, and 
 * creates <tt>Request</tt>/<tt>Metadata objects</tt> from that submission (for comparison to, and 
 * update of, the database).  
 */
public class BulkRequestTable {
	
	public static Numbering<String> STATUS_NUMBERING = 
		new Numbering<String>(
				"REDUNDANT", "FULFILLED",
				"PENDING", "INCOMPLETE",
				"ERROR", "ESCALATE"
				);

	public static String[] REQUEST_HEADERS = new String[] { 
		"Tracking ID",
		"DB source",
		"name",
		"UniProtKB Ac",
		"protein coordinates (beginning-end)",
		"residue#, Modification",
		"taxon ID",
		"Organism",
	};

	public static String[] METADATA_KEYS = new String[] {
		null,
		"db", 
		null,
		"uniprot",
		"coordinates",
		"modification",
		"taxon",
		"organism",
	};
	
	public static String[] RESPONSE_HEADERS = new String[] { 
		"Tracking ID",
		"DB source",
		"name",
		"UniProtKB Ac",
		"protein coordinates (beginning-end)",
		"residue#, Modification",
		"taxon ID",
		"Organism",
		"New Status",
		"Maintainer",
		"Comment",
	};

	private static Pattern requestIDPattern = Pattern.compile("http://.*/request/(\\d+)/?.*$");
	private ArrayList<BulkRequestLine> lines;

	/**
	 * Creates an empty table.
	 */
	public BulkRequestTable() { 
		lines = new ArrayList<BulkRequestLine>();
	}
	
	/**
	 * Creates a table from a string.
	 * 
	 * @param str The raw content of the file (e.g. URL-decoded file upload from user). 
	 * @throws IOException
	 */
	public BulkRequestTable(String str) throws IOException { 
		this(new StringReader(str));
	}
	
	public BulkRequestTable(Reader r) throws IOException { 
		this();
		BufferedReader br = new BufferedReader(r);
		String h = br.readLine();
		if(!h.startsWith(REQUEST_HEADERS[0])) { 
			throw new IllegalArgumentException(String.format("Illegal first line: \"%s\"", h));
		}
		
		String line;
		while((line = br.readLine()) != null) { 
			BulkRequestLine bulkline = new BulkRequestLine(line);
			lines.add(bulkline);
		}
	}

	public int getNumRows() {
		return lines.size();
	}
	
	/**
	 * Converts a row of the table into the equivalent Result object.
	 * 
	 * @param line The index of the row to convert.
	 * @return
	 */
	public RequestObject getRequest(int line) { return lines.get(line).getRequest(); }
	
	public int getNewStatus(int line) { return lines.get(line).newStatus(); }
	
	public Collection<MetadataObject> getMetadata(int line) { return lines.get(line).getMetadata(); }
	
	public void printTable(PrintWriter writer) { 
		for(int i = 0; i < REQUEST_HEADERS.length; i++) { 
			if(i > 0) { writer.print("\t"); }
			writer.print(REQUEST_HEADERS[i]);
		}
		writer.println();
		
		for(BulkRequestLine line : lines) { 
			writer.println(line.toString());
		}
	}
	
	/**
	 * Represents a single row in either the request or response table.
	 * 
	 */
	public static class BulkRequestLine { 
		
		private String[] data;
		private ArrayList<String> extra;
		
		public BulkRequestLine() { 
			data = new String[REQUEST_HEADERS.length];
			extra = new ArrayList<String>();
			for(int i = 0; i < data.length; i++) { data[i] = ""; }
		}
		
		public BulkRequestLine(RequestObject req, Collection<MetadataObject> mds) { 
			this();
		
			throw new UnsupportedOperationException("implement me");
		}

		public BulkRequestLine(String line) {
			this();
			String[] array = line.split("\t");
			if(array.length < REQUEST_HEADERS.length) { 
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
		
		public RequestObject getRequest() { 
			RequestObject req = new RequestObject();
			req.request_id = requestID();
			req.search_text = data[2];
			return req;
		}
		
		public Collection<MetadataObject> getMetadata() { 
			LinkedList<MetadataObject> mds = new LinkedList<MetadataObject>();
			
			for(int i = 0; i < METADATA_KEYS.length; i++) { 
				if(METADATA_KEYS[i] != null && data[i] != null && data[i].length() > 0) { 
					String[] valueArray = data[i].split("|");
					Integer reqID = requestID();
					for(int j = 0; j < valueArray.length; j++) {
						if(valueArray[j].length() > 0) { 
							MetadataObject md = new MetadataObject();
							md.request_id = reqID;
							md.metadata_key = METADATA_KEYS[i];
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


}

