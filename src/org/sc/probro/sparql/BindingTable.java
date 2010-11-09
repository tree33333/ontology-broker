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
package org.sc.probro.sparql;

import java.lang.reflect.Array;
import java.util.*;

public class BindingTable<Value> {
	
	private Class<Value> valueClass;
	private String[] names;
	private Map<String,Integer> nameIndices;
	private ArrayList<Value[]> bindings;
	
	public BindingTable(Class<Value> cls) { 
		valueClass = cls;
		names = null;
		nameIndices = null;
		bindings = new ArrayList<Value[]>();
	}
	
	public BindingTable(Class<Value> cls, String[] ns) { 
		valueClass = cls;
		names = ns.clone();
		nameIndices = new TreeMap<String,Integer>();
		createNameIndices();
		bindings = new ArrayList<Value[]>();
	}
	
	protected void setNames(String... ns) { 
		names = ns.clone();
		createNameIndices();
	}
	
	public String toString() { 
		StringBuilder sb = new StringBuilder();
		sb.append("#");
		for(String n : names) { 
			sb.append("\t" + n);
		}
		sb.append("\n");
		for(int i = 0; i < bindings.size(); i++) { 
			sb.append(String.valueOf(i));
			for(int j = 0; j < bindings.get(i).length; j++) { 
				sb.append("\t" + bindings.get(i)[j]);
			}
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public void addRow(Value[] r) { 
		if(r.length != names.length) { 
			throw new IllegalArgumentException(String.format("%d != names.length (%d)", 
					r.length, names.length));
		}
		bindings.add(r);
	}
	
	public String[] getNames() { return names; }
	public Value[] getResult(int i) { return bindings.get(i); }
	
	public int size() { return bindings.size(); }
	public int width() { return names.length; }
	
	public Value[] getColumn(int j) {
		Value[] vals = (Value[])Array.newInstance(valueClass, bindings.size());
		for(int i = 0; i < vals.length; i++) { 
			vals[i] = bindings.get(i)[j];
		}
		return vals;
	}
	
	public Value[] getColumn(String name) { 
		for(int j = 0; names != null && j < names.length; j++) { 
			if(names[j].equals(name)) { 
				return getColumn(j);
			}
		}
		return (Value[])Array.newInstance(valueClass, 0);
	}
	
	protected void createNameIndices() { 
		nameIndices = new TreeMap<String,Integer>();
		for(int i = 0; names != null && i < names.length; i++) { 
			nameIndices.put(names[i], i);
		}
	}
	
	public Value get(int k) { 
		return get(k, names[0]);
	}

	public Value get(int k, String string) {
		return bindings.get(k)[nameIndices.get(string)]; 
	}

	public int findNameColumn(String name) {
		return nameIndices.get(name);
	}
}
