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
import org.sc.probro.BrokerProperties;
import org.sc.probro.data.Metadata;
import org.sc.probro.data.Request;

public class IndexCreator {
	
	public static void main(String[] args) throws IOException { 
		BrokerProperties ps = new BrokerProperties();
		File f = new File(ps.getLuceneIndex());

		IndexCreator creator = new IndexCreator(f);
		creator.close();
	}
	
	private File indexFile;
	private Directory dir;
	private Analyzer analyzer;
	private IndexWriter writer;
	//private IndexReader reader;
	//private IndexSearcher searcher;

	public IndexCreator(File f) throws IOException {
		indexFile = f;  
		analyzer = new BioAnalyzer();

		dir = FSDirectory.open(indexFile);
		
		//IndexWriter.unlock(dir);		
		Log.info(String.format("Creating Lucene Writer: %s", indexFile.toString()));
		writer = new IndexWriter(dir, analyzer, IndexWriter.MaxFieldLength.LIMITED);
		
		//reader = IndexReader.open(dir, true);
		//searcher = new IndexSearcher(reader);
	}
	
	public void checkpoint() throws IOException { 
		writer.commit();
		//writer.optimize();
	}

	public void close() throws IOException  {
		Log.info("CLOSING IndexCreator...");
		checkpoint();
		
		writer.close();
		dir.close();
		//searcher.close();
	}
	
	public void addTerm(String id, String type, Set<String> accessions, Set<String> descriptions) throws IOException { 
		Document doc = new Document();

		doc.add(new Field("protein-id", id, 
				Field.Store.YES, 
				Field.Index.NOT_ANALYZED, 
				Field.TermVector.NO));
		
		doc.add(new Field("document-type", type, 
				Field.Store.YES, 
				Field.Index.NOT_ANALYZED, 
				Field.TermVector.NO));
		
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
	}
}
