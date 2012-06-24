PfamScanner
===========
PfamScanner is a wrapper around [hmmscan](http://hmmer.janelia.org/ "HMMER") 
for scanning against protein sequences against [Pfam]("http://pfam.sanger.ac.uk/" "Pfam")-defined 
domain models. Scan output is provided in a fasta-like format (xdom).

###### The .xdom format
For example, the Human ATP-binding cassette sub-family F member 2 
([Q9UG63]("http://www.uniprot.org/uniprot/Q9UG63" "See Q9UG63 at UniProt")) contains
three Pfam-A domains:
<pre>
>Q9UG63  623
44  257  ABC_tran
296 383  ABC_tran_2
437 545  ABC_tran	
</pre>
The xdom specifies 

<code>
Usage: HmmerRunner [OPTIONS] -in <infile> -o <outfile> -d <workingdir>
Run HMMSCAN against Pfam defined domains
</code>
<pre>
 -c,--collapse				Collapse domains of type repeat
 -dir <directory>  		Working directory
 -E,--Evalue <arg>    Evalue threshold [Default: model defined]
 -h,--help            Print this help message
 -in <file>           Fasta input file
 -m,--merge           Merge split hits
 -out <file>          Output file
 -r,--remove-overlaps Resolve overlaps
 </pre>