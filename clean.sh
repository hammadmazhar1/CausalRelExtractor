#Delete .class and .txt files in SProj
cd SProj
echo "Clearing SProj Directory"
rm *.class
rm *.txt

#Delete .txt, .classifier and .mallet files in SProj
cd ../Mallet
echo "Clearing Mallet Directory"
rm *.txt
rm *.classifier
rm *.mallet