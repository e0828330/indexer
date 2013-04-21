package indexer;

import java.util.ArrayList;
import java.util.Map;

public class CollectionFrequencyBuilder implements Runnable {

	private Map<String, Integer> cfMap;
	private ArrayList<Posting> postingList;
	private String term;
	
	public CollectionFrequencyBuilder(Map<String, Integer> cfMap, ArrayList<Posting> postingList, String term) {
		this.cfMap = cfMap;
		this.postingList = postingList;
		this.term = term;
	}
	
	
	@Override
	public void run() {
		Integer cf = 0;
		for (Posting p : postingList) {
			cf += p.getTf();
		}
		cfMap.put(term, cf);
	}

}
