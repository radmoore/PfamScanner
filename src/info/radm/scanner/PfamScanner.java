package info.radm.scanner;


import info.radm.scanner.hmmer.Hmmer;
import info.radm.scanner.hmmer.HmmerParser;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.UnrecognizedOptionException;


/**
 * Class HmmerRunner
 * Main class, sets up arguments and parameters (after check),
 * runs hmmscan and initiates parsing to xdom.
 * 
 * @author Andrew D. Moore <radmoore@uni-muenster.de>
 *
 */
public class PfamScanner {
	
	@SuppressWarnings("static-access")
	static Option inputFile = OptionBuilder.withArgName( "file" )
			.hasArg()
            .withDescription("Fasta || hmmout input file (see option parse-only)")
            .isRequired()
            .create("in");
	
	@SuppressWarnings("static-access")
	static Option outputFile = OptionBuilder.withArgName( "file" )
			.hasArg()
            .withDescription("XDOM output file")
            .isRequired()
            .create("out");
	
	@SuppressWarnings("static-access")
	static Option workingDir = OptionBuilder.withArgName( "directory" )
			.hasArg()
            .withDescription("The working directory. This directory must contain the Pfam-A domain models," +
            		" pressed using hmmpress and named Pfam-A.hmm. This directory must also contain" +
            		" the hmmscan binary (version >= 3.0). By default, the working directory is set to the" +
            		" current directory.")
            .create("dir");
	
	@SuppressWarnings("static-access")
	static Option evalue = OptionBuilder.withArgName( "float" )
			.hasArg()
            .withDescription("Evalue threshold [Default: model defined gathering threshold]")
            .withLongOpt("evalue")
            .create("e");
	
	@SuppressWarnings("static-access")
	static Option verbose = OptionBuilder
            .withDescription("Verbose scan")
            .withLongOpt("verbose")
            .create("v");
	
	@SuppressWarnings("static-access")
	static Option keepAnn = OptionBuilder.withArgName( "file" )
			.hasArg()
            .withDescription("Save hmmscan output (full path required)")
            .withLongOpt("save")
            .create("s");
	
	@SuppressWarnings("static-access")
	static Option parseOnly = OptionBuilder.withArgName( "file" )
            .withDescription("Parse previous run of hmmscan (save to file). If set, <in> file must be hmmscan (version 3)" +
            		" domtblout format, and <out> the name of the xdom that should be written to.")
            .withLongOpt("parse-only")
            .create("p");
	
	@SuppressWarnings("static-access")
	static Option tempDir = OptionBuilder.withArgName( "dir" )
            .withDescription("Directory in which temporary files are to be written.")
            .hasArg()
            .withLongOpt("tempdir")
            .create("t");
	
	@SuppressWarnings("static-access")
	static Option modelFile = OptionBuilder.withArgName( "models" )
            .withDescription("File containing HMMs for scan (must be indexed)")
            .hasArg()
            .withLongOpt("model")
            .create("M");
	
	public static void main(String[] args) {
		
		Options opt = new Options();
		HelpFormatter f = new HelpFormatter();
		f.setSyntaxPrefix("Usage: ");
		
		try {

			opt.addOption(inputFile);
			opt.addOption(outputFile);
			opt.addOption(workingDir);
			opt.addOption(evalue);
			opt.addOption(verbose);
			opt.addOption(keepAnn);
			opt.addOption(parseOnly);
			opt.addOption(tempDir);
			opt.addOption(modelFile);
			opt.addOption("acc", "accession", false, "Use Pfam (PF00002) accessions instead of IDs (7tm_2)");
			opt.addOption("m", "merge", false, "Merge split hits");
			opt.addOption("c", "cpu", true, "Number of parallel CPU workers to use for multithreads (hmmscan)");
			opt.addOption("r", "remove-overlaps", false, "Resolve overlaps (Best match cascade)");
			opt.addOption("C", "collapse", false, "Collapse domains of type repeat");
            opt.addOption("h", "help", false, "Print this help message");
            
            PosixParser parser = new PosixParser();
            CommandLine cl = parser.parse(opt, args, false);

            if ( cl.hasOption('h') ) {  
                f.printHelp("PfamScanner [OPTIONS] -in <infile> -out <outfile> -dir <workingdir>", 
                		"Run hmmscan against Pfam defined domains.\n", opt, "");
                System.exit(0);
            }
            
            else {
            	
            	Double evalue = null;
            	// Check evalue format, if specified
            	if (cl.hasOption("e")) {
            		try {
            			evalue = Double.valueOf(cl.getOptionValue("e"));
            		} 
            		catch (NumberFormatException nfe) {
            			System.err.println("ERROR: Specified evalue not a valid number. Exiting.");
            			System.exit(-1);
            		}
            	}
            	// go to parse only mode
            	if (cl.hasOption("p")) {
            		String domtbloutPath = cl.getOptionValue("in");
            		HmmerParser hmmoutParser = new HmmerParser(domtbloutPath, cl.getOptionValue("out"));
            	
            		// consider parsing options
            		if (cl.hasOption("m"))
            			hmmoutParser.setMergeMode();
            		if (cl.hasOption("C")) {
            			System.err.println("INFO: Collapse mode not yet supported - ignoring.");
            			//hmmoutParser.setCollapseMode();
            		}
            		if (cl.hasOption("r"))
            			hmmoutParser.setResolveOverlapsMode();
            		if (cl.hasOption("e"))
            			hmmoutParser.setEvalueThreshold(evalue);
            		
            		if (HmmerParser.determineFileFormat(domtbloutPath) == HmmerParser.HMMSCAN) {
            			System.out.println("File format: hmmscan");
            			hmmoutParser.hmmscan2xdom();
            		}
            		else if (HmmerParser.determineFileFormat(domtbloutPath) == HmmerParser.PFAMSCAN) {
            			System.out.println("File format: pfamscan");
            			hmmoutParser.pfamscan2xdom();
            		}
            		else {
            			System.err.println("ERROR: Cannot determine file type of "+domtbloutPath);
            			System.exit(-1);
            		}
            		System.exit(0);	
            	}
            	
            	// get working dir, or set to CRW if non provided
            	String wd = System.getProperty("user.dir");
            	if (cl.hasOption("dir"))
            		wd = cl.getOptionValue("dir");
            	
            	Hmmer hmmer = new Hmmer(cl.getOptionValue("in"), cl.getOptionValue("out"), wd);
            	
            	if (cl.hasOption("M"))
            		hmmer.setModelFile(cl.getOptionValue("M"));
            	if ( cl.hasOption("c") )
            		hmmer.setCPUs(cl.getOptionValue("c"));
            	if ( cl.hasOption("v") )
            		hmmer.setVerbose(true);
            	if ( cl.hasOption("s") )
            		hmmer.setOutputFile(cl.getOptionValue("s"));
            	if ( cl.hasOption("t") )
            		hmmer.setTempDir(cl.getOptionValue("t"));
        		if (cl.hasOption("e"))
        			hmmer.setEvalueThreshold(evalue);
            	
            	
            	if ( hmmer.checkParams() ) {
            		
            		int retValue = hmmer.doInBackground();
            		
            		if (retValue == 0) {
	            		String domtblout = hmmer.getHmmoutPath();
	            		HmmerParser hmmoutParser = new HmmerParser(domtblout, cl.getOptionValue("out"));
	            	
	            		// consider parsing options
	            		if (cl.hasOption("m"))
	            			hmmoutParser.setMergeMode();
	            		if (cl.hasOption("C")) {
	            			System.err.println("INFO: Collapse mode not yet supported - ignoring.");
	            			//hmmoutParser.setCollapseMode();
	            		}
	            		if (cl.hasOption("acc"))
	            			hmmoutParser.setAccMode();
	            		
	            		if (cl.hasOption("r"))
	            			hmmoutParser.setResolveOverlapsMode();

	            		hmmoutParser.hmmscan2xdom();
	            		
	            		if ( hmmer.saveOutfile() )
	            			System.out.println("INFO: hmmout saved to "+hmmer.getHmmoutPath());
	            		
	            		else {
	            			hmmoutParser.destroyHmmoutFile();
	            		}
            		}
            		// hmmscan returned != 0
            		else {
            			System.err.println("ERROR: there was some problem running hmmscan (see error message above).");
            			System.exit(-1);
            		}
            	}
            	// check params failed
            	else {
            		System.exit(-1);
            	}
        	}
        }
		catch (MissingOptionException e) {
			f.printHelp("PfamScanner [OPTIONS] -in <infile> -out <outfile> -dir <workingdir>", 
        		"Run hmmscan against Pfam defined domains\n", opt, "");
			System.exit(-1);
		}
		catch (MissingArgumentException e) {
			System.err.println(e.getMessage());
			f.printHelp("PfamScanner [OPTIONS] -in <infile> -out <outfile> -dir <workingdir>", 
        		"Run hmmscan against Pfam defined domains\n", opt, "");
			System.exit(-1);
		}
		catch (UnrecognizedOptionException e) {
			System.err.println(e.getMessage());
			f.printHelp("PfamScanner [OPTIONS] -in <infile> -out <outfile> -dir <workingdir>",
	        		"Run hmmscan against Pfam defined domains\n", opt, "");
			System.exit(-1);
		}
		catch (ParseException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		System.exit(0);
	}
	
}