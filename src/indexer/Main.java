package indexer;


public class Main {

	public static void main(String[] args) throws InterruptedException {
		// TODO: Parse cmdline arguments

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
