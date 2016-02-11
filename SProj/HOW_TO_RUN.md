### Stanford POS Tagger
WordNet must be installed before running code

export environment variable WNHOME before running code. WNHOME is the Wordnet directory
for Windows it would look like "C:\Program Files\WordNet-3.0"
and for Linux/Unix it would look like "/usr/local/WordNet-3.0"

	javac -cp lib\* filename.java
	java -cp ".;lib\*" filename > out.txt

### Stanford Parser
	javac -cp lib\* test.java
	java -Xmx512m -cp ".;lib\*" test > out.txt