package indexer;

public class Posting {

	private String docId;
	private int tf = 0;
	private double weight = 0.; // TF-IDF
	
	public Posting(String docId) {
		this.setDocId(docId);
	}
	
	public int getTf() {
		return tf;
	}

	public void setTf(int tf) {
		this.tf = tf;
	}
	
	public void increaseTf() {
		this.tf++;
	}
	
	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public String getDocId() {
		return docId;
	}

	public void setDocId(String docId) {
		this.docId = docId;
	}
	
}
