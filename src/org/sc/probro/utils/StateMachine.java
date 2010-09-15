package org.sc.probro.utils;

import java.util.*;
import java.util.regex.*;

public class StateMachine {
	
	public static void main(String[] args) throws TransitionException { 
		StateMachine sm = new StateMachine(true);
		
		sm.addState("A");
		sm.addState("B");
		sm.addState("C");
		
		sm.addTransition("A", "foo.bar", "B");
		
		sm.addTransition("B", Pattern.compile("x{3,5}"), "C");
		
		String start = "A";
		String m1 = "foo.bar";
		String m2 = "xxxxxx";
		
		System.out.println(sm.transition(start, m1, m2));
	}
	
	private Map<String,Map<Pattern,String>> transitions;
	private boolean selfDefault;
	
	public StateMachine() { 
		this(false);
	}

	public StateMachine(boolean selfDefault) { 
		transitions = new TreeMap<String,Map<Pattern,String>>();
		this.selfDefault = selfDefault;
	}
	
	public int size() { return transitions.size(); }
	
	public Set<String> states() { return new TreeSet<String>(transitions.keySet()); }
	
	public boolean isReachable(String start, String end) { 
		for(Pattern p : transitions.get(start).keySet()) { 
			if(transitions.get(start).get(p).equals(end)) { 
				return true;
			}
		}
		return false;
	}
	
	public Set<String> reachable(String state) { 
		TreeSet<String> reachSet = new TreeSet<String>();
		for(Pattern p : transitions.get(state).keySet()) { 
			reachSet.add(transitions.get(state).get(p));
		}
		return reachSet;
	}
	
	public void addState(String state) { 
		if(transitions.containsKey(state)) { throw new IllegalArgumentException(state); }
		transitions.put(state, new LinkedHashMap<Pattern,String>());
		if(selfDefault) { 
			addSelfTransition(state);
		}
	}
	
	public void addSelfTransition(String state) { 
		if(!transitions.containsKey(state)) { throw new IllegalArgumentException(state); }
		transitions.get(state).put(Pattern.compile(".*"), state);
	}
	
	public void addTransition(String s1, Pattern p, String s2) {
		if(!transitions.containsKey(s1)) { throw new IllegalArgumentException(s1); }
		if(!transitions.containsKey(s2)) { throw new IllegalArgumentException(s2); }
		if(transitions.get(s1).containsKey(p)) { throw new IllegalArgumentException(p.toString()); }
		
		transitions.get(s1).put(p, s2);
	}
	
	public void addTransition(String s1, String m, String s2) { 
		addTransition(s1, Pattern.compile(escape(m)), s2);
	}
	
	private static final String escapeable = "[]().^$\\{}+*?";
	
	private static String escape(String m) { 
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < m.length(); i++) { 
			char c = m.charAt(i);
			if(escapeable.indexOf(String.valueOf(c)) != -1) { 
				sb.append("\\");
			} 
			sb.append(c);
		}
		return sb.toString();
	}
	
	public String transition(String state, String m1, String m2, String... mRest) throws TransitionException { 
		String current = transition(transition(state, m1), m2);
		for(String m : mRest) { 
			current = transition(current, m);
		}
		return current;
	}
	
	public String transition(String state, String message) throws TransitionException {
		
		if(!transitions.containsKey(state)) { throw new IllegalArgumentException(state); }

		LinkedList<Pattern> transits = new LinkedList<Pattern>(transitions.get(state).keySet());
		Collections.reverse(transits);
		
		for(Pattern p : transits) { 
			Matcher m = p.matcher(message);
			if(m.matches()) { 
				return transitions.get(state).get(p);
			}
		}
		
		throw new TransitionException(state, message);
	}
}


