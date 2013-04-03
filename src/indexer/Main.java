package indexer;


public class Main {

	public static void main(String[] args) throws InterruptedException {
		// TODO: Parse cmdline arguments

		Indexer idx = new Indexer("/home/linux/Dokumente/Information Retrieval/20_newsgroups_subset/", true);
		idx.buildIndex(0, 15);
		idx.buildARFF("/var/tmp/test.arff");
		
		// We pass every word of the document as query
		Tokenizer tk = new Tokenizer("/home/linux/Dokumente/Information Retrieval/20_newsgroups_subset/misc.forsale/76057");
		idx.get_similar_docs(tk.getTokens());
	}

}
