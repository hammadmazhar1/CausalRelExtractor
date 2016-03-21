cd SProj

#Delete .class and .txt files in SProj
echo "Clearing SProj Directory"
rm *.class
rm *.txt

cd Mallet
#import data into mallet data
rm *.txt
rm *.classifier
rm *.mallet