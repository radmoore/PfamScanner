package hmmerRunner;

import java.io.File;
import java.util.Random;

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

import parser.HmmerParser;

/**
 * Class HmmerRunner
 * Main class, sets up arguments and parameters (after check),
 * runs hmmscan and initiates parsing to xdom.
 * 
 * @author Andrew D. Moore <radmoore@uni-muenster.de>
 *
 */
public class HmmerRunner {
	
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
            		" the hmmscan binary (version >= 3.0). The working directory is, by default, the current directory.")
            .create("dir");
	
	@SuppressWarnings("static-access")
	static Option evalue = OptionBuilder.withArgName( "float" )
			.hasArg()
            .withDescription("Evalue threshold [Default: model defined gathering threshold]")
            .withLongOpt("evalue")
            .create("e");
	
	@SuppressWarnings("static-access")
	static Option verbose = OptionBuilder.withArgName( "float" )
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
            		String domtblout = cl.getOptionValue("in");
            		HmmerParser hmmoutParser = new HmmerParser(domtblout, cl.getOptionValue("out"));
            	
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
            		
            		hmmoutParser.writeXdom();
            		System.exit(0);	
            	}
            	
            	Hmmer hmmer = new Hmmer(cl.getOptionValue("in"), cl.getOptionValue("out"), cl.getOptionValue("dir"));
            	
            	if ( cl.hasOption("c") )
            		hmmer.setCPUs(cl.getOptionValue("c"));
            	if ( cl.hasOption("v") )
            		hmmer.setVerbose(true);
            	if ( cl.hasOption("s") )
            		hmmer.setOutputFile(cl.getOptionValue("s"));
            	
            	
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
	            		if (cl.hasOption("r"))
	            			hmmoutParser.setResolveOverlapsMode();
	            		if (cl.hasOption("e"))
	            			hmmoutParser.setEvalueThreshold(evalue);
	            		
	            		hmmoutParser.writeXdom();
	            		
	            		if ( hmmer.saveOutfile() )
	            			System.out.println("INFO: hmmout saved to "+hmmer.getHmmoutPath());
	            		
	            		else {
	            			hmmoutParser.destroyHmmoutFile();
	            		}
            		}
            		else {
            			System.err.println("ERROR: there was some problem running hmmscan (see error message above).");
            			System.exit(-1);
            		}
            	}
            	else {
            		System.exit(-1);
            	}
        	}
        }
		catch (MissingOptionException e) {
			
			f.printHelp("PfamScanner [OPTIONS] -in <infile> -o <outfile> -d <workingdir>", 
        		"Run hmmscan against Pfam defined domains\n", opt, "");
			System.exit(-1);
		}
		catch (MissingArgumentException e) {
			System.err.println(e.getMessage());
			f.printHelp("PfamScanner [OPTIONS] -in <infile> -o <outfile> -d <workingdir>", 
        		"Run hmmscan against Pfam defined domains\n", opt, "");
			System.exit(-1);
		}
		catch (UnrecognizedOptionException e) {
			System.err.println(e.getMessage());
			f.printHelp("PfamScanner [OPTIONS] -in <infile> -o <outfile> -d <workingdir>", 
	        		"Run hmmscan against Pfam defined domains\n", opt, "");
			System.exit(-1);
		}
		catch (ParseException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		System.exit(0);
	}
	
	
	private static void parseOnlyMode(CommandLine cl, Double evalue) {
	
	}
	
}