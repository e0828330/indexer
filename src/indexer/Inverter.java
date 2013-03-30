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
	
	public Inverter(Hashtable<String, Vector<String>> input, 
					Hashtable<String, ArrayList<Posting>> index, String term) {
		
		this.term = term;
		this.input = input;
		this.index = index;
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
			result.add(pList.get(docId));
		}
		index.put(term, result);
	}

}
