PfamScanner
===========
PfamScanner is a wrapper around [hmmscan](http://hmmer.janelia.org/ "HMMER") 
for scanning against protein sequences against [Pfam]("http://pfam.sanger.ac.uk/" "Pfam")-defined 
domain models. Scan output is provided in a fasta-like format (xdom).

###### The .xdom format
For example, the Human ATP-binding cassette sub-family F member 2 
([Q9UG63]("http://www.uniprot.org/uniprot/Q9UG63" "See Q9UG63 at UniProt")) 
contains three Pfam-A domains:
<pre>
>Q9UG63  623
44  257  ABC_tran
296 383  ABC_tran_2
437 545  ABC_tran	
</pre>
The first line specifies the protein ID and its length in amino acids. The remaining lines
specify each domain (in sequence) with its repective co-ordinates and ID or accession 
number. Optionally, the ID fields can be followed by an Evalue inidcating the significance of
the hit between the domain sequence and the defining model. 

###### Features
* Runs in scan / parse-only mode
* Provides overlap resolution (by best matching cascade)
* Can collapse successive domains of type repeat (repeat arrays)
* Can merge split hits (successive hits, in sequence, to the same model)


###### Usage
<pre>
$ java -jar build/PfamScanner.jar
</pre>
```java
Usage: PfamScanner [OPTIONS] -in <infile> -out <outfile> -dir <workingdir>
Run hmmscan against Pfam defined domains

 -acc,--accession       Use Pfam (PF00002) accessions instead of IDs
                        (7tm_2)
 -c,--cpu <arg>         Number of parallel CPU workers to use for
                        multithreads (hmmscan)
 -C,--collapse          Collapse domains of type repeat
 -dir <directory>       The working directory. This directory must contain
                        the Pfam-A domain models, pressed using hmmpress
                        and named Pfam-A.hmm. This directory must also
                        contain the hmmscan binary (version >= 3.0). By
                        default, the working directory is set to the
                        current directory.
 -e,--evalue <float>    Evalue threshold [Default: model defined gathering
                        threshold]
 -h,--help              Print this help message
 -in <file>             Fasta || hmmout input file (see option parse-only)
 -M,--model <models>    File containing HMMs for scan (must be indexed)
 -m,--merge             Merge split hits
 -out <file>            XDOM output file
 -p,--parse-only        Parse previous run of hmmscan (save to file). If
                        set, <in> file must be hmmscan (version 3)
                        domtblout format, and <out> the name of the xdom
                        that should be written to.
 -r,--remove-overlaps   Resolve overlaps (Best match cascade)
 -s,--save <file>       Save hmmscan output (full path required)
 -t,--tempdir <dir>     Directory in which temporary files are to be
                        written.
 -v,--verbose           Verbose scan
```
