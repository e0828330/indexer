package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.Map;

import org.apache.log4j.Logger;

import args.ArgumentValidator;

public class TRECFormater {

		// Logger
		private static Logger logger = Logger.getLogger(TRECFormater.class);
	
		/**
		 * Prints output TREC file to stdout or writes it to a file depending on the
		 * arguments.
		 * 
		 * @param result
		 * @param validator
		 * @param limit
		 */
		public static void printResult(Map<String, Double> result, ArgumentValidator validator, int limit) {
			try {
				FileWriter fstream = null;
				BufferedWriter out = null;
				boolean writeToFile = validator.getSearchOut() == null ? false : true; 
				
				// Create file 
				if (writeToFile) {
					fstream = new FileWriter(new File(validator.getSearchOut()));
					out = new BufferedWriter(fstream);
				}
				
				DecimalFormat df = new DecimalFormat();
				df.setMaximumFractionDigits(2);

				int i = 0;
				for (String doc : result.keySet()) {
					if (i == limit)
						break;
					if (writeToFile) {
						out.write("topic" + validator.getTopicNumber() + " Q0 " + doc + " " + (i+1) + " " + df.format(result.get(doc)) + " group1_" + validator.getListSize()+"\n");
					}
					else {
						System.out.printf("topic%d Q0 %s %d %.2f groupA_%s\n", validator.getTopicNumber(), doc, i + 1, result.get(doc), validator.getListSize());
					}
					i++;
				}
				if (writeToFile) {
					out.close();
					logger.info("Wrote search results to " + validator.getSearchOut());
				}
				
			}
			catch (Exception e) {
				logger.error("Search failed :/", e);
			}
		}
}

