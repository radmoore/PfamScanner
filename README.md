HmmerRunner
===========
Bioinformatics: Wrapper around HMMSCAN for scanning against Pfam-defined domain models.

Usage: HmmerRunner [OPTIONS] -in <infile> -o <outfile> -d <workingdir>
Run HMMSCAN against Pfam defined domains

 -c,--collapse          Collapse domains of type repeat
 -dir <directory>       Working directory
 -E,--Evalue <arg>      Evalue threshold [Default: model defined]
 -h,--help              Print this help message
 -in <file>             Fasta input file
 -m,--merge             Merge split hits
 -out <file>            Output file
 -r,--remove-overlaps   Resolve overlaps