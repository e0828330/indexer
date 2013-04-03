package indexer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import utils.ARFFWriter;

public class Indexer {

	private String targetDirectory;
	
	// Used for parallel processing
	private ExecutorService executorService;

	// Output of the map phase i.e (term, doc) pairs
	private Hashtable<String, Vector<String>> mapOut;
	
	// Inverted index gets build during reduce
	private Hashtable<String, ArrayList<Posting>> index;
	
	// Stores the documents as vectors
	private HashMap<String, TreeMap<Integer, Double>> documentVectors;

	private ArrayList<String> docIds = new ArrayList<String>();
	private HashSet<String> classes = new HashSet<String>();
	private int numDocs = 0;
	
	public Indexer(String targetDirectory) {
		this.targetDirectory = targetDirectory;
	}
	
	/**
	 * Build the index and exclude terms with a tf out of the threshold range
	 * 
	 * @param minThreshold
	 * @param maxThreshold
	 * @throws InterruptedException
	 */
	public void buildIndex(int minThreshold, int maxThreshold) throws InterruptedException {
		
		Long startTime = System.currentTimeMillis();
		int maxThreads = Runtime.getRuntime().availableProcessors();
		
		executorService = Executors.newFixedThreadPool(maxThreads);
		mapOut = new Hashtable<String, Vector<String>>();
		index = new Hashtable<String, ArrayList<Posting>>();

		traverseDir(new File(targetDirectory));

		// Wait for all threads to finish
		executorService.shutdown();
		executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		executorService = null;
		
		// Create Posting lists
		executorService = Executors.newFixedThreadPool(maxThreads);

		for(String term : mapOut.keySet()) {
			executorService.execute(new Inverter(mapOut, index, term, minThreshold, maxThreshold, numDocs));
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
					out.write("(" + p.getDocId() + ", " + p.getTf() + ", " + p.getWeight() + "), ");
				}
				out.write("\n");
			}
			out.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Build the document vectors
		documentVectors = new HashMap<String, TreeMap<Integer, Double>>();
		int i = 0;
		for (String term : index.keySet()) {
			for (Posting p : index.get(term)) {
				if (!documentVectors.containsKey(p.getDocId())) {
					documentVectors.put(p.getDocId(), new TreeMap<Integer, Double>());
				}
				documentVectors.get(p.getDocId()).put(i, p.getWeight());
			}
			i++;
		}
		
		System.out.println("Done indexing " + numDocs + " documents in " 
							+ (System.currentTimeMillis() - startTime) + "ms ");
		System.out.println("Number of terms: " + index.size());
		
	}

	/**
	 * Traverse the target directory and start
	 * queue a parser run for each document we find.
	 * 
	 * While doing that take the opportunity and count the documents,
	 * and build the docId and classes lists.
	 * 
	 * @param currentFile
	 */
	private void traverseDir(File currentFile) {
		if (!currentFile.isDirectory()) {
			executorService.execute(new Parser(currentFile, true, mapOut));
			docIds.add(currentFile.getParentFile().getName() + "/" + currentFile.getName());
			classes.add(currentFile.getParentFile().getName()); //build a list of classes
			numDocs++;
		}
		if (currentFile.list() != null) { 
			for (String fileName : currentFile.list()) {
				traverseDir(new File(currentFile, fileName));
			}
		}
	}

	/**
	 * Create an ARFF file containing the index as document vectors
	 * 
	 * @param filename
	 */
	public void buildARFF(String filename) {
		try {
			ARFFWriter writer = new ARFFWriter(filename, "index");
			String docClasses = "{";
			boolean first = true;
			for (String className : classes) {
				if (first) {
					docClasses += className;
					first = false;
				}
				else {
					docClasses += ", " + className;
				}
			}
			writer.addAttribute("@documentClass@", docClasses + "}");
			writer.addAttribute("@documentName@", "STRING");
			for (String term : index.keySet()) {
				writer.addAttribute(term, "NUMERIC");
			}
			writer.beginData();
			for (String entry : documentVectors.keySet()) {
				String[] tmp = entry.split("/");
				writer.startRow();
				writer.addConstantValue(true, 0, tmp[0]);
				writer.addStringValue(false, 1, tmp[1]);
				TreeMap<Integer, Double> list = documentVectors.get(entry);
				for (Integer idx : list.keySet()) {
					// The first two entries are class and name, so we have
					// to add 2 to the index
					writer.addDoubleValue(false, idx + 2, list.get(idx));
				}
				writer.endRow();
			}
			writer.close();
			System.out.println("Wrote " + filename);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
