### Stanford POS Tagger
	javac -cp lib\* filename.java
	java -cp ".;lib\*" filename > out.txt

### Stanford Parser
	javac -cp lib\* test.java
	java -Xmx512m -cp ".;lib\*" test > out.txt