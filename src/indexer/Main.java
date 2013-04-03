package indexer;


public class Main {

	public static void main(String[] args) throws InterruptedException {
		// TODO: Parse cmdline arguments

		Indexer idx = new Indexer("/home/linux/Dokumente/Information Retrieval/20_newsgroups_subset/", true);
		idx.buildIndex(0, 15);
		idx.buildARFF("/var/tmp/test.arff");
	}

}
