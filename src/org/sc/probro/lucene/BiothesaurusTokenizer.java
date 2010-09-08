package org.sc.probro.lucene;

import java.util.*;
import java.io.Reader;
import org.apache.lucene.analysis.CharTokenizer;
import org.apache.lucene.util.AttributeSource;

public class BiothesaurusTokenizer extends CharTokenizer {
	
	private static Set<Character> breakChars;
	
	static { 
		breakChars = new TreeSet<Character>();
		
		breakChars.add('.');
		breakChars.add('-');

		breakChars.add(';');
		breakChars.add(',');
		breakChars.add('?');
		breakChars.add('"');
		breakChars.add('\'');
		breakChars.add('|');
		breakChars.add('/');
		breakChars.add('\\');
		breakChars.add('=');
		breakChars.add('+');
		breakChars.add('(');
		breakChars.add(')');
		breakChars.add('[');
		breakChars.add(']');
	}

	public BiothesaurusTokenizer(Reader in) {
		super(in);
	}

	public BiothesaurusTokenizer(AttributeSource source, Reader in) {
		super(source, in);
	}

	public BiothesaurusTokenizer(AttributeFactory factory, Reader in) {
		super(factory, in);
	}

	protected boolean isTokenChar(char c) {
		return !Character.isWhitespace(c) && !breakChars.contains(c); 
	}
}
