package indexer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.instance.NonSparseToSparse;

public class Indexer {

	private String targetDirectory;
	
	// Used for parallel processing
	private ExecutorService executorService;

	// Output of the map phase i.e (term, doc) pairs
	private Hashtable<String, Vector<String>> mapOut;
	
	// Inverted index gets build during reduce
	private Hashtable<String, ArrayList<Posting>> index;
	
	// Stores the documents as vectors
	private HashMap<String, HashMap<Integer, Double>> documentVectors;

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
		documentVectors = new HashMap<String, HashMap<Integer, Double>>();
		int i = 0;
		for (String term : index.keySet()) {
			for (Posting p : index.get(term)) {
				if (!documentVectors.containsKey(p.getDocId())) {
					documentVectors.put(p.getDocId(), new HashMap<Integer, Double>());
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
			executorService.execute(new Parser(currentFile, false, mapOut));
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
		Instances data;
		FastVector attributes = new FastVector();
		FastVector docClasses = new FastVector(classes.size());

		for (String className : classes) {
			docClasses.addElement(className);
		}
		attributes.addElement(new Attribute("documentClass", docClasses));
		attributes.addElement(new Attribute("documentName", (FastVector) null));
		
		for (String term : index.keySet()) {
			attributes.addElement(new Attribute(term));
		}
		
		data = new Instances("index", attributes, 0);
		double[] values;
		for (String entry : documentVectors.keySet()) {
			values = new double[data.numAttributes()];
			String[] tmp = entry.split("/");
			values[0] = docClasses.indexOf(tmp[0]);
			values[1] = data.attribute(1).addStringValue(tmp[1]);
			HashMap<Integer, Double> list = documentVectors.get(entry);
			for (Integer idx : list.keySet()) {
				// The first two entries are class and name, so we have
				// to add 2 to the index
				values[idx + 2] = list.get(idx);
			}
			data.add(new Instance(1.0, values));
		}
		
		
		try {
			NonSparseToSparse nonSparseToSparseInstance = new NonSparseToSparse(); 
			nonSparseToSparseInstance.setInputFormat(data);
			Instances sparseDataset = Filter.useFilter(data, nonSparseToSparseInstance);

			//System.out.println(sparseDataset);
			
			ArffSaver arffSaverInstance = new ArffSaver(); 
			arffSaverInstance.setInstances(sparseDataset); 
			arffSaverInstance.setFile(new File(filename));
			arffSaverInstance.writeBatch();

			System.out.println("Wrote " + filename);

			// TODO: gzip compress the file
		} catch (Exception e) {
			e.printStackTrace();
		}

		Tokenizer tk = new Tokenizer("/home/linux/Dokumente/Information Retrieval/20_newsgroups_subset/misc.forsale/76057");
		get_similar_docs(tk.getTokens());

	}

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

	private void get_similar_docs(String[] query) {
		HashMap<String, Double> sources = new HashMap<String, Double>();

		for (String term : query) {
			term = term.toLowerCase();
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

		for (String docId : sources.keySet()) {
			if (sources.containsKey(docId)) {
				sources.put(docId, sources.get(docId) / documentVectors.get(docId).size());
			}
		}

		SortedSources ss = new SortedSources(sources);
		TreeMap<String, Double> sorted = new TreeMap<String, Double>(ss);
		sorted.putAll(sources);

		int i = 0;
		for (String doc : sorted.keySet()) {
			if (i == 10)
				break;
			System.out.printf("topic1 Q0 %s %d %.2f group1_medium\n", doc, i + 1, sorted.get(doc));
			i++;
		}

	}

}
