package indexer;


public class Main {

	public static void main(String[] args) throws InterruptedException {
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
		
		Indexer idx = new Indexer("/home/linux/Dokumente/Information Retrieval/20_newsgroups_subset/", false);
		idx.buildIndex(2, 15);
		idx.buildARFF("/tmp/test.arff.gz");
		
		// We pass every word of the document as query
		Tokenizer tk = new Tokenizer("/home/linux/Dokumente/Information Retrieval/20_newsgroups_subset/misc.forsale/76057");
		idx.get_similar_docs(tk.getTokens());
		
		System.out.println("---");
		
		idx = new Indexer("/home/linux/Dokumente/Information Retrieval/20_newsgroups_subset/", false);
		idx.readFromARFF("/tmp/test.arff.gz");
		
		// We pass every word of the document as query
		tk = new Tokenizer("/home/linux/Dokumente/Information Retrieval/20_newsgroups_subset/misc.forsale/76057");
		idx.get_similar_docs(tk.getTokens());
		
		// Both search results should be the same ..
		
	}

}
