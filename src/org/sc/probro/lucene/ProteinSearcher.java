package org.sc.probro.lucene;

import java.util.*;
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
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.eclipse.jetty.util.log.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ProteinSearcher {
	
	private Analyzer analyzer;
	private IndexReader reader;
	private IndexSearcher search;
	private Directory dir;
	
	public ProteinSearcher(File indexFile) throws IOException { 
		//analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
		//analyzer = new WhitespaceAnalyzer();
		analyzer = new BioAnalyzer();

		dir = FSDirectory.open(indexFile);
		reader = IndexReader.open(dir, true);
		search = new IndexSearcher(reader);
		System.out.println(String.format("Opened: %s \n\t(# docs: %d)", dir.toString(), reader.numDocs()));
		
	}
	
	public JSONArray evaluate(String query) throws IOException, JSONException { 
		search.getIndexReader().reopen();

		Query q = createPhraseQuery(query);
		HitCollector clt = new HitCollector();
		search.search(q, clt);
		Collection<Document> docs = clt.getDocuments();
		
		LinkedList<JSONObject> objs = new LinkedList<JSONObject>();
		
		//System.out.println(String.format("-- Search Results -----------------"));
		for(Document doc : docs) { 
			objs.add(renderDocumentAsJSON(doc));
		}
		
		return new JSONArray(objs);
	}
	
	public JSONObject renderDocumentAsJSON(Document doc) throws JSONException { 
		JSONObject obj = new JSONObject();
		
		System.out.println("Document");
		List fields = doc.getFields();
		for(Object f : fields) { 
			Field ff = (Field)f;
			//System.out.println(String.format("\t%s=\"%s\"", ff.name(), ff.stringValue()));
		}
		
		String proteinID = doc.getFields("protein-id")[0].stringValue();
		String type = doc.getFields("document-type")[0].stringValue();

		obj.put("id", proteinID);
		obj.put("type", type);
		
		for(Field f : doc.getFields("description")) { 
			obj.append("description", f.stringValue());
		}
		for(Field f : doc.getFields("accession")) { 
			obj.append("accession", f.stringValue());
		}
		
		return obj;
	}
	
	public void close() throws IOException {
		Log.info("Closing ProteinSearcher...");
		search.close();
		reader.close();
		dir.close();
		
		search = null;
		reader = null;
		dir = null;
		//IndexWriter.unlock(dir);
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
			stream = new LowerCaseFilter(stream);
			
			stream.reset();

			while(stream.incrementToken()) { 
				if(stream.hasAttribute(TermAttribute.class)) { 
					TermAttribute termattr = (TermAttribute)stream.getAttribute(TermAttribute.class);
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
	
	// use this method.
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
			stream = new LowerCaseFilter(stream);
			
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
		search.getIndexReader().reopen();
		HitCollector clt = new HitCollector();
		search.search(q, clt);
		return clt.getDocuments();
	}
		
	private class HitCollector extends Collector {

		private Set<Integer> hits;
		private int docBase;
		
		public HitCollector() {
			hits = new TreeSet<Integer>();
		}

		public boolean acceptsDocsOutOfOrder() {
			return true;
		}
		
		public int getCount() { return hits.size(); }
		
		public Collection<Document> getDocuments() throws IOException { 
			LinkedList<Document> docs = new LinkedList<Document>();
			for(Integer n : hits) { 
				docs.add(reader.document(n));
			}
			return docs;
		}

		public void collect(int idx) throws IOException {
			hits.add(idx + docBase);
		}

		public void setNextReader(IndexReader rdr, int docbase)
				throws IOException {
			docBase = docbase;
		}

		public void setScorer(Scorer sc) throws IOException {
		} 
	}
}
