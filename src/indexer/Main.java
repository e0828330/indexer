package indexer;

import args.ArgumentValidator;


public class Main {

	public static void main(String[] args) throws InterruptedException {
		// check arguments

		ArgumentValidator validator = new ArgumentValidator();
		if (!validator.validateArgs(args)) {
			System.exit(1);
		}
	
		Indexer idx;
		idx = new Indexer();		
		if (validator.hasIndexer()) {
			idx.buildIndex(validator.getInput(), validator.getMinThreshold(), 
					validator.getMaxThreshold(), validator.hasStemming());
			idx.buildARFF(validator.getOutput());	
		}
		
		idx.readFromARFF(validator.getOutput());
		
		// We pass every word of the document as query
		String[] query = null;
		if (validator.isQueryPath()) {
			Tokenizer tk = new Tokenizer(validator.getQuery());
			query = tk.getTokens();
		}
		else {
			query = validator.getQuery().split(" ");
		}
		idx.search(query);			
		
		// Both search results should be the same ..
		
	}

}
