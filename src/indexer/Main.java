package indexer;

import java.io.File;
import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

	
	private static ExecutorService executorService;
	private static Hashtable<String, Vector<String>> out;
	
	/**
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
		executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		out = new Hashtable<String, Vector<String>>();

		// TODO: This is hardcoded for now, should pass in parent dir and do that per topic
		traverseDir(new File("/home/linux/Dokumente/Information Retrieval/20_newsgroups_subset/alt.atheism"));

		// Wait for all threads to finish
		executorService.shutdown();
		executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		
		// Just print out the posting lists
		// TODO: Compress them
		// TODO: weighting
		// TODO: sort
		for(String word : out.keySet()) {
			System.out.print(word + ": ");
			for (String doc : out.get(word)) {
				System.out.print(doc + ", ");
			}
			System.out.print("\n");
		}
	}
	
	public static void traverseDir(File currentFile) {
		if (!currentFile.isDirectory()) {
			executorService.execute(new FileIndexer(currentFile, true, out));
		}
		if (currentFile.list() != null) { 
			for (String fileName : currentFile.list()) {
				traverseDir(new File(currentFile, fileName));
			}
		}
	}


}
