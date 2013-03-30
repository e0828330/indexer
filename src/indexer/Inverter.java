package indexer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

public class Inverter implements Runnable {

	private String term;
	private Hashtable<String, ArrayList<Posting>> index;
	private Hashtable<String, Vector<String>> input;
	
	private int maxThreshold;
	private int minThreshold;
	
	private int numDocs;
	
	/**
	 * Inverter thread run in the "reduce" phase
	 * 
	 * @param input
	 * @param index
	 * @param term
	 * @param minThreshold
	 * @param maxThreshold
	 * @param numDocs
	 */
	public Inverter(Hashtable<String, Vector<String>> input, 
					Hashtable<String, ArrayList<Posting>> index, String term,
					int minThreshold, int maxThreshold, int numDocs) {
		
		this.term = term;
		this.input = input;
		this.index = index;
		this.minThreshold = minThreshold;
		this.maxThreshold = maxThreshold;
		this.numDocs = numDocs;
	}
	
	@Override
	public void run() {
		HashMap<String, Posting> pList = new HashMap<String, Posting>();
		for (String docId : input.get(term)) {
			if (!pList.containsKey(docId)) {
				pList.put(docId, new Posting(docId));
			}
			pList.get(docId).increaseTf();
		}

		ArrayList<Posting> result = new ArrayList<Posting>(pList.size());
		ArrayList<String> docIds = new ArrayList<String>(pList.keySet());
		Collections.sort(docIds);
		for (String docId : docIds) {
			int tf = pList.get(docId).getTf();
			if (tf < minThreshold)
				continue;
			if (maxThreshold != -1 && tf > maxThreshold)
				continue;
			result.add(pList.get(docId));
		}
		
		if (!result.isEmpty()) {
			int idf = numDocs / result.size();
			
			for (Posting p : result) {
				p.setWeight(Math.log10(1 + p.getTf()) * Math.log10(idf));  
			}

			index.put(term, result);
		}
	}

}
