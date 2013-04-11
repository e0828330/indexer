package indexer;

import args.ArgumentValidator;
import args.ArgumentValidator.Options;


public class Main {

	public static void main(String[] args) throws InterruptedException {
		// check arguments

		ArgumentValidator validator = new ArgumentValidator();
		if (!validator.validateArgs(args)) {
			System.exit(1);
		}
	
		
		// TODO: Parse cmdline arguments
		// Need:
		// 1. Build index
		// 1.1 Passing thresholds (and stemming yes/no)
		// 1.2 Passing destination file
		//
		// 2. Search
		// 2.1 Index file (ARFF) generated from 1
		// 2.2 Stemming got used yes/no
		// 2.3 Document to search (file as input)
		// 2.4 Output file
		// 2.5 Search string?
		
		if (validator.getOption() == Options.INDEXER) {
			Indexer idx = new Indexer(validator.getInput(), false);
			idx.buildIndex(validator.getMinThreshold(), validator.getMaxThreshold());
			idx.buildARFF(validator.getOutput());
			
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
						
		}
		

		

		System.out.println("---");
		/*
		idx = new Indexer("/home/linux/Dokumente/Information Retrieval/20_newsgroups_subset/", false);
		idx.readFromARFF("/home/martin/Dokumente/information_retrieval/test.arff.gz");
		
		// We pass every word of the document as query
		tk = new Tokenizer("/home/martin/Dokumente/information_retrieval/20_newsgroups_subset/misc.forsale/76057");
		
		idx.search(new String[]{"crash"});
		
		// Both search results should be the same ..
		*/
	}

}
