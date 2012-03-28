package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamGobbler extends Thread {

	InputStream is;
	Boolean verbose = true;
	    
	public StreamGobbler(InputStream is, Boolean verbose) {
		this.is = is;
		this.verbose = verbose;
	}
	    
	public void run() {
		try {
			InputStreamReader isr = new InputStreamReader(is);
	        BufferedReader br = new BufferedReader(isr);
	        String line = null;
	        while ( (line = br.readLine()) != null )
	        	if ( verbose )
	        		System.out.println(line);    
	    } 
		catch (IOException ioe) {
			ioe.printStackTrace();  
		}
	}
	

}
