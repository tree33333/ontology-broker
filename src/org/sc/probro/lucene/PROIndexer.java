package org.sc.probro.lucene;

import java.util.*;
import java.util.regex.*;

import java.io.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.eclipse.jetty.util.log.Log;
import org.sc.probro.data.Metadata;
import org.sc.probro.data.Request;

public class PROIndexer {
	
	private File indexFile;
	private Analyzer analyzer;
	private IndexWriter writer;
	private IndexReader reader;
	private IndexSearcher searcher;

	public PROIndexer(File f) throws IOException {
		indexFile = f;  
		analyzer = new BioAnalyzer();

		Directory dir = FSDirectory.open(indexFile);
		Log.info("Creating IndexWriter in PROIndexer...");
		writer = new IndexWriter(dir, analyzer, IndexWriter.MaxFieldLength.LIMITED);
		reader = IndexReader.open(dir, true);
		searcher = new IndexSearcher(reader);
	}

	public void close() throws IOException  {
		writer.close();
		searcher.close();
	}
	
	public void updateProtein(
			String protein, 
			Collection<String> newDescriptions, 
			Collection<String> newAccessions) throws IOException {

		reader.reopen();
		Term t = new Term("protein-id", protein);

		TopDocs top = searcher.search(new TermQuery(t), 1);
		if(top.scoreDocs.length == 0) { throw new IllegalArgumentException(protein); }
		
		int n = top.scoreDocs[0].doc;
		Document doc = reader.document(n);

		for(String description : newDescriptions) { 
			doc.add(new Field("description", description, 
					Field.Store.YES, 
					Field.Index.ANALYZED, 
					Field.TermVector.YES));		
		}

		for(String acc : newAccessions) { 
			Field f = new Field("accession", acc, 
					Field.Store.YES,
					Field.Index.NOT_ANALYZED, 
					Field.TermVector.NO);
			doc.add(f);
		}

		writer.updateDocument(t, doc);
		writer.commit();
	}
	
	public void deleteQuery(String protein) throws IOException { 
		Term id = new Term("protein-id", protein);
		Term type = new Term("type", "query");
		
		BooleanQuery query = new BooleanQuery();
		query.add(new TermQuery(id), BooleanClause.Occur.MUST);
		query.add(new TermQuery(type), BooleanClause.Occur.MUST);

		writer.deleteDocuments(query);
		writer.commit();
	}
	
	public void addQuery(Request req, Collection<Metadata> mds) throws IOException { 
		Document doc = new Document();

		doc.add(new Field("protein-id", String.valueOf(req.request_id), 
				Field.Store.YES, 
				Field.Index.NOT_ANALYZED, 
				Field.TermVector.NO));
		
		doc.add(new Field("document-type", "query", 
				Field.Store.YES, 
				Field.Index.NOT_ANALYZED, 
				Field.TermVector.NO));
		
		Set<String> descriptions = new TreeSet<String>();
		Set<String> accessions = new TreeSet<String>();
		
		descriptions.add(req.search_text);
		descriptions.add(req.context);

		for(Metadata md : mds) { 
			if(md.metadata_key.equals("uniprot")) { 
				String[] array = md.metadata_value.split("[, ]");
				for(String acc : array) { accessions.add(acc); }
			} else { 
				descriptions.add(md.metadata_value);
			}
		}

		for(String description : descriptions) { 
			doc.add(new Field("description", description, 
					Field.Store.YES, 
					Field.Index.ANALYZED, 
					Field.TermVector.YES));		
		}

		for(String acc : accessions) { 
			Field f = new Field("accession", acc, 
					Field.Store.YES,
					Field.Index.NOT_ANALYZED, 
					Field.TermVector.NO);
			doc.add(f);
		}

		writer.addDocument(doc);
		writer.commit();
	}
}
