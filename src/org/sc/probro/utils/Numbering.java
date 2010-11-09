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
package org.sc.probro.utils;

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