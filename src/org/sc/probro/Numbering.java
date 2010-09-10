package org.sc.probro;

public class Numbering<S> extends Pairing<S,Integer> {
	
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
	
	public boolean containsNumber(int idx) { return idx >= 0 && idx < size(); }
	public boolean containsValue(S value) { return containsFirstValue(value); } 
}