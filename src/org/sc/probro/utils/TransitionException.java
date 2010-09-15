package org.sc.probro.utils;

public class TransitionException extends Exception { 
	public TransitionException(String s1, String m) { 
		super(String.format("%s is not a valid message in state %s", m, s1));
	}
}