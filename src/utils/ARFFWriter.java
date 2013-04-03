package utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ARFFWriter {

	private FileWriter fstream;
	
	/**
	 * Writes a new (sparse) ARFF file
	 * 
	 * @param filename
	 * @param name
	 * @throws IOException
	 */
	public ARFFWriter(String filename, String name) throws IOException {
		fstream = new FileWriter(new File(filename));
		fstream.write("@RELATION " + name + "\n\n");
		// TODO: gzip the file
	}
	
	/**
	 * Adds a new attribute
	 * 
	 * @param name
	 * @param type
	 * @throws IOException
	 */
	public void addAttribute(String name, String type) throws IOException {
		fstream.write("@ATTRIBUTE \"" + name + "\" " + type + "\n");
	}
	
	/**
	 * Start data section
	 * 
	 * @throws IOException
	 */
	public void beginData() throws IOException {
		fstream.write("@DATA\n");
	}
	
	/**
	 * Start new row
	 * @throws IOException
	 */
	public void startRow() throws IOException {
		fstream.write("{");
	}
	
	/**
	 * Adds a new double value
	 * 
	 * @param first
	 * @param index
	 * @param value
	 * @throws IOException
	 */
	public void addDoubleValue(boolean first, int index, double value) throws IOException {
		if (first) {
			fstream.write(index + " " + value);
		}
		else {
			fstream.write(", " + index + " " + value);
		}
	}
	
	/**
	 * Adds a new string value
	 * 
	 * @param first
	 * @param index
	 * @param value
	 * @throws IOException
	 */
	public void addStringValue(boolean first, int index, String value) throws IOException {
		if (first) {
			fstream.write(index + " " + value);
		}
		else {
			fstream.write(", " + index + " " + value);
		}
	}
	
	/**
	 * Adds a new constant value
	 * 
	 * @param first
	 * @param index
	 * @param value
	 * @throws IOException
	 */
	public void addConstantValue(boolean first, int index, String value) throws IOException {
		if (first) {
			fstream.write(index + " \"" + value + "\"");
		}
		else {
			fstream.write(", " + index + " " + value);
		}
	}
	
	/**
	 * Ends the row
	 * 
	 * @throws IOException
	 */
	public void endRow() throws IOException {
		fstream.write("}\n");	
	}
	
	/**
	 * Closes the file stream
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		fstream.close();
	}
	
}
