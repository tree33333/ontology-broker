package org.sc.probro.lucene;

import java.util.*;
import java.util.regex.*;
import java.io.IOException;
import java.io.Reader;

public class ProteinRewritingReader extends Reader {
	
	private static Pattern xT = Pattern.compile("(^|[\\s()])(N|C)-?T($|[\\s()])");
	private static Pattern xTerm = Pattern.compile("(^|[\\s()])(N|C)(?:-|\\s)?TERM($|[\\s()])");
	private static Pattern xTerminus = Pattern.compile("(^|[\\s()])(N|C)\\s+TERMINUS($|[\\s()])");
	private static Pattern xTerminal = Pattern.compile("(^|[\\s()])(N|C)(?:-|\\s)?TERMINAL($|[\\s()])");
	
	private static Pattern numbered = Pattern.compile("(^|[(\\s,.])([a-zA-Z]{2,})-(\\d{1,})($|[\\s,.;)])");
	
	public static RewriteRule[] proteinRules = new RewriteRule[] {
		
		new RewriteRule(xT, "%s%s-TERMINUS%s"),
		new RewriteRule(xTerm, "%s%s-TERMINUS%s"),
		new RewriteRule(xTerminus, "%s%s-TERMINUS%s"),
		new RewriteRule(xTerminal, "%s%s-TERMINUS%s"),
		
		new RewriteRule(numbered, "%s%s%s%s"),

		new RewriteRule("AA\\s(\\d+)-(\\d+)", "AA<%s-%s>"),
		
		new RewriteRule("(^|[a-zA-Z0-9])(ALPHA)($|[\\s.,])", "%s-%s%s"),
		new RewriteRule("(^|[a-zA-Z0-9])(BETA)($|[\\s.,])", "%s-%s%s"),
		new RewriteRule("(^|[a-zA-Z0-9])(GAMMA)($|[\\s.,])", "%s-%s%s"),
		new RewriteRule("(^|[a-zA-Z0-9])(DELTA)($|[\\s.,])", "%s-%s%s"),
		new RewriteRule("(^|[a-zA-Z0-9])(EPSILON)($|[\\s.,])", "%s-%s%s"),
		new RewriteRule("(^|[a-zA-Z0-9])(ZETA)($|[\\s.,])", "%s-%s%s"),
		new RewriteRule("(^|[AC-GI-Y0-9])(ETA)($|[\\s.,])", "%s-%s%s"),
		new RewriteRule("(^|[a-zA-Z0-9])(THETA)($|[\\s.,])", "%s-%s%s"),
		new RewriteRule("(^|[a-zA-Z0-9])(IOTA)($|[\\s.,])", "%s-%s%s"),
		new RewriteRule("(^|[a-zA-Z0-9])(KAPPA)($|[\\s.,])", "%s-%s%s"),
		new RewriteRule("(^|[a-zA-Z0-9])(LAMBDA)($|[\\s.,])", "%s-%s%s"),
		new RewriteRule("(^|[a-zA-Z0-9])(MU)($|[\\s.,])", "%s-%s%s"),
		new RewriteRule("(^|[a-zA-Z0-9])(NU)($|[\\s.,])", "%s-%s%s"),
		new RewriteRule("(^|[a-zA-Z0-9])(XI)($|[\\s.,])", "%s-%s%s"),
		new RewriteRule("(^|[a-zA-Z0-9])(OMICRON)($|[\\s.,])", "%s-%s%s"),
		new RewriteRule("(^|[a-zA-Z0-9])(PI)($|[\\s.,])", "%s-%s%s"),
		new RewriteRule("(^|[a-zA-Z0-9])(RHO)($|[\\s.,])", "%s-%s%s"),
		new RewriteRule("(^|[a-zA-Z0-9])(SIGMA)($|[\\s.,])", "%s-%s%s"),
		new RewriteRule("(^|[a-zA-Z0-9])(TAU)($|[\\s.,])", "%s-%s%s"),
		new RewriteRule("(^|[a-zA-Z0-9])(UPSILON)($|[\\s.,])", "%s-%s%s"),
		new RewriteRule("(^|[a-zA-Z0-9])(PHI)($|[\\s.,])", "%s-%s%s"),
		new RewriteRule("(^|[a-zA-Z0-9])(CHI)($|[\\s.,])", "%s-%s%s"),
		new RewriteRule("(^|[a-zA-Z0-9])(PSI)($|[\\s.,])", "%s-%s%s"),
		new RewriteRule("(^|[a-zA-Z0-9])(OMEGA)($|[\\s.,])", "%s-%s%s"),
		
		new ProteinArrayRewriteRule(),
	};
	
	private String value;
	private int index;
	
	private ArrayList<RewriteRule> rules;
	
	public ProteinRewritingReader(Reader r) throws IOException { 
		this(r, proteinRules);
	}
	
	public ProteinRewritingReader(Reader r, RewriteRule... rules) throws IOException {
		this.rules = new ArrayList<RewriteRule>(Arrays.asList(rules));
		StringBuilder sb = new StringBuilder();
		int read = -1;
		while((read = r.read()) != -1) { 
			sb.append((char)read);
		}
		index = 0;
		value = sb.toString().toUpperCase();
		
		int k = 0;
		RewriteRule rule = null;
		while(k < 100 && (rule = findRewrite()) != null) {
			//System.out.println(String.format("Matched: %s\n\tin \"%s\"", rule.pattern.toString(), value));
			value = rule.rewrite(value);
			k++;
		}
	}
	
	private RewriteRule findRewrite() { 
		for(RewriteRule rule : rules) { 
			if(rule.matches(value)) { 
				return rule;
			}
		}
		return null;
	}

	public void close() throws IOException {
		value = null;
		index = -1;
	}

	public int read(char[] cbuf, int off, int len) throws IOException {
		if(value == null || index >= value.length()) { return -1; }
		
		int bufSpace = Math.min(len, cbuf.length-off);
		int readChars = Math.min(bufSpace, value.length()-index);
		
		for(int i = 0; i < readChars; i++) { 
			cbuf[off+i] = value.charAt(index++);
		}
		
		return readChars;
	}

	public static class RewriteRule {
		
		public Matcher matcher;
		public Pattern pattern;
		public String format;
		
		public RewriteRule() { 
			pattern = null;
			format = null;
		}
		
		public RewriteRule(String p, String f) { 
			this(Pattern.compile(p), f);
		}
		
		public RewriteRule(Pattern p, String f) { 
			pattern = p;
			format = f;
		}
		
		public boolean matches(String str) {
			if(pattern == null) { return false; }
			matcher = pattern.matcher(str);
			return matcher.find();
		}
		
		public String rewrite(String str) {
			if(format == null) { return str; }
			String[] array = new String[matcher.groupCount()];
			for(int i = 0; i < array.length; i++){ 
				array[i] = matcher.group(i+1);
			}
			String internal = String.format(format, array);
			return 
				str.substring(0, matcher.start()) + 
				internal + 
				str.substring(matcher.end(), str.length());
		}
	}
}

class ProteinArrayRewriteRule extends ProteinRewritingReader.RewriteRule {
	
	public Pattern arrayPattern =
		Pattern.compile(
				"(^|[\\s.,])" +
				"([a-zA-Z.]+)" +
				"\\s*(-?)\\s*" +
				"(\\d{1,2})" +
				"(" +
				"(?:" +
				"\\s*,?\\s*" +
				"(?:-|/)\\s*\\d{1,2}" +
				")+" +
				")" +
				"($|[\\s,.])");
	
	private Matcher m;
	
	public ProteinArrayRewriteRule() { 
		super();
	}
	
	public boolean matches(String str) {
		boolean found = (m = arrayPattern.matcher(str)).find();
		if(found) { 
			int s = m.start(), e = m.end();
			//System.out.println(String.format("%d-%d \"%s\"", s, e, str));
		}
		return found;
	}
	
	public String rewrite(String str) { 
		String leader = m.group(1), main = m.group(2),
		 	splitter = m.group(3), first = m.group(4),
		 	array = m.group(5), trailer = m.group(6);
		
		StringBuilder internal = new StringBuilder(String.format("%s-%s", main, first));
		String[] a = array.split("[\\s-\\\\/,]");
		for(int i = 0; i < a.length; i++) {
			if(a[i].length() > 0) {
				//System.out.println(String.format("\t\"%s\" -> \"%s\"", array, a[i]));
				internal.append(String.format(" %s-%s", main, a[i]));
			}
		}
		
		return str.substring(0, m.start()) 
			+ leader + internal.toString() + trailer + 
			str.substring(m.end(), str.length());
		
	}
}
