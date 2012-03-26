package hmmerRunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import parser.HmmerParser;

public class Hmmer {
	
	private static String PFAMANAME = "Pfam-A_snip.hmm";
	private static String PFAMBNAME = "Pfam-B.hmm";
	private static String HMMEREXEC = "hmmscan" ;
	private File inputFile, outputFile, workingDir;
	private static String testFile;
	private static File tempHmmerout = null;
	
	
	public Hmmer(String inputFilePath, String outputFilePath, String workingDirPath) {
		try {
			this.inputFile = new File(inputFilePath);
			this.outputFile = new File(outputFilePath);
			this.workingDir = new File(workingDirPath);
		}
		catch(Exception e) {
			System.err.println("ERROR: could not create files. Exiting.");
			e.printStackTrace();
		}
	}
	
	public boolean checkParams() {
		if ( ( !inputFile.isFile() ) || ( !inputFile.canRead() ) ) {
			System.err.println("ERROR: could not read from "+inputFile.getName()+" or not a file. Exiting.");
			return false;
		}
//		if ( ( !outputFile.isFile() ) || ( !outputFile.canWrite() ) ) {
//			System.err.println("ERROR: could not write to "+outputFile.getName()+" or not a file. Exiting.");
//			return false;
//		}
		if (! workingDir.isDirectory() ) {
			System.err.println("ERROR: "+workingDir.getName()+" is not a directory. Exiting.");
			return false;
		}	
		return true;
	}
	
	
	public void execute() {
		List<String> command = prepareArgs();
		try {
      		ProcessBuilder pb = new ProcessBuilder(command);
      		Process process = pb.start();
      		InputStream stdout = process.getInputStream();
      		InputStream stderr = process.getErrorStream();
      		
      		BufferedReader stdoutReader = new BufferedReader (new InputStreamReader(stdout));
      		BufferedReader stderrReader = new BufferedReader (new InputStreamReader(stderr));
      		String line;
      		
      		while ( (line = stdoutReader.readLine ()) != null ) {
				//System.out.println (line);
      			//System.out.println("HELLO!");
			}
 
      		while ( (line = stderrReader.readLine ()) != null ) {
				System.err.println (line);
			}
      		
      		int rc = process.waitFor();
      		if ( rc == 0 ) {
      			HmmerParser parser = new HmmerParser(tempHmmerout, outputFile);
      			parser.writeXdom();
      			parser.destoryFile();
      		}
      		else {
      			System.out.println("ERROR: "+rc);
      			// some HMMERSCAN error
      		}
      		
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private List<String> prepareArgs() {
		try {
			tempHmmerout = File.createTempFile("hmmscan_run", ".domtblout", workingDir);
		}
		catch (Exception e) {
			System.err.println("ERROR: could not create TEMP file. Exiting.");
			System.exit(-1);
		}
		List<String> command = new ArrayList<String>();
   		command.add(HMMEREXEC);
   		command.add("--domtblout");
   		command.add(tempHmmerout.getAbsolutePath());
   		command.add("--cut_ga");
   		command.add(workingDir.getAbsolutePath()+"/"+PFAMANAME);
   		command.add(inputFile.getAbsolutePath());
   		//System.out.println(command);
		return command;
	}
	

}
