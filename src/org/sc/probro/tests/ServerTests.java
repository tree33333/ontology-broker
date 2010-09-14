package org.sc.probro.tests;

import org.junit.*;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.json.*;
import java.io.*;

import static org.junit.Assert.*;

public class ServerTests {
	
	public static String server = "http://ashby.csail.mit.edu:8080";
	
	public static String[] paths = new String[] { 
		"/ontologies",
		"/users",
		"/requests",
		"/bulk-requests",
	};
	
	public static String urlString(String suffix) { 
		if(suffix.startsWith("/")) { 
			return server + suffix;
		} else { 
			return server + "/" + suffix;
		}
	}
	
	@org.junit.Test
	public void testServerAvailability() throws IOException {
		
		for(String path : paths) { 
			int responseCode = http_ResponseCode("GET", path);
			assertTrue(
					String.format("GET %s returned code %d", 
							urlString(path), responseCode),
					responseCode==200);
		}
	}

	public static int http_ResponseCode(String method, String path) throws IOException { 
		URL url = new URL(urlString(path));
		HttpURLConnection cxn = (HttpURLConnection) url.openConnection();
		cxn.setRequestMethod(method);
		try { 
			cxn.connect();
			int responseCode = cxn.getResponseCode();
			cxn.getInputStream().close();
			return responseCode;
		} catch(Exception e) { 
			return cxn.getResponseCode();
		}
	}
	
	public static String httpPUT_JSONObject(String urlstring, JSONObject postObj) throws IOException { 
		URL url = new URL(urlstring);
		HttpURLConnection cxn = (HttpURLConnection) url.openConnection();
		cxn.setRequestMethod("POST");
		cxn.connect();
		
		OutputStream os = cxn.getOutputStream();
		PrintStream ps = new PrintStream(os);
		ps.println(postObj.toString());
		
		ps.close();
		
		StringBuilder sb = new StringBuilder();
		InputStream is = cxn.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		
		int c ;
		while((c = isr.read()) != -1) { sb.append((char)c); }
		isr.close();
		
		return sb.toString();
	}

	public static JSONObject httpGET_JSONObject(String urlstring) throws IOException { 
		URL url = new URL(urlstring);
		HttpURLConnection cxn = (HttpURLConnection) url.openConnection();
		cxn.connect();

		InputStream is = cxn.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		JSONObject obj = new JSONObject(isr);
		isr.close();
		return obj;
	}
}

