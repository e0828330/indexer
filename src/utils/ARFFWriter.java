package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.zip.GZIPOutputStream;

public class ARFFWriter {
	
	private BufferedWriter writer;
	
	/**
	 * Writes a new (sparse) ARFF file
	 * 
	 * @param filename
	 * @param name
	 * @throws IOException
	 */
	public ARFFWriter(String filename, String name) throws IOException {
		GZIPOutputStream gzout = new GZIPOutputStream(new FileOutputStream(new File(filename)));
		writer = new BufferedWriter(new OutputStreamWriter(gzout));
		writer.append("@RELATION " + name + "\n\n");
	}
	
	/**
	 * Adds a new attribute
	 * 
	 * @param name
	 * @param type
	 * @throws IOException
	 */
	public void addAttribute(String name, String type) throws IOException {
		writer.append("@ATTRIBUTE \"" + name + "\" " + type + "\n");
	}
	
	/**
	 * Start data section
	 * 
	 * @throws IOException
	 */
	public void beginData() throws IOException {
		writer.append("@DATA\n");
	}
	
	/**
	 * Start new row
	 * @throws IOException
	 */
	public void startRow() throws IOException {
		writer.append("{");
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
			writer.append(index + " " + value);
		}
		else {
			writer.append(", " + index + " " + value);
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
			writer.append(index + " \"" + value + "\"");
		}
		else {
			writer.append(", " + index + " \"" + value + "\"");
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
			writer.append(index + " " + value);
		}
		else {
			writer.append(", " + index + " " + value);
		}
	}
	
	/**
	 * Adds a new numeric value
	 * 
	 * @param first
	 * @param index
	 * @param value
	 * @throws IOException
	 */
	public void addNumericValue(boolean first, int index, int value) throws IOException {
		if (first) {
			writer.append(index + " " + value);
		}
		else {
			writer.append(", " + index + " " + value);
		}
	}	
	
	/**
	 * Ends the row
	 * 
	 * @throws IOException
	 */
	public void endRow() throws IOException {
		writer.append("}\n");	
	}
	
	/**
	 * Closes the file stream
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		writer.close();
	}
	
}
