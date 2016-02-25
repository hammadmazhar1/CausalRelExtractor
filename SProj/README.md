CausalRelExtractor
==================

This is an implemtation of Mehwish Riaz's PhD thesis

How To Run
==========

Stanford POS Tagger
-------------------
WordNet must be installed before running code

Export environment variable WNHOME before running code. WNHOME is the Wordnet directory
for Windows it would look like "C:\Program Files\WordNet-3.0"
and for Linux/Unix it would look like "/usr/local/WordNet-3.0"

	javac -cp "lib\*" *.java
	java -cp ".;lib\*" filename

Stanford Parser	
---------------

	javac -cp "lib\*" *.java
	java -Xmx512m -cp ".;lib\*" filename
