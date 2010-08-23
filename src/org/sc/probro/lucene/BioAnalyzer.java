package org.sc.probro.lucene;

import java.io.*;
import org.apache.lucene.analysis.*;

public class BioAnalyzer extends Analyzer {
	
    public TokenStream reusableTokenStream(
        String fieldName, Reader reader) throws IOException {

    	/*
        SavedStreams streams =
            (SavedStreams) getPreviousTokenStream();

        if (streams == null) {
            streams = new SavedStreams();

            streams.tokenizer = new BioTokenizer(reader);
            //streams.stream = new StandardFilter(streams.tokenizer);
            streams.stream = new LowerCaseFilter(streams.tokenizer);

            setPreviousTokenStream(streams.stream);
            
        } else {
            streams.tokenizer.reset(reader);
        }

        return streams.stream;
        */
    	
    	return tokenStream(fieldName, reader);
    }

    private class SavedStreams {
        Tokenizer tokenizer;
        TokenStream stream;
    }

    public TokenStream tokenStream(
        String fieldName, Reader reader) {

        Tokenizer tokenizer = new BioTokenizer(reader);
        TokenStream stream = null;
        
        //stream = new StandardFilter(tokenizer);
        stream = new LowerCaseFilter(tokenizer); 

        return stream;
    }
}