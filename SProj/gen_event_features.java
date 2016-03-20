import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Scanner;
import java.util.Set;
import java.net.*;
import java.io.*;


import edu.mit.jwi.*;
import edu.mit.jwi.item.*;
import edu.mit.jwi.morph.*;

public class gen_event_features{
	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.out.println("Usage: java -cp \".:lib/*\" gen_event_features <inputfilename> <outputfilename> ");
			return;
		}
		String inputFile = null;
		String outputFile = null;
		int instance = 0;
		inputFile = args[0];
		outputFile = args[1];
		List<String> hypernyms = Arrays.asList("think","cogitate","cerebrate","move","displace","act","move");
		//instantiate Wordnet Dictionary
		String wnhome = System.getProperty("user.dir");
    	String path = wnhome + File.separator + "dict";
    	URL url = null;
    	try{ url = new URL("file", null, path); } 
    	catch(MalformedURLException e){ e.printStackTrace(); }
    	if(url == null) return;
    
    	// construct the dictionary object and open it
    	IDictionary dict = new Dictionary(url);
    	try {dict.open();}
    	catch(IOException e){e.printStackTrace();}
    	WordnetStemmer stemmer = new WordnetStemmer(dict);

		PrintWriter pw = new PrintWriter(new File(outputFile));
    	PrintWriter refPw = new PrintWriter(new File("pair_ref_event.txt"));
    	try {
  			Scanner scanner = new Scanner(new File(inputFile));
  			while (scanner.hasNextLine()) {
		  		String verb_pair = scanner.nextLine();
		  		//System.out.println(verb_pair);
    			String[] verbs_pair = verb_pair.split(" ",3);
    			Word_Pair wp = new Word_Pair(verbs_pair[0],verbs_pair[2],0,0);
          		scanner.nextLine();
          		scanner.nextLine();
          		int sentences = Integer.parseInt(scanner.nextLine());
    			//System.out.println(sentences);
    			for (int i = 0; i < sentences; i++) {
    				String s = scanner.nextLine();
            		instance++;
    				//System.out.println(s);
    				// add word one, its lemma, POS tag and sense keys
    				List<String> strings = stemmer.findStems(wp.word_one,POS.VERB);
    				String lemma = null;
    				if (strings.isEmpty())
    					lemma = wp.word_one;
    				else lemma = strings.get(0);
    				String affix = wp.word_one.replace(lemma,"");
    				if (affix.equals("")) {
    					affix = "null";
    				} else if (affix.endsWith("ing")) {
    					affix = "ing";
    				} else if (affix.endsWith("id")) {
    					affix = "id";
    				}
    				refPw.println(instance + "i " +wp.word_one);
    				pw.print(instance + "i " + wp.word_one + " " + lemma + " " + affix + " VERB ");
    				IIndexWord idxWord = null;
					idxWord = dict.getIndexWord(lemma, POS.VERB);
					ArrayList<String> synsets = new ArrayList<String>();
					int hypernym_feature = 0;
					String sense = null;
					if (idxWord != null) {
    					for (int k = 0; k <  idxWord.getWordIDs().size(); k++){
   							IWordID wordID = idxWord.getWordIDs().get(k);
   							IWord iword = dict.getWord(wordID);
   							if (k == 0) 
								sense = iword.getSenseKey().toString();
      						synsets.add(iword.getSynset().getWord(1).toString());
    						
    					}
    					for (String synset : synsets) {
    						for (String hypernym : hypernyms) {
    							if (synset.contains(hypernym)) {
    								hypernym_feature = 1;
    								break;
    							}
    						}
    					}
						//print semantic class features
						if (sense == null) 
							pw.print("null ");
						else
							pw.print(sense + " ");
						//print hypernym features
						pw.print(hypernym_feature+ "\n");
						pw.flush();
						refPw.flush();
					} else {
						pw.print("null 0\n");
					}
					// add word two and its features
					strings = stemmer.findStems(wp.word_two,POS.VERB);
    				lemma = null;
    				if (strings.isEmpty())
    					lemma = wp.word_two;
    				else lemma = strings.get(0);
    				affix = wp.word_two.replace(lemma,"");
    				if (affix.equals("")) {
    					affix = "null";
    				}  else if (affix.endsWith("ing")) {
    					affix = "ing";
    				} else if (affix.endsWith("id")) {
    					affix = "id";
    				}
    				refPw.println(instance + "j " +wp.word_two);
    				pw.print(instance + "j " + wp.word_two + " " + lemma + " " + affix + " VERB ");
    				idxWord = null;
					idxWord = dict.getIndexWord(lemma, POS.VERB);
					synsets = new ArrayList<String>();
					hypernym_feature = 0;
					sense = null;
					if (idxWord != null) {
    					for (int k = 0; k <  idxWord.getWordIDs().size(); k++){
   							IWordID wordID = idxWord.getWordIDs().get(k);
   							IWord iword = dict.getWord(wordID);
   							if (k == 0) 
								sense = iword.getSenseKey().toString();
      						synsets.add(iword.getSynset().getWord(1).toString());
    						
    					}
    					for (String synset : synsets) {
    						for (String hypernym : hypernyms) {
    							if (synset.contains(hypernym)) {
    								hypernym_feature = 1;
    								break;
    							}
    						}
    					}
						//print semantic class features
						if (sense == null) 
							pw.print("null ");
						else
							pw.print(sense + " ");
						//print hypernym features
						pw.print(hypernym_feature+ "\n");
						pw.flush();
						refPw.flush();
					} else {
						pw.print("null 0\n");
					}
    			}
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    		return;
    	}
    	pw.close();
    	refPw.close();
	}
}