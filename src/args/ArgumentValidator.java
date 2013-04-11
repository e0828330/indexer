package args;


import java.io.File;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class ArgumentValidator {
	
	private Logger logger = Logger.getLogger(ArgumentValidator.class);

	@Option (name = "-indexer", required = false, usage = "-indexer")
	private boolean indexer = false;
	
	@Option (name = "-min", required = false, usage = "-min MIN")
	private int minThreshold = 0;
	
	@Option (name = "-max", required = false, usage = "-max MAX")
	private int maxThreshold = -1;
	
	@Option (name = "-i",  required = true, usage = "-i <INPUT>")
	private String input = null;
	
	private String output = null;	
	
	private String query = null;
	
	@Option (name = "-stemming", required = false, usage = "[-stemming]")
	private boolean stemming = false;	
	
	private boolean checkedQuery = false;
	
	private boolean isQueryPath = false;

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
		if (this.output == null) {
			System.out.println("output invalid.");
			usage();
			return false;
		}
		if (!indexer && this.query == null) {
			System.err.println("If -indexer option is not set, a query is required.");
			usage();
			return false;			
		}
				
		
		// Debug information
		logger.debug("Set -indexer to " + (indexer ? "true" : "false"));
		
		logger.debug("Set -min to " + minThreshold + ".");
		logger.debug("Set -max to " + maxThreshold + ".");
		logger.debug("Set -stemming to " + stemming + ".");
		logger.debug("Set -i to " + input + ".");
		logger.debug("Set -o to " + output + ".");
		logger.debug("Set -q to " + query + ".");
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

	public String getOutput() {
		return output;
	}

	@Option (name = "-o", required = true, usage = "-o <OUTPUTPATH/FILENAME.arff.gz>")
	public void setOutput(String output) {
		if (!output.contains("arff.gz")) this.output = null;
		else this.output = output;
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
		this.checkedQuery = false;
		File file = new File(query);
		if (file.isFile()) {
			isQueryPath = true;
		}
		else {
			isQueryPath = false;
		}
		checkedQuery = true;
	}	

	private void usage() {
		System.err.println("This program has the following options:\n" +
				"[-indexer] : Indexes the collection.\n" +
				"[-min MIN] : Sets the minimum threshold (default 0).\n" +
				"[-max MAX] : Sets the maximum threshold (default -1 = unlimited)\n" +
				"[-stemming] : Enables stemming.\n" + 
				"-i <path> : The input path to the collection.\n" +
				"-o <path> : The output arff.gz file.\n" +
				"[-q (<path>|query)] : The path to the query file or the query itself.");
	}
	
	public boolean hasStemming() {
		return stemming;
	}

	public void setStemming(boolean stemming) {
		this.stemming = stemming;
	}
	
}

