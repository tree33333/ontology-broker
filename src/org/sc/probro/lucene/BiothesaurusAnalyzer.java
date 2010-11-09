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