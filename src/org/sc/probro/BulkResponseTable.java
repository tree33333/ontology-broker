package org.sc.probro;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public class BulkResponseTable extends BulkRequestTable {

	public BulkResponseTable(Reader r) throws IOException { 
		super();
		BufferedReader br = new BufferedReader(r);
		String h = br.readLine();
		if(!h.startsWith(REQUEST_HEADERS[0])) { 
			throw new IllegalArgumentException(String.format("Illegal first line: \"%s\"", h));
		}
		
		String line;
		while((line = br.readLine()) != null) { 
			BulkLine bulkline = new BulkLine(line);
			addLine(bulkline);
		}

	}
	
	public BulkResponseTable(String str) throws IOException { 
		this(new StringReader(str));
	}
}
