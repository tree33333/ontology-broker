package org.sc.probro.lucene;

import java.io.*;

import org.apache.lucene.analysis.*;

public class BiothesaurusAnalyzer extends Analyzer {
	
    public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
    	return tokenStream(fieldName, reader);
    }

    public TokenStream tokenStream(String fieldName, Reader reader) {

    	/*
        TokenStream stream = new PunctuationSplitter(reader);
        stream = new WhitespacingTrimmer(stream);
        stream = new CasingFilter(stream, true);  // -> uppercase.
        stream = new ProteinNameRewriter(stream);
        stream = new TerminusNormalizer(stream);
        */
    	
		try {
			TokenStream stream = new BiothesaurusTokenizer(new ProteinRewritingReader(reader));

			return stream;
		} catch (IOException e) {
			e.printStackTrace(System.err);
			return null;
		}

    }
}