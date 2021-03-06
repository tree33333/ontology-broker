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
package org.sc.probro.lucene;

import java.util.*;
import java.io.Reader;
import org.apache.lucene.analysis.CharTokenizer;
import org.apache.lucene.util.AttributeSource;

public class BioTokenizer extends CharTokenizer {
	
	private static Set<Character> breakChars;
	
	static { 
		breakChars = new TreeSet<Character>();
		breakChars.add(';');
		breakChars.add(',');
		//breakChars.add('.');
		breakChars.add('?');
		breakChars.add('"');
		breakChars.add('\'');
		breakChars.add('/');
		breakChars.add('\\');
		breakChars.add('=');
		breakChars.add('(');
		breakChars.add(')');
		breakChars.add('[');
		breakChars.add(']');
	}

	public BioTokenizer(Reader in) {
		super(in);
	}

	public BioTokenizer(AttributeSource source, Reader in) {
		super(source, in);
	}

	public BioTokenizer(AttributeFactory factory, Reader in) {
		super(factory, in);
	}

	protected boolean isTokenChar(char c) {
		return !Character.isWhitespace(c) && !breakChars.contains(c); 
	}
}
