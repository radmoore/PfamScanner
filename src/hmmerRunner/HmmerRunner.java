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
			opt.addOption("m", false, "Merge split hits");
			opt.addOption("e", false, "Evalue threshold [Default: model defined]");
			opt.addOption("r", false, "Resolve overlaps [Default: true, best match cascade]");
			opt.addOption("c", false, "Collapse domains of type repeat [Default: false]");
            opt.addOption("h", false, "Print this help message");
            
            BasicParser parser = new BasicParser();
            CommandLine cl = parser.parse(opt, args, false);

            if ( cl.hasOption('h') ) {    
                f.printHelp("HmmerRunner", opt);
            }
            else {
            	Hmmer hmmer = new Hmmer(
            			cl.getOptionValue("in"), 
            			cl.getOptionValue("out"), 
            			cl.getOptionValue("dir"));
            	if ( hmmer.checkParams() )
            		hmmer.execute();
            }
            
        }
		catch (MissingOptionException e) {
            f.printHelp("HmmerRunner", opt);
		}
		catch (MissingArgumentException e) {
			System.err.println(e.getMessage());
			f.printHelp("HmmerRunner", opt);
		}
        catch (ParseException e) {
            e.printStackTrace();
        }
	}

	
}

