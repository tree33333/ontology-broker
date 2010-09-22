package org.sc.probro.utils;

import java.util.*;

public class Pairing<S,T> {

	private S[] s;
	private T[] t;
	private Map<S,Integer> sidx;
	private Map<T,Integer> tidx;
	
	public Pairing(S[] ss, T[] tt) {
		if(ss.length != tt.length) { throw new IllegalArgumentException(ss.length + " != " + tt.length); }
		s = ss.clone();
		t = tt.clone();
		sidx = new HashMap<S,Integer>();
		tidx = new HashMap<T,Integer>();
		for(int i = 0; i < s.length; i++) { 
			sidx.put(s[i], i);
			tidx.put(t[i], i);
		}
	}
	
	public boolean containsFirstValue(S v) { return sidx.containsKey(v); }
	public boolean containsSecondValue(T v) { return tidx.containsKey(v); }
	
	public int size() { return s.length; }
	
	public T forward(S s) {
		if(!sidx.containsKey(s)) { 
			throw new IllegalArgumentException(String.format("%s not in %s", String.valueOf(s), sidx.keySet().toString()));
		}
		Integer i = sidx.get(s);
		return t[i];
	}
	
	public S backward(T t) {
		if(!tidx.containsKey(t)) { 
			throw new IllegalArgumentException(String.format("%s not in %s", String.valueOf(t), tidx.keySet().toString()));
		}
		Integer i = tidx.get(t);
		return s[i]; 
	}
	
	public int firstIndex(S s) { return sidx.get(s); }
	public int secondIndex(T t) { return tidx.get(s); }
}

