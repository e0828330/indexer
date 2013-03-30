package indexer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

	
	private static ExecutorService executorService;
	private static Hashtable<String, Vector<String>> mapOut;
	private static Hashtable<String, ArrayList<Posting>> index;
	private static int numDocs = 0;
	private static ArrayList<String> docIds = new ArrayList<String>();
	
	/**
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
		executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		mapOut = new Hashtable<String, Vector<String>>();
		index = new Hashtable<String, ArrayList<Posting>>();

		// TODO: This is hardcoded for now, should pass in parent dir and do that per topic
		traverseDir(new File("/home/linux/Dokumente/Information Retrieval/20_newsgroups_subset/"));

		// Wait for all threads to finish
		executorService.shutdown();
		executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		executorService = null;
		
		// Create Posting lists
		executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		
		ArrayList<String> terms = new ArrayList<String>(mapOut.keySet());
		Collections.sort(terms);
		
		for(String term : mapOut.keySet()) {
			executorService.execute(new Inverter(mapOut, index, term));
		}

		// Wait for all threads to finish
		executorService.shutdown();
		executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		executorService = null;

		// Write result to text file for debugging
		try {
			FileWriter fstream = new FileWriter("/tmp/debug.txt");
			BufferedWriter out = new BufferedWriter(fstream);
			for(String term : index.keySet()) {
				out.write(term + " : ");
				for (Posting p : index.get(term)) {
					out.write("(" + p.getDocId() + ", " + p.getTf() + "), ");
				}
				out.write("\n");
			}
			out.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void traverseDir(File currentFile) {
		if (!currentFile.isDirectory()) {
			executorService.execute(new FileIndexer(currentFile, true, mapOut));
			docIds.add(currentFile.getParentFile().getName() + "/" + currentFile.getName());
			numDocs++;
		}
		if (currentFile.list() != null) { 
			for (String fileName : currentFile.list()) {
				traverseDir(new File(currentFile, fileName));
			}
		}
	}


}
