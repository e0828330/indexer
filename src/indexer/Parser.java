package indexer;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;

import utils.Stemmer;

public class Parser implements Runnable {

	private Map<String, Vector<String>> out;
	private String filename;
	private Stemmer stemmer = new Stemmer();
	private String docId;
	private boolean useStemming;
	
	private Logger logger = Logger.getLogger(Parser.class);
	
	/**
	 * Parser
	 *  
	 * @param file - File to index
	 * @param useStemming - use stemming?
	 * @param out - output hashtable
	 */
	public Parser(File file, boolean useStemming, Map<String, Vector<String>> out) {
		this.out = out;
		this.filename = file.getAbsolutePath();
		this.docId = file.getParentFile().getName() + "/" + file.getName();
		this.useStemming = useStemming;
	}
	
	@Override
	public void run() {
		Tokenizer tk;
		try {
			tk = new Tokenizer(filename);
			for (String word : tk.getTokens()) {
				if (word.length() <= 1) {
					continue; // filter some nonsense
				}
				if (useStemming) {
					word = stem(word);
				}
				word = word.toLowerCase();
				if (!out.containsKey(word)) {
					out.put(word, new Vector<String>());
			}
			out.get(word).add(docId);
		}
		} catch (IOException e) {
			logger.warn("Cannot read file " + filename + " for tokenizer, skipping file!", e);
		}
					
	}

	/**
	 * Returns the stemmed version of @word
	 * 
	 * @param word
	 * @return
	 */
	private String stem(String word) {
		stemmer.add(word.toCharArray(), word.length());
		stemmer.stem();
		return stemmer.toString();
	}
}
