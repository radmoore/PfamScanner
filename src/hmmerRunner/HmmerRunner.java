package hmmerRunner;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

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
			opt.addOption("m", "merge", false, "Merge split hits");
			opt.addOption("E", "Evalue", true, "Evalue threshold [Default: model defined]");
			opt.addOption("r", "remove-overlaps", false, "Resolve overlaps");
			opt.addOption("c", "collapse", false, "Collapse domains of type repeat");
            opt.addOption("h", "help", false, "Print this help message");
            
            PosixParser parser = new PosixParser();
            CommandLine cl = parser.parse(opt, args, false);

            if ( cl.hasOption('h') ) {    
                f.printHelp("HmmerRunner [OPTIONS] -in <infile> -o <outfile> -d <workingdir>", 
                		"Run HMMSCAN against Pfam defined domains\n", opt, "");
                System.exit(0);
            }
            else {
            	Hmmer hmmer = new Hmmer(
            			cl.getOptionValue("in"), 
            			cl.getOptionValue("out"), 
            			cl.getOptionValue("dir"));
            	if ( hmmer.checkParams() )
            		//TODO: set other params on hmmer instance
            		hmmer.execute();
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

