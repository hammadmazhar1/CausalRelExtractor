cd SProj

#clear all files
./clean.sh

#run java code in SProj folder
echo "Running SProj code"
./run.sh

cd ..

cp SProj/output*.txt Mallet/

cd Mallet
#import data into mallet data
echo "Importing data into Mallet"
bin/mallet import-file --input output_features_tagged.txt --output linguistic_causal_train.mallet
bin/mallet import-file --input output_event_training.txt --output linguistic_event_train.mallet
bin/mallet import-file --input output_features_tagged_cause_effect.txt --output cause_effect_train.mallet
#train classifiers
echo "Training classifiers"
bin/mallet train-classifier --input linguistic_causal_train.mallet --output-classifier linguistic_causal.classifier --trainer MaxEnt
bin/mallet train-classifier --input linguistic_event_train.mallet --output-classifier linguistic_event.classifier --trainer MaxEnt
bin/mallet train-classifier --input cause_effect_train.mallet --output-classifier cause_effect.classifier
#classify on test data

echo "Classifying test data"
bin/csv2classify --input output_features.txt --classifier linguistic_causal.classifier --output ling_causal_res.txt
bin/csv2classify --input output_event_features.txt --classifier linguistic_event.classifier --output ling_event_res.txt
bin/csv2classify --input output_features_cause_effect.txt --classifier cause_effect.classifier --output cause_effect_res.txt