CausalRelExtractor
==================

This is an implementation of Mehwish Riaz's PhD thesis, "Mining Novel Sources Of Knowledge To Identify Causal
Information In Text". 

We have created a system which automatically creates a training corpus, trains a classifier on this corpus, then uses the classifier, along with extracted background knowledge, to determine whether events in the given text are causal or non-causal.

Bibliography
============

Our bibliography for this project can be found at: https://github.com/hammadmazhar1/CausalRelExtractor/blob/master/Documentation/Bibliography/Bibliography.md

How To Run
==========

WordNet must be installed before running code

Export environment variable WNHOME before running code. WNHOME is the Wordnet directory
for Windows it would look like "C:\Program Files\WordNet-3.0"
and for Linux/Unix it would look like "/usr/local/WordNet-3.0"

To run individual files,
	javac -cp "lib\*" *.java
	java -Xmx512m -cp ".;lib\*" filename

To run the entire package,
	cd to the main causalrelextractor directory
	./run.sh

The Code
========

splitter.cpp
------------
* Helper file which took in a single text file containing many articles and outputted the separate articles to separate files.

Class_Pair.java, Class_Word_Count.java, Class_Word_Pair.java
------------------------------------------------------------
* Helper classes, contain the data structures used.

create_explicit_corpus.java
---------------------------
* Takes in a collection of files
* Creates a dictionary of all words in these files, with their counts.
	* Outputs to count_words.txt
* Extracts verb-verb pairs on the basis of explicit causal and non-causal markers
	* Outputs their counts to count_verb_verb.txt (for reference purposes, not important)
	* Outputs their counts along with their sentences, the cause and effect in that sentence, and what context they appear in (causal or non-causal marker in the sentence) to input_features_tagged.txt

create_verb_verb_pairs_with_sentences.java
------------------------------------------
* Takes in a collection of files
* Extracts all possible verb-verb pairs
	* Outputs their counts along with their sentences and what context they appear in to input_features.txt

generate_features.java
----------------------
* Takes in a file with verb-verb pairs (with their counts, sentences, etc. - input_features.txt, input_features_tagged.txt)
* Generates an output file with the provided filename which contains data formatted in a way which can be fed to the supervised Mallet Classifier.

gen_event_features.java
-----------------------
* Takes in a file with verb-verb pairs (with their counts, sentences, etc. - input_features.txt, input_features_tagged.txt)
* Generates an output file with the provided filename which contains data formatted in a way which can be fed to the supervised Mallet Classifier.

TimeMLParser.java
-----------------
* Takes in TimeML annotated files to extract event and non-event instances.