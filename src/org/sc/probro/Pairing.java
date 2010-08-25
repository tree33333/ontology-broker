package org.sc.probro;

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
	
	public int size() { return s.length; }
	public T forward(S s) { return t[sidx.get(s)]; }
	public S backward(T t) { return s[tidx.get(t)]; }
	
	public int firstIndex(S s) { return sidx.get(s); }
	public int secondIndex(T t) { return tidx.get(s); }
}

class Numbering<S> extends Pairing<S,Integer> {
	
	public static <T> Integer[] range(T[] ss) { 
		Integer[] idx = new Integer[ss.length];
		for(int i = 0; i < ss.length; i++) { idx[i] = i; }
		return idx;
	}

	public Numbering(S... ss) { 
		super(ss, range(ss));
	} 
	
	public int number(S s) { return forward(s); }
	public S value(int idx) { return backward(idx); }
}