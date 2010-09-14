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
	
	@org.junit.Test
	public void testServerAvailability() throws IOException { 
		String urlstring = server + "/";
		URL url = new URL(urlstring);
		HttpURLConnection cxn = (HttpURLConnection) url.openConnection();
		cxn.connect();
		int responseCode = cxn.getResponseCode();
		assertTrue(
				String.format("Server %s returned response code %d", urlstring, responseCode),
				responseCode == 200);
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

