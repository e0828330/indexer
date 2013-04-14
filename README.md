Text Indexer & Similarity Retrieval

1. Usage

A runnable jar can be created by running 'ant' in the root directory.

The program has the following options:

[-indexer] : Indexes the collection.
[-min MIN] : Sets the minimum threshold (default 0).
[-max MAX] : Sets the maximum threshold (default -1 = unlimited)
[-stemming] : Enables stemming.
[-q (<path>|query)] : The path to the query file or the query itself.
[-lsize (none|small|medium|large)] :  Sets the list size. (default none)
[-t X] : Sets the topic number. (default 0) 
[-searchout <path>] : The search output (TREC) file.
-i <path> : Path to the collection or arff.gz containing the index.
[-idxout <path>] : The indexer output file (arff.gz).

If the program is started without -indexer option, a query is required for the
search. The query can be a file or a space divided sequence of words like 
"word1 word2 word3". In search mode, the -searchout option is optional. If
-searchout is set, the output for the search is written into the given file,
otherwise it is printed to the console.

If the -indexer option is set, the program creates the index and therefore needs
the -idxout option where the generated .arff.gz file is stored.

The -i option points to either the document collection (when in indexing mode)
otherwise to the arff.gz file that contains a previously created index.

If additionally to the -indexer option a query is given, the search is started.

The -lsize and -t options are only used to change the text fiels in the TREC
output file. The lsize option gives the name of the positing list while the -t
option gives the topic number. Both have no effect on the algorithms, they are
just required for the ouput.


-min and -max set the threshold for lower and upperbound term frequency and are
used by the indexer to implement the frequency thresholding.

-stemming enables stemming.

Sample options for call:
-i "/home/.../20_newsgroups_subset"  
-indexer -idxout "/home/.../indexer.arff.gz" 
-q "windows" 
-min 5 
-max 30 
-stemming

Sample1:
java -jar indexer.jar -indexer -i ../information_retrieval/20_newsgroups_subset -idxout index_large.arff.gz -q "microsoft" -stemming

Sample 2:
java -jar indexer.jar -indexer -i ../information_retrieval/20_newsgroups_subset -idxout index_large.arff.gz -q "microsoft" -stemming -lsize small -t 1

Sample 3:

java -jar indexer.jar -i index_large.arff.gz -q "microsoft" -searchout out.txt




The above command will index the document collection "/home/.../20_newsgroups_subset"
write the index to "/home/.../indexer.arff.gz". The indexer will use stemming
and a fequency threshold between 5 and 30. The -q will trigger a search once the
indexing is done.

2. Implementation details

The indexing process gets as input a directory that contains the document
collection, a Boolean that indicates whether stemming should be used or not 
and the frequency threshold.

The indexer itself works similar to the MapReduce approach described in the book
and the slides. Instead of assigning tasks to nodes it uses threads on a single 
machine. The idea behind that was to use the multiple CPU cores found on todays
machines to speed up the indexing process.

A thread pool is used to lower the overhead of spawning new threads.

The parser threads parse one document each. The number of running threads is lim
ited to the number of available Processors. The directory traversal, which is done
by the main thread generally is faster then the the parsers but this isn't a 
problem be cause the threads will just get queued and run once one of the currently
 running parsers is finished.

Once the parsers are done parsing the document the indexer will run the inverter
threads using the same techniques as the parser. Each inverter gets a term and a 
list of documents that contain said term. Those lists have been generated by the
parsers in the previous step.
The inverters create the posting lists, calculate the term frequencies (and 
discard documents whose term frequencies are outside of the thresholds).
Afterwards the tf.idf weights are calculatedfor each posting.

Once the inverters are done we have an inverted index (that is currently
completely in memory due to its small size). 

The index is then used to create a sparse gzip compressed ARFF file. The ARFF 
file writing has been implemented by hand because WEKA turned out to be 
significantly slower at doing this task. In addition to the terms the ARFF
contains the document class, document id and a flag indicating whether stemming
has been used or not to create the ARFF file. The later ensures that stemming
will be used for the search when stemming has been used to create the index.

Stemming has been implemented using the Porter stemmer library: 
http://www.tartarus.org/~martin/PorterStemmer

For the search step the document vectors and the index are rebuild from the ARFF
file. Sorting of the posting lists is done using multiple threads to speed things
up. 

The search itself uses the FASTCOSINESOURCE algorithm from the book (using the 
td.idf weight) to find the most relevant documents.

We have just used the whole text of the topic files as input for the search, but
we do provide an interface for searching using any text (see usage section for
 details).

3. Small - Medium - Large ARFF files

The 3 ARFF files were generated with the following parameters:

Set -indexer
Set -min to 0.
Set -max to -1.
Set -stemming to false
Set -i to ../information_retrieval/20_newsgroups_subset.
Set -idxout to ../information_retrieval/index_large.arff.gz.
Done indexing 8000 documents in 6669ms 
Number of terms: 130450
index_large.arff.gz

Cmdline: java -jar indexer.jar -indexer -i ../information_retrieval/20_newsgroups_subset -idxout index_large.arff.gz


Set -indexer
Set -min to 2.
Set -max to 150.
Set -stemming to false.
Set -i to ../information_retrieval/20_newsgroups_subset.
Set -idxout to ../information_retrieval/index_medium.arff.gz.
Done indexing 8000 documents in 4989ms 
Number of terms: 27483
index_medium.arff.gz

Cmdline: java -jar indexer.jar -indexer -i ../information_retrieval/20_newsgroups_subset -min 2 -max 150 -idxout index_medium.arff.gz


Set -indexer
Set -min to 5.
Set -max to 10.
Set -stemming to false.
Set -i to ../information_retrieval/20_newsgroups_subset.
Set -idxout to ../information_retrieval/index_small.arff.gz.
Done indexing 8000 documents in 4652ms 
Number of terms: 5883
index_small.arff.gz

Cmdline: java -jar indexer.jar -indexer -i ../information_retrieval/20_newsgroups_subset -min 5 -max 10 -idxout index_small.arff.gz

4. TREC files

The files are generated with the following command:

Cmdline: ./create_trec_files.sh ../information_retrieval/20_newsgroups_subset/ index_small.arff.gz small
Cmdline: ./create_trec_files.sh ../information_retrieval/20_newsgroups_subset/ index_medium.arff.gz medium
Cmdline: ./create_trec_files.sh ../information_retrieval/20_newsgroups_subset/ index_large.arff.gz large
