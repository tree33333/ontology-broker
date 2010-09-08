package org.sc.probro.lucene;

import java.util.*;

import java.util.regex.*;

import java.io.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class BiothesaurusSearcher {
	
	private Analyzer analyzer;
	private IndexReader reader;
	private IndexSearcher search;
	private UniprotMapping uniprotMapping;
	
	public BiothesaurusSearcher(File indexFile, File uniprotMappingFile) throws IOException { 
		//analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
		//analyzer = new WhitespaceAnalyzer();
		analyzer = new BiothesaurusAnalyzer();

		Directory dir = FSDirectory.open(indexFile);
		reader = IndexReader.open(dir, true);
		search = new IndexSearcher(reader);
		uniprotMapping = new UniprotMapping(uniprotMappingFile);
		System.out.println(String.format("Opened: %s \n\t(# docs: %d)", dir.toString(), reader.numDocs()));
		
	}
	
	private DisjunctionMaxQuery createDisjunction(float tiebreak, Query... qs) { 
		return new DisjunctionMaxQuery(Arrays.asList(qs), tiebreak);
	}
	
	private BooleanQuery createShould(Query... qs) { 
		return createBoolean(BooleanClause.Occur.SHOULD, qs);
	}
	
	private BooleanQuery createMust(Query... qs) { 
		return createBoolean(BooleanClause.Occur.MUST, qs);
	}
	
	private BooleanQuery createBoolean(BooleanClause.Occur type, Query... qs) { 
		BooleanQuery q = new BooleanQuery();
		for(Query qq : qs) { 
			q.add(new BooleanClause(qq, type));
		}
		q.setMinimumNumberShouldMatch(Math.max(1, qs.length-2));
		return q;
	}
	
	private static Pattern leadingNumeric = Pattern.compile("\\d+.*");
	
	private Term[] reorderLeadingNumerics(Term... ts) { 
		int numLeadingNumeric = 0;
		Matcher m = null;
		
		for(int i = 0; i < ts.length && 
			(leadingNumeric.matcher(ts[i].text()).matches() || Greeks.isGreekName(ts[i].text())); 
			i++, numLeadingNumeric++) {
			
			// do nothing.
		}
		
		if(numLeadingNumeric == 0) { return ts; }
		LinkedList<Term> numerics = new LinkedList<Term>();
		LinkedList<Term> rest = new LinkedList<Term>();
		
		for(int i = 0; i < ts.length; i++) {  
			if(i < numLeadingNumeric) { 
				numerics.addLast(ts[i]);
			} else { 
				rest.addLast(ts[i]);
			}
		}
		
		rest.addAll(numerics);
		return rest.toArray(new Term[0]);
	}
	
	private Query createDescendingQuery(Term... ts) {
		ts = reorderLeadingNumerics(ts);
		BooleanQuery bq = new BooleanQuery();

		if(ts.length > 1) { 
			BooleanQuery all = new BooleanQuery();
			float boost = 1024.0f;
			for(int i = 0; i < ts.length; i++, boost /= 2.0f) {
				TermQuery tq = new TermQuery(ts[i]);
				tq.setBoost(boost);
				String termText = ts[i].text();
				BooleanClause.Occur occurs = 
					Greeks.isGreekName(termText) ? BooleanClause.Occur.MUST : 
						BooleanClause.Occur.SHOULD;
				all.add(new BooleanClause(new TermQuery(ts[i]), occurs));
			}

			BooleanQuery forbidden = new BooleanQuery();
			for(int i = 1; i < ts.length; i++) { 
				BooleanQuery pair = new BooleanQuery();
				pair.add(new BooleanClause(new TermQuery(ts[i-1]), BooleanClause.Occur.MUST_NOT));
				pair.add(new BooleanClause(new TermQuery(ts[i]), BooleanClause.Occur.MUST));

				forbidden.add(new BooleanClause(pair, BooleanClause.Occur.SHOULD));
			}

			bq.add(new BooleanClause(all, BooleanClause.Occur.MUST));
			bq.add(new BooleanClause(forbidden, BooleanClause.Occur.MUST_NOT));
		} else if (ts.length == 1) { 
			bq.add(new BooleanClause(new TermQuery(ts[0]), BooleanClause.Occur.SHOULD));
		}
		
		/*
		int count = ts.length;
		for(int i = ts.length-1, w = 1; i >= 0; i--) { 
			TermQuery tq = new TermQuery(ts[i]);
			tq.setBoost((float)w);
			bq.add(new BooleanClause(tq, BooleanClause.Occur.SHOULD));
			if(i < 10) { w *= 2; }
		}
		*/
		return bq;
	}
	
	private PhraseQuery createPhrase(Collection<Term> ts) { 
		return createPhrase(ts.toArray(new Term[0]));
	}
	
	private PhraseQuery createPhrase(Term... ts) { 
		PhraseQuery pq = new PhraseQuery();
		for(Term t : ts) { 
			pq.add(t);
		}
		return pq;
	}
	
	public Collection<String> convertToUniprot(Collection<Document> ds) { 
		Set<String> pros = new TreeSet<String>();
		for(Document d : ds) { pros.add(convertToUniprot(d)); }
		return pros;		
	}
	
	public Collection<String> convertToPRO(Collection<Document> ds) { 
		Set<String> pros = new TreeSet<String>();
		for(Document d : ds) {
			Collection<String> ps = convertToPRO(d);
			if(ps != null) { pros.addAll(ps); } 
		}
		return pros;
	}
	
	public Collection<String> convertToPRO(Document doc) { 
		return uniprotMapping.getProIDs(doc.getField("protein-id").stringValue());
	}

	public String convertToUniprot(Document doc) { 
		return doc.getField("protein-id").stringValue();
	}

	public Map<String,Collection<Document>> collateByPRO(Collection<Document> docs) { 
		Map<String,Collection<Document>> docmap = new LinkedHashMap<String,Collection<Document>>();
		LinkedList<Document> unassigned = new LinkedList<Document>();
		
		for(Document doc : docs) { 
			Collection<String> pros = uniprotMapping.getProIDs(doc.getField("protein-id").stringValue());
			if(pros != null && !pros.isEmpty()) { 
				for(String pro : pros) { 
					if(!docmap.containsKey(pro)) { docmap.put(pro, new LinkedList<Document>()); }
					docmap.get(pro).add(doc);
				}
			} else { 
				unassigned.add(doc);
			}
		}
		docmap.put("unassigned", unassigned);
		return docmap;
	}
	
	public Query createQuery(String phrase) {
		
		phrase = phrase.trim();
		
		TermQuery idQuery = new TermQuery(new Term("protein-id", phrase.toUpperCase()));
		TermQuery accQuery = new TermQuery(new Term("accession", phrase.toUpperCase()));
		
		ArrayList<TermQuery> descQueries = new ArrayList<TermQuery>();
		ArrayList<Term> descTerms = new ArrayList<Term>();
		
		TokenStream stream = analyzer.tokenStream("description", new StringReader(phrase));
		TermAttribute attr = (TermAttribute)stream.getAttribute(TermAttribute.class);
		try {
			stream.reset();
			Term lastTerm = null;
			
			while(stream.incrementToken()) { 
				Term t = new Term("description", attr.term());
				descQueries.add(new TermQuery(t));
				descTerms.add(t);
				
				if(lastTerm != null) { 
					Term hyph = new Term("description", lastTerm.text() + "-" + t.text());
					descQueries.add(new TermQuery(hyph));
					//descTerms.add(hyph);
				}
				lastTerm = t;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return createDisjunction(2.0f,
				createMust(idQuery),
				createMust(accQuery),
				//createShould(descQueries.toArray(new Query[0])));
				createDescendingQuery(descTerms.toArray(new Term[0])));
	}
	
	public Query createTermQuery(String term) { 
		TermQuery t1 = new TermQuery(new Term("protein-id", term.toLowerCase()));
		TermQuery t2 = new TermQuery(new Term("description", term));
		TermQuery t3 = new TermQuery(new Term("accession", term.toLowerCase()));
		BooleanQuery query = new BooleanQuery();
		query.add(t1, BooleanClause.Occur.SHOULD);
		query.add(t2, BooleanClause.Occur.SHOULD);
		query.add(t3, BooleanClause.Occur.SHOULD);
		return query;
		//return t2;
	}
	
	public Query createPhraseQuery(String phrase) throws IOException { 
		Query t1 = new TermQuery(new Term("protein-id", phrase));
		Query t3 = new TermQuery(new Term("accession", phrase));
		//Query t2 = createPhraseQuery("description", phrase);
		
		BooleanQuery t2 = new BooleanQuery();
		String[] tokens = tokenize(phrase);
		for(String token : tokens) { 
			t2.add(new TermQuery(new Term("description", token)), BooleanClause.Occur.MUST);
		}

		/*
		LinkedList termlist = new LinkedList();
		for(String token : tokenize(phrase)) { 
			termlist.add(token);
		}
		Query t2 = new DisjunctionMaxQuery(termlist, 0.0f);
		*/

		BooleanQuery query = new BooleanQuery();
		query.add(t1, BooleanClause.Occur.SHOULD);
		query.add(t2, BooleanClause.Occur.SHOULD);
		query.add(t3, BooleanClause.Occur.SHOULD);
		return query;		
	}
	
	public String[] tokenize(String input) { 
		ArrayList<String> tokens = new ArrayList<String>();
		try { 
			TokenStream stream = analyzer.tokenStream(null, new StringReader(input));
			TermAttribute termattr = (TermAttribute)stream.getAttribute(TermAttribute.class);
			//stream = new LowerCaseFilter(stream);
			
			stream.reset();

			while(stream.incrementToken()) { 
				if(stream.hasAttribute(TermAttribute.class)) { 
					String term = termattr.term();
					tokens.add(term);
				}
			}

			stream.end();
			stream.close();

		} catch(IllegalArgumentException e) { 
			System.err.println(String.format("Phrase: \"%s\"", input));
			e.printStackTrace(System.err);
		} catch (IOException e) {
			System.err.println(String.format("Phrase: \"%s\"", input));
			e.printStackTrace();
		}
		
		return tokens.toArray(new String[0]);
	}
	
	public Query createPhraseQuery(String field, String phrase) throws IOException { 
		PhraseQuery query = new PhraseQuery();
		/*
		String[] array = phrase.split("\\s+");
		for(int i = 0; i < array.length; i++) { 
			query.add(new Term(field, array[i]));
		}
		*/

		try { 
			TokenStream stream = analyzer.tokenStream(field, new StringReader(phrase));
			//stream = new LowerCaseFilter(stream);
			
			stream.reset();

			while(stream.incrementToken()) { 
				if(stream.hasAttribute(TermAttribute.class)) { 
					TermAttribute termattr = (TermAttribute)stream.getAttribute(TermAttribute.class);
					Term t = new Term(field, termattr.term());
					query.add(t);
				}
			}

			stream.end();
			stream.close();

		} catch(IllegalArgumentException e) { 
			e.printStackTrace(System.err);
			System.err.println(String.format("Phrase: \"%s\"", phrase));
		}
		
		return query;
	}
	
	public Collection<Document> search(Query q) throws IOException {
		//System.out.println(String.format("Search: \"%s\"", query.toString()));
		//BioThesaurusCollector clt = new BioThesaurusCollector();
		
		TopDocsCollector clt = TopScoreDocCollector.create(25, true);
		
		search.search(q, clt);
		
		LinkedList<Document> docs = new LinkedList<Document>();
		for(ScoreDoc sdoc : clt.topDocs().scoreDocs) { 
			docs.add(reader.document(sdoc.doc));
		}
		
		//return clt.getDocuments();
		return docs;
	}

	public void list() { 
		for(int i = 0; i < reader.maxDoc(); i++) {
			try { 
				if(!reader.isDeleted(i)) { 
					Document doc = reader.document(i);
					printDocument(doc);
				}
			} catch(IOException e) { 
				e.printStackTrace(System.err);
			}
		}
	}
	
	public static void printDocuments(Collection<Document> ds) { 
		for(Document d : ds) { printDocument(d); }
	}
	
	public String renderDocuments(Collection<Document> ds) { 
		StringBuilder sb = new StringBuilder();
		Set<String> pros = new TreeSet<String>();
		/*
		for(Document d : ds) { 
			if(sb.length() > 0) { sb.append("\n"); }
			sb.append(renderDocument(d));
		}
		*/
		Map<String,Set<Hit>> hitmap = collateHits(ds);
		for(String id : hitmap.keySet()) {
			String inPRO = 
				uniprotMapping.containsUniprotID(id) ? 
				"* " : "  ";
			sb.append(inPRO + id);
			if(uniprotMapping.containsUniprotID(id)) { 
				sb.append(" (");
				for(String pro : uniprotMapping.getProIDs(id)) { 
					sb.append(" "); 
					sb.append(pro);
				}
				sb.append(" )");
			}
			sb.append("\n");
			boolean first = true;
			for(Hit h : hitmap.get(id)) { 
				sb.append(String.format("       \t%s\n", combine(" ", h.accessions)));
				sb.append("\t\t");
				sb.append(combine("\n\t\t", h.descriptions));
				first = false;
			}
			sb.append(hitmap.get(id).isEmpty() ? "" : "\n");
		}
		return sb.toString();
	}
	
	public Map<String,Set<Hit>> collateHits(Collection<Document> docs) { 
		Map<String,Set<Hit>> hitmap = new TreeMap<String,Set<Hit>>();
		for(Document d : docs) {
			Hit h = convertDocument(d);
			if(!hitmap.containsKey(h.id)) {  
				hitmap.put(h.id, new TreeSet<Hit>());
			}
			hitmap.get(h.id).add(h);
		}
		return hitmap;
	}
	
	public Hit convertDocument(Document d) { 
		Hit h = new Hit(d);
		if(uniprotMapping.containsUniprotID(h.id)) { 
			h.pro = uniprotMapping.getProIDs(h.id).toArray(new String[0]);
		}
		return h;
	}
	
	public static String combine(String sep, String... vals) { 
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < vals.length; i++) { 
			if(i > 0) { sb.append(sep); }
			sb.append(vals[i]);
		}
		return sb.toString();
	}
	
	public static class Hit implements Comparable<Hit> {
		
		public String id;
		public String[] accessions, descriptions;
		public String[] pro; 
		
		public Hit(Document d) {
			id = d.getField("protein-id").stringValue();
			Field[] idfield = d.getFields("accession");
			Field[] descfield = d.getFields("description");
			accessions = new String[idfield.length];
			descriptions = new String[descfield.length];
			
			for(int i = 0; i < accessions.length; i++) { accessions[i] = idfield[i].stringValue(); }
			for(int i = 0; i < descriptions.length; i++) { descriptions[i] = descfield[i].stringValue(); }
		}
		
		public String toString() { 
			StringBuilder sb = 
				new StringBuilder(String.format("%s [%s]\n\t%s",
						id, 
						combine(",", accessions),
						combine("\n\t", descriptions)));
			return sb.toString();
		}
		
		public int hashCode() { 
			int code = 17;
			code += id.hashCode(); code *= 37;
			for(int i = 0; i < accessions.length; i++) { 
				code += accessions[i].hashCode(); 
				code *= 37;
			}
			for(int i = 0; i < descriptions.length; i++) { 
				code += descriptions[i].hashCode();
				code *= 37;
			}
			return code;
		}
		
		public boolean equals(Object o) { 
			if(!( o instanceof Hit)) { return false; }
			Hit h = (Hit)o;
			if(!id.equals(h.id)) { return false; }
			if(accessions.length != h.accessions.length) { return false; }
			for(int i = 0; i < accessions.length; i++) { 
				if(!accessions[i].equals(h.accessions[i])) { 
					return false;
				}
			}
			for(int i = 0; i < descriptions.length; i++) { 
				if(!descriptions[i].equals(h.descriptions[i])) { 
					return false;
				}
			}
			return true;
		}
		
		public int compareTo(Hit h) {
			int c = 0;
			c = id.compareTo(h.id);
			if(c != 0) { return c; }
			for(int i = 0; i < Math.min(accessions.length, h.accessions.length); i++) { 
				c = accessions[i].compareTo(h.accessions[i]); 
				if(c != 0) { return c; }
			}
			if(accessions.length != h.accessions.length) { 
				if(accessions.length < h.accessions.length) { return -1; } else { return 1; }
			}
			for(int i = 0; i < Math.min(descriptions.length, h.descriptions.length); i++) { 
				c = descriptions[i].compareTo(h.descriptions[i]); 
				if(c != 0) { return c; }
			}
			if(descriptions.length != h.descriptions.length) { 
				if(descriptions.length < h.descriptions.length) { 
					return -1; 
				} else { 
					return 1; 
				}
			}
			return 0;
		}
	}
	
	public static String renderDocument(Document d) { 
		Field id = d.getField("protein-id");
		Field epitope = d.getField("description");
		String idstr = id != null ? id.stringValue() : "??";
		String epistr = epitope != null ? epitope.stringValue() : "??";
		return String.format("%s \"%s\"", idstr, epistr);
	}
	
	public static void printDocument(Document d) { 
		System.out.println(renderDocument(d));
	}
	
	private class BioThesaurusCollector extends Collector {
		
		private IndexReader nextReader;
		private int base;
		private int count;
		private LinkedList<Integer> docs;
		
		public BioThesaurusCollector() { count = 0; docs = new LinkedList<Integer>(); base = 0; count = 0; }

		public boolean acceptsDocsOutOfOrder() {
			return true;
		}
		
		public int getCount() { return count; }
		
		public Collection<Document> getDocuments() { 
			LinkedList<Document> doclist = new LinkedList<Document>();
			for(Integer n : docs) { 
				try {
					doclist.add(reader.document(n));
				} catch (IOException e) {
					e.printStackTrace(System.err);
				}
			}
			return doclist;
		}

		public void collect(int idx) throws IOException {
			docs.add(base + idx);
			count += 1;
		}

		public void setNextReader(IndexReader rdr, int docbase)
				throws IOException {
			nextReader = reader;
			base = docbase;
		}

		public void setScorer(Scorer sc) throws IOException {
		} 
		
	}
	
	public Map<String,Set<Hit>> hits(String input) { 
		try {
			Collection<Document> docs = search(createPhraseQuery(input));
			return collateHits(docs);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return new TreeMap<String,Set<Hit>>();
	}

	public String evaluate(String inp) {
		inp = inp.trim();
		if(inp.length() == 0) { return ""; }
		Collection<Document> docs;
		try {
			docs = search(createPhraseQuery(inp));
			//docs = search(createTermQuery(inp));
			return renderDocuments(docs);
		} catch (IOException e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}

	public void close() {
		try {
			reader.close();
			search.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
