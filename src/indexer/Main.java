package indexer;

import java.util.Locale;

import utils.TRECFormater;
import args.ArgumentValidator;


public class Main {

	public static void main(String[] args) throws InterruptedException {
		Locale.setDefault(new Locale("en", "US"));		
		// check arguments

		ArgumentValidator validator = new ArgumentValidator();
		if (!validator.validateArgs(args)) {
			System.exit(1);
		}
	
		Indexer idx;
		idx = new Indexer();	
		
		// Create Index
		if (validator.hasIndexer()) {
			idx.buildIndex(validator.getInput(), validator.getMinThreshold(), 
					validator.getMaxThreshold(), validator.hasStemming());
			idx.buildARFF(validator.getIdexOut());	
		}
		
		// Search
		if (validator.getQuery() != null) {
			if (!validator.hasIndexer()) idx.readFromARFF(validator.getInput());
			// We pass every word of the document as query
			String[] query = null;
			if (validator.isQueryPath()) {
				try {
					Tokenizer tk = new Tokenizer(validator.getQuery());
					query = tk.getTokens();
				}
				catch (Exception e) {
					System.err.println("Cannot read query file.");
					System.exit(1);
				}
			}
			else {
				query = validator.getQuery().split(" ");
			}
			
			TRECFormater.printResult(idx.search(query, validator), validator, 10);
	
		}
		
		// Both search results should be the same ..
		
	}

}
