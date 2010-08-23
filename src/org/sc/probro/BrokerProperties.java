package org.sc.probro;

import java.io.File;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

public class BrokerProperties {  

	private ResourceBundle bundle;
	private Map<String,String> properties;
	
	public BrokerProperties() { 
		this("org.sc.probro.broker");
	}

	public BrokerProperties(String qualifiedName) { 
		bundle = ResourceBundle.getBundle(qualifiedName);
		properties = new TreeMap<String,String>();
		Enumeration<String> keys = bundle.getKeys();
		while(keys.hasMoreElements()) { 
			String key = keys.nextElement();
			String value = bundle.getString(key);
			properties.put(key, value);
		}
	}
	
	public Iterator<String> keys() { return properties.keySet().iterator(); }
	public String getStringValue(String k) { return properties.get(k); }
	public boolean hasKey(String k) { return properties.containsKey(k); }
	
	public String getResourceBase() { return getStringValue("resourceBase"); }
	
	public int getPort() { return Integer.parseInt(getStringValue("port")); }
	
	public String getDBPath() { return getStringValue("dbPath"); }
	
	public String getLuceneIndex() { return getStringValue("luceneIndex"); }
}
