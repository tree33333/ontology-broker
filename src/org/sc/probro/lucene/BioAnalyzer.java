/*
	Copyright 2010 Massachusetts General Hospital

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	    http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/
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