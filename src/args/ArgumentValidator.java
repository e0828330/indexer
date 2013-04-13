package args;


import java.io.File;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class ArgumentValidator {
	
	public static String SIZE_NONE = "none";
	public static String SIZE_SMALL = "small";
	public static String SIZE_MEDIUM = "medium";
	public static String SIZE_LARGE = "large";
	
	private Logger logger = Logger.getLogger(ArgumentValidator.class);

	@Option (name = "-indexer", required = false)
	private boolean indexer = false;
	
	@Option (name = "-min", required = false)
	private int minThreshold = 0;
	
	@Option (name = "-max", required = false)
	private int maxThreshold = -1;
	
	@Option (name = "-i",  required = true)
	private String input = null;
	
	@Option (name = "-idxout", required = false)	
	private String idxout = null;
	
	@Option (name = "-searchout", required = false)	
	private String searchOut = null;		
	
	private String query = null;
	
	@Option (name = "-stemming", required = false)
	private boolean stemming = false;	
	
	private boolean isQueryPath = false;
	
	private String listSize = ArgumentValidator.SIZE_NONE;
	
	@Option (name = "-t", required = false, usage = "[-t X]")
	private int topicNumber = 0;
	
	@Argument
	private ArrayList<String> all = new ArrayList<>();
	
	public boolean validateArgs(String[] args) {
		
		CmdLineParser parser = new CmdLineParser(this);
		
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			usage();
			return false;
		}
		
		// Threshold check
		if (maxThreshold != -1 && maxThreshold < minThreshold) {
			usage();
			return false;
		}
		
		// Input Output Query check
		if (this.input == null) {
			System.out.println("input invalid.");
			usage();
			return false;
		}
		if (!indexer && this.query == null) {
			System.err.println("If -indexer option is not set, a query is required.");
			usage();
			return false;			
		}
		
		// Check inputfile if no -index is given
		if (!indexer) {
			if (!input.endsWith("arff.gz")) {
				System.err.println("Input must be an arff.gz file.");
				usage();
				return false;				
			}
			if (!input.endsWith("arff.gz")) {
				File file = new File(input);
				if (!file.isFile()) {
					System.err.println("If -indexer option is not set, a query is required.");
					usage();
					return false;					
				}
			}
		}
		else {
			if (idxout == null || !idxout.endsWith("arff.gz")) {
				System.err.println("Please set the indexer output file (FILE.arff.gz).");
				usage();	
				return false;
			}
		}
		
				
		
		// Debug information
		logger.debug("Set -indexer to " + (indexer ? "true" : "false"));
		
		logger.debug("Set -min to " + minThreshold + ".");
		logger.debug("Set -max to " + maxThreshold + ".");
		logger.debug("Set -stemming to " + stemming + ".");
		logger.debug("Set -i to " + input + ".");
		logger.debug("Set -idxout to " + idxout + ".");
		logger.debug("Set -q to " + query + ".");
		logger.debug("Set -lsize to " + listSize + ".");
		logger.debug("Set -t to " + topicNumber + ".");
		logger.debug("Query type is " + (this.isQueryPath ? "a path to query document." : "a direct query input."));

		
		return true;
	}
	
	public boolean hasIndexer() {
		return indexer;
	}

	public void setIndexer(boolean indexer) {
		this.indexer = indexer;
	}

	public int getMinThreshold() {
		return minThreshold;
	}

	public void setMinThreshold(int minThreshold) {
		this.minThreshold = minThreshold;
	}

	public int getMaxThreshold() {
		return maxThreshold;
	}

	public void setMaxThreshold(int maxThreshold) {
		this.maxThreshold = maxThreshold;
	}

	public String getInput() {
		return input;
	}

	public void setInput(String input) {
		this.input = input;
	}

	public String getIdexOut() {
		return idxout;
	}

	public void setIdexOut(String output) {
		this.idxout = output;
	}
	
	
	public String getQuery() {
		return query;
	}

	public boolean isQueryPath() {
		return isQueryPath;
	}

	@Option (name = "-q", required = false, usage = "-q (<PATH>|QUERY)")	
	public void setQuery(String query) {
		this.query = query;
		File file = new File(query);
		if (file.isFile()) {
			isQueryPath = true;
		}
		else {
			isQueryPath = false;
		}
	}	

	public String getListSize() {
		return listSize;
	}

	@Option (name = "-lsize", required = false, usage = "[-lsize (none|small|medium|large)]")	
	public void setListSize(String listSize) {
		if (listSize != ArgumentValidator.SIZE_NONE ||
			listSize != ArgumentValidator.SIZE_SMALL ||
			listSize != ArgumentValidator.SIZE_MEDIUM ||
			listSize != ArgumentValidator.SIZE_LARGE) {
			System.err.println("-lsize not of type (none|small|medium|large). Default set to none.");
			this.listSize = ArgumentValidator.SIZE_NONE;
		}
		this.listSize = listSize;
	}

	public int getTopicNumber() {
		return topicNumber;
	}

	public void setTopicNumber(int topicNumber) {
		this.topicNumber = topicNumber;
	}

	private void usage() {
		System.err.println("This program has the following options:\n" +
				"[-indexer] : Indexes the collection.\n" +
				"[-min MIN] : Sets the minimum threshold (default 0).\n" +
				"[-max MAX] : Sets the maximum threshold (default -1 = unlimited)\n" +
				"[-stemming] : Enables stemming.\n" + 
				"[-q (<path>|query)] : The path to the query file or the query itself.\n" +
				"[-lsize (none|small|medium|large)] :  Sets the list size. (default none)\n" + 
				"[-t X] : Sets the topic number. (default 0) \n" +
				"[-searchout <path>] : The search output (TREC) file.\n" + 
				"-i <path> : The input path to the collection.\n" +
				"-idxout <path> : The indexer output file (arff.gz).\n"
				);
	}
	
	public boolean hasStemming() {
		return stemming;
	}

	public void setStemming(boolean stemming) {
		this.stemming = stemming;
	}

	public String getSearchOut() {
		return searchOut;
	}

	public void setSearchOut(String searchout) {
		this.searchOut = searchout;
	}
	
	
}

