package indexer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
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

import utils.ARFFWriter;
import utils.SortThread;
import utils.Stemmer;

public class Indexer {
	
	// Used for parallel processing
	private ExecutorService executorService;

	// Output of the map phase i.e (term, doc) pairs
	private ConcurrentHashMap<String, Vector<String>> mapOut;

	// Inverted index gets build during reduce
	private ConcurrentHashMap<String, ArrayList<Posting>> index;

	// Stores the documents as vectors
	private HashMap<String, TreeMap<Integer, Double>> documentVectors;

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
		this.useStemming = useStemming;		

		executorService = Executors.newFixedThreadPool(maxThreads);
		mapOut = new ConcurrentHashMap<String, Vector<String>>();

		traverseDir(new File(targetDirectory));

		// Wait for all threads to finish
		waitForThreads();

		// Create Posting lists
		index = new ConcurrentHashMap<String, ArrayList<Posting>>();
		executorService = Executors.newFixedThreadPool(maxThreads);

		for(String term : mapOut.keySet()) {
			executorService.execute(new Inverter(mapOut, index, term, minThreshold, maxThreshold, numDocs));
		}

		// Wait for all threads to finish
		waitForThreads();

		// Build the document vectors
		documentVectors = new HashMap<String, TreeMap<Integer, Double>>(numDocs, 1.0f);
		int i = 0;
		for (String term : index.keySet()) {
			for (Posting p : index.get(term)) {
				if (!documentVectors.containsKey(p.getDocId())) {
					documentVectors.put(p.getDocId(), new TreeMap<Integer, Double>());
				}
				documentVectors.get(p.getDocId()).put(i, (double) p.getWeight());
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
					// The first tree entries are class and name, so we have
					// to add 3 to the index
					writer.addDoubleValue(false, idx + 3, list.get(idx));
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
	 * Reads the index from the compressed ARFF file
	 * 
	 * @param filename
	 */
	public void readFromARFF(String filename) {
		Long startTime = System.currentTimeMillis();
		try {
			GZIPInputStream gzin = new GZIPInputStream(new FileInputStream(new File(filename)));
			BufferedReader reader = new BufferedReader(new InputStreamReader(gzin));
			String line;
			
			// Initialize maps
			index = new ConcurrentHashMap<String, ArrayList<Posting>>();
			HashMap<Integer, String> termMap = new HashMap<Integer, String>();
			documentVectors = new HashMap<String, TreeMap<Integer, Double>>();
			
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
						Double w = Double.parseDouble(tmp[1]);
						Posting p = new Posting(docId);
						p.setWeight(w);
						index.get(termMap.get(idx)).add(p);
						if (!documentVectors.containsKey(docId)) {
							documentVectors.put(docId, new TreeMap<Integer, Double>());
						}
						documentVectors.get(docId).put(idx - 3, w);
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
			
			
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Done construcing index from ARFF file in " + (System.currentTimeMillis() - startTime) + "ms ");
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
	 * @throws IOException 
	 */
	public Map<String, Double> search(String[] query, String filepath) throws IOException {
		HashMap<String, Double> sources = new HashMap<String, Double>();

		// Create file 
		FileWriter fstream = new FileWriter(filepath);
		BufferedWriter out = new BufferedWriter(fstream);

		
		/*HashSet<String> terms = new HashSet<String>(Arrays.asList(query));*/
				
		Stemmer stemmer = new Stemmer();
		
		// Compute sources
		for (String term : query) {
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
		
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(2);
		
		int i = 0;
		for (String doc : sorted.keySet()) {
			if (i == 10)
				break;
			//System.out.printf("topic1 Q0 %s %d %.2f group1_medium\n", doc, i + 1, sorted.get(doc));
			out.write("topic1 Q0 " + doc + " " + (i+1) + " " + df.format(sorted.get(doc)) + " group1_medium\n");
			i++;
		}
		out.close();
		return sorted;

	}

}
