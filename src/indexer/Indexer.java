package indexer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import utils.ARFFWriter;
import utils.Stemmer;

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
	
	private boolean useStemming;
	
	public Indexer(String targetDirectory, boolean useStemming) {
		this.targetDirectory = targetDirectory;
		this.useStemming = useStemming;
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
			executorService.execute(new Parser(currentFile, useStemming, mapOut));
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
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * This is used for sorting the top 10 documents
	 */
	static class SortedSources implements Comparator<String> {

		Map<String, Double> base;

		public SortedSources(Map<String, Double> base) {
			this.base = base;
		}

		@Override
		public int compare(String a, String b) {
			Double x = base.get(a);
			Double y = base.get(b);
			if (x.equals(y)) {
				return b.compareTo(a);
			}
			return y.compareTo(x);
		}

	}

	/**
	 * Find the 10 most similar documents for a given query
	 * 
	 * @param query
	 */
	public void get_similar_docs(String[] query) {
		HashMap<String, Double> sources = new HashMap<String, Double>();
		HashSet<String> terms = new HashSet<String>(Arrays.asList(query));
		
		Stemmer stemmer = new Stemmer();
		
		// Compute sources
		for (String term : terms) {
			term = term.toLowerCase();

			if (useStemming) {
				stemmer.add(term.toCharArray(), term.length());
				stemmer.stem();
				term = stemmer.toString();
			}

			if (!index.containsKey(term)) {
				continue;
			}

			for (Posting p : index.get(term)) {
				double value = 0.;
				if (sources.containsKey(p.getDocId())) {
					value = sources.get(p.getDocId());
				}
				value += p.getWeight();
				sources.put(p.getDocId(), value);
			}
		}

		// Normalize sources
		for (String docId : sources.keySet()) {
			if (sources.containsKey(docId)) {
				sources.put(docId, sources.get(docId) / documentVectors.get(docId).size());
			}
		}

		// Sort documents by source to get the top 10
		SortedSources ss = new SortedSources(sources);
		TreeMap<String, Double> sorted = new TreeMap<String, Double>(ss);
		sorted.putAll(sources);

		int i = 0;
		for (String doc : sorted.keySet()) {
			if (i == 10)
				break;
			// TODO: Write to file
			System.out.printf("topic1 Q0 %s %d %.2f group1_medium\n", doc, i + 1, sorted.get(doc));
			i++;
		}

	}

}
