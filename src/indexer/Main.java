package indexer;


public class Main {

	public static void main(String[] args) throws InterruptedException {
		// TODO: Parse cmdline arguments

		Indexer idx = new Indexer("/home/linux/Dokumente/Information Retrieval/20_newsgroups_subset/");
		idx.buildIndex(2, 15);
		idx.buildARFF("/tmp/test.arff");
	}

}
