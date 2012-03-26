package hmmerRunner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import parser.HmmerParser;

public class HmmerRunner {
	
	@SuppressWarnings("static-access")
	static Option inputFile = OptionBuilder.withArgName( "file" )
			.hasArg()
            .withDescription("Fasta input file")
            .isRequired()
            .create("in");
	
	@SuppressWarnings("static-access")
	static Option outputFile = OptionBuilder.withArgName( "file" )
			.hasArg()
            .withDescription("Output file")
            .isRequired()
            .create("out");
	
	@SuppressWarnings("static-access")
	static Option workingDir = OptionBuilder.withArgName( "directory" )
			.hasArg()
            .withDescription("Working directory")
            .isRequired()
            .create("dir");
	
	
	public static void main(String[] args) {
		
		Options opt = new Options();
		HelpFormatter f = new HelpFormatter();
		
		try {

			opt.addOption(inputFile);
			opt.addOption(outputFile);
			opt.addOption(workingDir);
			opt.addOption("M", "merge", false, "Merge split hits");
			opt.addOption("e", "Evalue", true, "Evalue threshold [Default: model defined]");
			opt.addOption("c", "cpu", true, "Number of parallel CPU workers to use for multithreads");
			opt.addOption("R", "remove-overlaps", false, "Resolve overlaps");
			opt.addOption("C", "collapse", false, "Collapse domains of type repeat");
            opt.addOption("h", "help", false, "Print this help message");
            //opt.addOption("p", "--parse-only", true, "Only parse output");
            
            PosixParser parser = new PosixParser();
            CommandLine cl = parser.parse(opt, args, false);

            if ( cl.hasOption('h') ) {    
                f.printHelp("HmmerRunner [OPTIONS] -in <infile> -o <outfile> -d <workingdir>", 
                		"Run HMMSCAN against Pfam defined domains\n", opt, "");
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
            	Hmmer hmmer = new Hmmer(cl.getOptionValue("in"), cl.getOptionValue("out"), cl.getOptionValue("dir"));
            	// set CPUs, if provided
            	if ( cl.hasOption("c") )
            		hmmer.setCPUs(cl.getOptionValue("c"));
            	
            	if ( hmmer.checkParams() ) {
            		
            		int retValue = hmmer.doInBackground();
            		
            		if (retValue == 0) {
	            		String domtblout = hmmer.getTempOutput();
	            		HmmerParser hmmoutParser = new HmmerParser(domtblout, cl.getOptionValue("out"));
	            	
	            		// consider parsing options
	            		if (cl.hasOption("M"))
	            			hmmoutParser.setMergeMode();
	            		if (cl.hasOption("C"))
	            			hmmoutParser.setCollapseMode();            		
	            		if (cl.hasOption("R"))
	            			hmmoutParser.setResolveOverlapsMode();
	            		if (cl.hasOption("e"))
	            			hmmoutParser.setEvalueThreshold(evalue);
	            		
	            		hmmoutParser.writeXdom();
	            		//hmmoutParser.destoryTempFile();
            		}
            	}
            	else {
            		System.exit(-1);
            	}
        	}
        }
		catch (MissingOptionException e) {
			f.printHelp("HmmerRunner [OPTIONS] -in <infile> -o <outfile> -d <workingdir>", 
        		"Run HMMSCAN against Pfam defined domains\n", opt, "");
			System.exit(0);
		}
		catch (MissingArgumentException e) {
			System.err.println(e.getMessage());
			f.printHelp("HmmerRunner [OPTIONS] -in <infile> -o <outfile> -d <workingdir>", 
        		"Run HMMSCAN against Pfam defined domains\n", opt, "");
		}
		catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
}