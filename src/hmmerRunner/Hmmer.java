package hmmerRunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingWorker;

import utils.StreamGobbler;

public class Hmmer extends SwingWorker<Integer, Void> {
	
	private static String PFAMANAME = "Pfam-A.hmm";
	//private static String PFAMBNAME = "Pfam-B.hmm";
	private static String HMMEREXEC = "hmmscan" ;
	private String CPUs = null;
	private File inputFile, outputFile, workingDir, tmpHmmerOut;
	
	
	public Hmmer(String inputFilePath, String outputFilePath, String workingDirPath) {
		try {
			this.inputFile = new File(inputFilePath);
			this.outputFile = new File(outputFilePath);
			this.workingDir = new File(workingDirPath);
		}
		catch(Exception e) {
			System.err.println("ERROR: could not create files. Exiting.");
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public String getTempOutput() {
		return this.tmpHmmerOut.getAbsolutePath();
	}
	
	public void setCPUs(String CPUs) {
		this.CPUs = CPUs;
	}
	
	public boolean checkParams() {
		if ( ( !inputFile.isFile() ) || ( !inputFile.canRead() ) ) {
			System.err.println("ERROR: could not read from "+inputFile.getName()+" or not a file. Exiting.");
			return false;
		}
		if ( (!outputFile.isFile()) ) { 
			try {
				// create if non-existent
				if (!outputFile.createNewFile()) {		
					System.err.println("ERROR: could not create "+outputFile.getAbsoluteFile()+". Exiting.");
					return false;
				}
			} 
			catch (IOException ioe) {
				System.err.println("ERROR: there was a problem creating "+outputFile);
				System.exit(-1);
			}
		}
		
		if (! workingDir.isDirectory() ) {
			System.err.println("ERROR: "+workingDir.getName()+" is not a directory. Exiting.");
			return false;
		}
		
		if (this.CPUs != null) {
			try {
				Integer.getInteger(CPUs);
			}
			catch (NumberFormatException nfe) {
				System.err.println("ERROR: invalid number of CPUs: "+ CPUs +". Exiting.");
				return false;
			}
		}
		
		// all is well, create tmpfile
		try {
			tmpHmmerOut = File.createTempFile("hmmer", ".domtblout", workingDir);
		}
		catch (IOException ioe){
			System.err.println("ERROR: could not create temporary file. Exiting.");
			System.exit(-1);
		}
		return true;
	}
	
	
	protected Integer doInBackground() throws Exception {

		int exitValue = -1;
		List<String> command = prepareArgs();
		System.out.println("This is the command list: "+command);
		try {
      		ProcessBuilder pb = new ProcessBuilder(command);
      		pb.redirectErrorStream(true);
      		Process process = pb.start();
      		// remove the streams quickly in sepearte threads to avoid
      		// loosing the process
      		StreamGobbler stg = new StreamGobbler(process.getInputStream(), false);
      		stg.start();
      		
      		exitValue = process.waitFor();
		}
  		catch (InterruptedException ire) {
  			ire.printStackTrace();
  		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return exitValue;
	}
	
	private List<String> prepareArgs() {
		List<String> command = new ArrayList<String>();
   		command.add(HMMEREXEC);
   		command.add("--domtblout");
   		command.add(tmpHmmerOut.getAbsolutePath());
   		command.add("--cut_ga");
   		command.add("--cpu");
   		command.add("2");
   		command.add(workingDir.getAbsolutePath()+"/"+PFAMANAME);
   		command.add(inputFile.getAbsolutePath());
		return command;
	}


	

}
