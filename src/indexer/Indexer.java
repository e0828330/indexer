package indexer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;

import utils.ARFFWriter;
import utils.SortThread;
import utils.Stemmer;
import args.ArgumentValidator;

public class Indexer {
	
	// Logger
	private Logger logger = Logger.getLogger(Indexer.class);
	
	// Used for parallel processing
	private ExecutorService executorService;

	// Output of the map phase i.e (term, doc) pairs
	private ConcurrentHashMap<String, Vector<String>> mapOut;

	// Inverted index gets build during reduce
	private ConcurrentHashMap<String, ArrayList<Posting>> index;

	// Stores the documents as vectors
	private HashMap<String, TreeMap<Integer, Double>> documentVectors;

	// Stores collection frequencies for terms
	private ConcurrentHashMap<String, Integer> cfMap;
	
	// Number of tokens
	private int numTokens;
	
	/*
	 * We use Jelinek-­‐Mercer Smoothing with a small lambda value, because
	 * we mostly have long queries (whole documents).
	 */
	private final double LAMBDA = 0.2;
	
	private ConcurrentHashMap<String, Integer> termIdMap = new ConcurrentHashMap<String, Integer>();
	
	
	private ArrayList<String> docIds = new ArrayList<String>();
	private HashSet<String> classes = new HashSet<String>();
	private int numDocs = 0;
	
	private boolean useStemming;
	
	private int maxThreads = 1;
	
	public Indexer() {
		maxThreads = Runtime.getRuntime().availableProcessors();
	}
	
	/**
	 * Waits for the executorService to execute all queued / running
	 * threads.
	 */
	private void waitForThreads() {
		if (executorService != null) {
			try {
				executorService.shutdown();
				executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
				executorService = null;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Build the index and exclude terms with a tf out of the threshold range
	 * @param targetDirectory 
	 * 
	 * @param minThreshold
	 * @param maxThreshold
	 * @param useStemming 
	 */
	public void buildIndex(String targetDirectory, int minThreshold, int maxThreshold, boolean useStemming) {
		Long startTime = System.currentTimeMillis();
		
		logger.debug("Started indexing process.");
		
		this.useStemming = useStemming;		

		executorService = Executors.newFixedThreadPool(maxThreads);
		mapOut = new ConcurrentHashMap<String, Vector<String>>();

		traverseDir(new File(targetDirectory));

		// Wait for all threads to finish
		waitForThreads();
		
		logger.debug("End of map phase.");

		// Create Posting lists
		index = new ConcurrentHashMap<String, ArrayList<Posting>>();
		executorService = Executors.newFixedThreadPool(maxThreads);

		for(String term : mapOut.keySet()) {
			executorService.execute(new Inverter(mapOut, index, term, minThreshold, maxThreshold));
		}

		// Wait for all threads to finish
		waitForThreads();

		logger.debug("End of reduce phase.");
		
		// Build the document vectors
		documentVectors = new HashMap<String, TreeMap<Integer, Double>>(numDocs, 1.0f);
		int i = 0;
		for (String term : index.keySet()) {
			for (Posting p : index.get(term)) {
				if (!documentVectors.containsKey(p.getDocId())) {
					documentVectors.put(p.getDocId(), new TreeMap<Integer, Double>());
				}
				documentVectors.get(p.getDocId()).put(i, (double) p.getTf());
			}
			termIdMap.put(term, i);
			i++;
		}

		buildCfMap();

		logger.info("Done indexing " + numDocs + " documents in " 
							+ (System.currentTimeMillis() - startTime) + "ms ");
		logger.info("Number of terms: " + index.size());
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
			writer.addAttribute("@hasStemming@", "NUMERIC");
			for (String term : index.keySet()) {
				writer.addAttribute(term, "NUMERIC");
			}
			writer.beginData();
			for (String entry : documentVectors.keySet()) {
				String[] tmp = entry.split("/");
				writer.startRow();
				writer.addConstantValue(true, 0, tmp[0]);
				writer.addStringValue(false, 1, tmp[1]);
				writer.addNumericValue(false, 2, this.useStemming ? 1 : 0);
				TreeMap<Integer, Double> list = documentVectors.get(entry);
				for (Integer idx : list.keySet()) {
					// The first tree entries are class, name and the stemming attributes, so we have
					// to add 3 to the index
					writer.addDoubleValue(false, idx + 3, list.get(idx));
				}
				writer.endRow();
			}
			writer.close();
			logger.info("Wrote document vectors to " + filename);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * Reads the index from the compressed ARFF file
	 * 
	 * @param filename
	 */
	public void readFromARFF(String filename) {
		Long startTime = System.currentTimeMillis();
		try {
			File f = new File(filename);
			if (!f.exists()) {
				System.err.println("ARFF File <" + filename + "> does not exist.");
				System.exit(1);
			}
			GZIPInputStream gzin = new GZIPInputStream(new FileInputStream(f));
			BufferedReader reader = new BufferedReader(new InputStreamReader(gzin));
			String line;
			
			// Initialize maps
			index = new ConcurrentHashMap<String, ArrayList<Posting>>();
			documentVectors = new HashMap<String, TreeMap<Integer, Double>>();
			HashMap<Integer, String> termMap = new HashMap<Integer, String>();
			
			
			boolean inData = false; 
			int  i = 3;
			while ((line = reader.readLine()) != null) {
				// Read the attributes ... we intentionally skip the class and name
				if (!inData && line.startsWith("@ATTRIBUTE")) {
					String[] tmp = line.split(" ");
					if (!tmp[1].equals("\"@documentClass@\"") && !tmp[1].equals("\"@documentName@\"") && !tmp[1].equals("\"@hasStemming@\""))  {
						String term = tmp[1].substring(1, tmp[1].length() - 1);
						index.put(term, new ArrayList<Posting>());
						termMap.put(i, term);
						termIdMap.put(term, i);
						i++;
					}
				}
				// Done with attributes?
				else if (line.startsWith("@DATA")) {
					inData = true;
					continue;
				}
				// Data row: Parse and fill into the maps
				if (inData) {
					String docId;
					line = line.substring(1, line.length() -1);
					String[] attrs = line.split(", ");
					docId = attrs[0].substring(2, attrs[0].length()) + "/" 
							+ attrs[1].substring(3, attrs[1].length() - 1);
					
					useStemming = (attrs[2].substring(2).equals("1")) ? true : false;

					classes.add(attrs[0].substring(2, attrs[0].length()));
					
					
					for (i = 3; i < attrs.length; i++) {
						String[] tmp = attrs[i].split(" ");
						Integer idx = Integer.parseInt(tmp[0]);
						Integer tf = Integer.parseInt(tmp[1]);
						Posting p = new Posting(docId);
						p.setTf(tf);
						index.get(termMap.get(idx)).add(p);
						if (!documentVectors.containsKey(docId)) {
							documentVectors.put(docId, new TreeMap<Integer, Double>());
						}
						documentVectors.get(docId).put(idx - 3, (double) tf);
					}
				}
			}

			reader.close();
			
			// Done reading now sort the posting lists
			executorService = Executors.newFixedThreadPool(maxThreads);
			for(String term: index.keySet()) {
				executorService.execute(new SortThread(index, term));
			}
			// Wait for all threads to finish
			waitForThreads();
			
			// Build cfMap
			buildCfMap();
			
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("Done construcing index from ARFF file in " + (System.currentTimeMillis() - startTime) + "ms ");
	}

	/**
	 * Computes the collection frequency for each term
	 */
	private void buildCfMap() {
		cfMap = new ConcurrentHashMap<String, Integer>();
		executorService = Executors.newFixedThreadPool(maxThreads);
		for (String term : index.keySet()) {
			executorService.execute(new CollectionFrequencyBuilder(cfMap, index.get(term), term));
		}
		// Wait for all threads to finish
		waitForThreads();

		numTokens = 0;
		for (Integer cf : cfMap.values())  {
			numTokens += cf;
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
	 * @param file 
	 */
	public Map<String, Double> search(String[] query, ArgumentValidator validator) {
		HashMap<String, Double> sources = new HashMap<String, Double>();
		HashSet<String> distinctTerms = new HashSet<String>();

		Stemmer stemmer = new Stemmer();

		for (String term : query) {
			term = term.toLowerCase();

			if (useStemming) {
				stemmer.add(term.toCharArray(), term.length());
				stemmer.stem();
				term = stemmer.toString();
			}
			
			distinctTerms.add(term);
		}
		
		// Compute sources
		for (String doc : documentVectors.keySet()) {
			TreeMap<Integer, Double> tfList = documentVectors.get(doc);
			int length = 0;
			/* Get document length (sum of tf values) */
			for (Double tf : tfList.values()) {
				length += tf;
			}
			double pd = -1;
			for (String term : distinctTerms) {
				Integer termId;
				/* Skip terms that do not exist in the collection */
				if ((termId = termIdMap.get(term)) == null) {
					continue;
				}
				
				/* Compute P(t|M_d) */
				Double tf;
				double ptd = -1;
				if ((tf = tfList.get(termId)) != null) {
					if (ptd == -1) {
						ptd = tf / length;

					}
					else {
						ptd *= tf / length;
					}
				}
				
				/* Compute P(t|M_c) */
				Integer cf = cfMap.get(term);
				if (ptd == -1) {
					ptd = 0;
				}
				if (pd == -1) {
					pd = LAMBDA * ptd + (1 - LAMBDA) * (double)cf / (double)numTokens;
				}
				else {
					pd *= LAMBDA * ptd + (1 - LAMBDA) * (double)cf / (double)numTokens;
				}
			}

			if (pd != -1) {
				sources.put(doc, pd);
			}
		}

		// Sort documents by source to get the top 10
		SortedSources ss = new SortedSources(sources);
		TreeMap<String, Double> sorted = new TreeMap<String, Double>(ss);
		sorted.putAll(sources);

		return sorted;

	}

}
