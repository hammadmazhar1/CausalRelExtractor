#compile all files
echo "Compiling code"
javac -cp ".:lib/*" *.java

#create explicit corpus
echo "Creating explicit corpus"
java -cp ".:lib/*" create_explicit_corpus

#create features for training linguistic verb-verb causal classifier
echo "generating training data for linguistic causal classifier"
java -cp ".:lib/*" generate_features l input_features_tagged.txt output_features_tagged.txt

#create verb-verb pairs from documents to be analysed
echo "Analysing test documents"
java -cp ".:lib/*" create_verb_verb_pairs_with_sentences

#generate features for classsifying with linguistic verb-verb causal classifier
echo "Generating features from test data"
java -cp ".:lib/*" generate_features input_features.txt output_features.txt

#create features for train linguistic event classifier
echo "Generating features for linguistic event classifier training"
java -cp ".:lib/*" TimeMLParser timebank_1_2/data/timeml/ output_event_training.txt

#create features for classifying events on testing data
echo "Generating features from test data for linguistic event classification"
java -cp ".:lib/*" gen_event_features input_features.txt output_event_features.txt
