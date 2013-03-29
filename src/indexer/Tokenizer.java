package indexer;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class Tokenizer {
	
	private String buffer;
	
	public Tokenizer(String filename) {
		try {
			FileInputStream fstream = new FileInputStream(filename);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line = null;
			boolean headerDone = false;
			buffer = "";
			while((line = br.readLine()) != null) {
				if (!headerDone) {
					if (line.startsWith("Subject:")) {
						String[] tmp = line.split("Subject:");
						if (tmp[1].trim().startsWith("Re:")) {
							buffer += tmp[1].trim().substring(4).trim() + " ";
						}
						else {
							buffer += tmp[1].trim() + " ";
						}
					}
					if (line.trim().equals("")) {
						headerDone = true;
					}
				}
				else {
					buffer += line;
				}
			}
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String[] getTokens() {
		//TODO: be smarter?
		return buffer.split("\\W+");
	}
}
