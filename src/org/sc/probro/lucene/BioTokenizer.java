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
