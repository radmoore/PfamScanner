package hmmerRunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingWorker;

import utils.StreamGobbler;


/**
 * Class Hmmer
 * Runs hmmerscan from HMMER3 against pressed Pfam-A models.
 * The Pfam-A models muss be pressed using hmmpress, and must reside
 * as a file Pfam-A.hmm in workingdir.
 * 
 * @author Andrew D. Moore <radmoore@uni-muenster.de>
 *
 */
public class Hmmer extends SwingWorker<Integer, Void> {
	
	private static String PFAMANAME = "Pfam-A.hmm";
	//private static String PFAMBNAME = "Pfam-B.hmm";
	private static String HMMEREXEC = "hmmscan" ;
	private String CPUs = "1";
	private File inputFile, outputFile, workingDir, hmmoutFile;
	private boolean verbose, saveOutFile = false;
	
	
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
	
	/**
	 * 
	 * @return
	 */
	public String getHmmoutPath() {
		return this.hmmoutFile.getAbsolutePath();
	}
	
	/**
	 * 
	 * @param hmmoutFile
	 */
	public void setOutputFile(String hmmoutFile) {
		try {
			this.hmmoutFile = new File(hmmoutFile);
			saveOutFile = true;
		}
		catch (Exception e) {
			System.err.println("ERROR: could not hmmoutfile "+hmmoutFile+". Exiting.");
			System.exit(-1);
		}
		
	}
	 
	/**
	 * 
	 * @return
	 */
	public boolean saveOutfile() {
		return saveOutFile;
	}
	
	/**
	 * 
	 * @param CPUs
	 */
	public void setCPUs(String CPUs) {
		this.CPUs = CPUs;
	}
	
	/**
	 * 
	 * @param verbose
	 */
	public void setVerbose(boolean verbose) {
		System.out.println("Setting verbose mode.");
		this.verbose = verbose;
	}
	
	/**
	 * 
	 * @return
	 */
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
		// create temp out file, if save not requested
		if (this.hmmoutFile == null) {
			try {
				hmmoutFile = File.createTempFile("hmmer", ".domtblout", workingDir);
			}
			catch (IOException ioe){
				System.err.println("ERROR: could not create temporary file. Exiting.");
				System.exit(-1);
			}
		}
		return true;
	}
	
	/**
	 * 
	 */
	protected Integer doInBackground() {

		int exitValue = -1;
		List<String> command = prepareArgs();
		try {
      		ProcessBuilder pb = new ProcessBuilder(command);
      		//pb.redirectErrorStream(true);
      		Process process = pb.start();
      		StreamGobbler stdoutStg = new StreamGobbler(process.getInputStream(), verbose);
      		StreamGobbler stderrStg = new StreamGobbler(process.getErrorStream(), true);
      		stdoutStg.start();
      		stderrStg.start();
      		
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
	
	/**
	 * 
	 * @return
	 */
	private List<String> prepareArgs() {
		List<String> command = new ArrayList<String>();
   		command.add(HMMEREXEC);
   		command.add("--domtblout");
   		command.add(hmmoutFile.getAbsolutePath());
   		command.add("--cut_ga");
   		command.add("--cpu");	
   		command.add(CPUs);
   		command.add(workingDir.getAbsolutePath()+"/"+PFAMANAME);
   		command.add(inputFile.getAbsolutePath());
		return command;
	}

}
