package org.sc.probro;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import org.json.*;
import org.sc.probro.servlets.RequestStateServlet;
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
		
		// New Fields...
		"New Status",
		"Maintainer",
		"Comment",
	};

	private static Pattern requestIDPattern = Pattern.compile("http://.*/request/(\\d+)/?.*$");
	private ArrayList<BulkLine> lines;

	/**
	 * Creates an empty table.
	 */
	public BulkRequestTable() { 
		lines = new ArrayList<BulkLine>();
	}

	public int getNumRows() {
		return lines.size();
	}
	
	protected void addLine(BulkLine line) { 
		lines.add(line);
	}
	
	/**
	 * Converts a row of the table into the equivalent Result object.
	 * 
	 * @param line The index of the row to convert.
	 * @return
	 */
	public Request getRequest(int line) { return lines.get(line).getRequest(); }
	
	public int getNewStatus(int line) { return lines.get(line).newStatus(); }
	
	public Collection<Metadata> getMetadata(int line) { return lines.get(line).getMetadata(); }
	
	public void printTable(PrintWriter writer) { 
		for(int i = 0; i < REQUEST_HEADERS.length; i++) { 
			if(i > 0) { writer.print("\t"); }
			writer.print(REQUEST_HEADERS[i]);
		}
		writer.println();
		
		for(BulkLine line : lines) { 
			writer.println(line.toString());
		}
	}
	
	/**
	 * Represents a single row in either the request or response table.
	 * 
	 */
	public static class BulkLine { 
		
		private String[] data;
		private ArrayList<String> extra;
		
		public BulkLine() { 
			data = new String[REQUEST_HEADERS.length];
			extra = new ArrayList<String>();
			for(int i = 0; i < data.length; i++) { data[i] = ""; }
		}
		
		public BulkLine(Request req, Collection<Metadata> mds) { 
			this();
		
			throw new UnsupportedOperationException("implement me");
		}

		public BulkLine(String line) {
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
			return RequestStateServlet.STATES.number(extra.get(0));
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

			//req.request_id = requestID();
			
			req.id = data[0];
			req.search_text = data[2];
			
			if(extra.size() >= 3) { 
				req.status = extra.get(0);
				req.modified_by = new User(null, extra.get(1));
				req.comment = extra.get(2);
			}

			return req;
		}
		
		public Collection<Metadata> getMetadata() { 
			LinkedList<Metadata> mds = new LinkedList<Metadata>();
			Request req = getRequest();
			
			for(int i = 0; i < METADATA_KEYS.length; i++) { 
				if(METADATA_KEYS[i] != null && data[i] != null && data[i].length() > 0) { 
					String[] valueArray = data[i].split("|");
					Integer reqID = requestID();
					for(int j = 0; j < valueArray.length; j++) {
						if(valueArray[j].length() > 0) { 
							Metadata md = new Metadata();
							//md.request_id = reqID;
							
							md.key = METADATA_KEYS[i];
							md.value = valueArray[j];
							
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

