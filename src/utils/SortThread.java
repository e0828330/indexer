package utils;

import indexer.Posting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class SortThread implements Runnable {

	private String term;
	private Map<String, ArrayList<Posting>> index;
	
	public SortThread(Map<String, ArrayList<Posting>> index, String term) {
		this.term = term;
		this.index = index;
	}
	
	@Override
	public void run() {
		Collections.sort(index.get(term), new Comparator<Posting>() {
			@Override
			public int compare(Posting a, Posting b) {
				return a.getDocId().compareTo(b.getDocId());
			}
		});
	}

}
