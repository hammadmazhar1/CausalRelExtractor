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

import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.parser.nndep.DependencyParser;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.*;
import edu.mit.jwi.*;
import edu.mit.jwi.item.*;
import edu.mit.jwi.morph.*;

//=================================================================================
//=================================================================================

public class generate_features {
	static List<File>       all_files         = new ArrayList<>();
  static List<Word_Pair>  all_verb_pairs    = new ArrayList<>();
  static List<Word_Count> doc_count_words   = new ArrayList<>();
  static List<String>     phrases_all       = Arrays.asList("because", "for this reason", "for that reason", "consequently", "as a consequence of", "as a result of", "but", "in short", "in other words", "whereas", "on the other hand", "nevertheless", "nonetheless", "in spite of", "in contrast", "however", "even", "though", "despite the fact", "conversely", "although");
  static List<Integer>    length_phrases_all= Arrays.asList(1, 3, 3, 1, 4, 4, 1, 2, 3, 1, 4, 1, 1, 3, 2, 1, 1, 1, 3, 1, 1);
  static String           dirName           = System.getProperty("user.dir") + "\\textfiles\\test";
  static String           uDirName           = System.getProperty("user.dir") + "/textfiles/test";
  static String           modelFile         = "models\\english-left3words-distsim.tagger";
  static PrintWriter      pw                = null;
  static int              totalNumWords     = 0;
  static int              totalSentences    = 0;

  //=================================================================================
  //=================================================================================

  public static Word_Count find_WC(String word) {
    for (Word_Count wc : doc_count_words) {
      if (wc.word.equals(word)) {
        return wc;
      }
    }

    return null;
  }

  public static List<Word_Pair> find_WP(String word) {
    List<Word_Pair> wp_list = new ArrayList<>();

    for (Word_Pair wp : all_verb_pairs) {
      if (wp.word_one.equals(word) || wp.word_two.equals(word)) {
        wp_list.add(wp);
      }
    }

    return wp_list;
  }

  //=================================================================================
  //=================================================================================

  public static List<Pair> findPhraseLocations(List<TaggedWord> tSentence) {
    List<Pair> phraseLocations = new ArrayList<>();

    // Go through each tagged word in the sentence.
    for (int i = 0; i < tSentence.size(); i++) {

      for (int k = 0; k < phrases_all.size(); k++) {
        List<String> phrase = Arrays.asList(phrases_all.get(k).split(" "));

        // Find the position of the (non-)causal phrase.
        boolean found = false;
        if (tSentence.get(i).word().toLowerCase().equals(phrase.get(0))) {
          found = true;
          if (phrase.size() > 1) {
            for (int j = 1; j < phrase.size(); j++) {
              if (tSentence.size() <= i+j || !tSentence.get(i+j).word().toLowerCase().equals(phrase.get(j))) {
                found = false;
                break;
              }
            }
          }

          if (found) {
            phraseLocations.add(new Pair(i, length_phrases_all.get(k)));
          }
        }    
      }
    }

    return phraseLocations;
  }

  public static List<Word_Pair> findVerbPairs(List<TaggedWord> tSentence) {
    List<Word_Pair> returnValue = new ArrayList<>();
    List<Pair> phraseLocations = findPhraseLocations(tSentence);
    int start = 0;
    int end = 0;

    for (int i = 0; i < phraseLocations.size(); i++) {
      if (i == phraseLocations.size() - 1) {
        end = tSentence.size();
      } else {
        end = phraseLocations.get(i+1).x;
      }

      List<String> verbsBefore = new ArrayList<>();
      List<String> verbsAfter  = new ArrayList<>();

      // Check for verbs occurring before the unambiguous discourse marker.
      for (int j = start; j < phraseLocations.get(i).x; j++) {
        if (tSentence.get(j).tag().startsWith("VB")) {
          verbsBefore.add(tSentence.get(j).word());

          if (find_WC(tSentence.get(j).word()) == null) {
              pw.println("\nWORD DOES NOT EXIST IN DATABASE: " + tSentence.get(j).word() + "\n");
            doc_count_words.add(new Word_Count(tSentence.get(j).word()));
          }
        }
      }

      // Check for verbs occurring after the unambiguous discourse marker.
      for (int j = phraseLocations.get(i).x+phraseLocations.get(i).y; j < end; j++) {
        if (tSentence.get(j).tag().startsWith("VB")) {
          verbsAfter.add(tSentence.get(j).word());

          if (find_WC(tSentence.get(j).word()) == null) {
              pw.println("\nWORD DOES NOT EXIST IN DATABASE: " + tSentence.get(j).word() + "\n");
            doc_count_words.add(new Word_Count(tSentence.get(j).word()));
          }
        }
      }

      start = phraseLocations.get(i).x + phraseLocations.get(i).y;

      // If verbs exist both before and after the discourse marker, form all possible pairs of them.
      for (String s1 : verbsBefore) {
        for (String s2 : verbsAfter) {

          // if the verbs are different
          if (!s1.toLowerCase().equals(s2.toLowerCase())) {
            Word_Pair wp = new Word_Pair(s1, s2);
            returnValue.add(wp);

            if (!all_verb_pairs.contains(wp)) {
              pw.println("\nVERB-VERB PAIR DOES NOT EXIST IN DATABASE: " + wp.word_one + "-" + wp.word_two + "\n");
              all_verb_pairs.add(wp);
            }
          }
        }
      }
    }
    return returnValue;
  }

  //=================================================================================
  //=================================================================================

  /**
   * words, lemmas, part-of-speech tags and all senses of both verbs from WordNet.
   */
  static List<String> feature_verbs(Word_Pair wp, List<TaggedWord> tSentence) {
    List<String> returnValue = new ArrayList<>();
    
    // construct the URL to the Wordnet dictionary directory
    String wnhome = System.getenv("WNHOME");
    String path = wnhome + File.separator + "dict";
    URL url = null;
    try{ url = new URL("file", null, path); } 
    catch(MalformedURLException e){ e.printStackTrace(); }
    if(url == null) return null;
    
    // construct the dictionary object and open it
    IDictionary dict = new Dictionary(url);
    try {dict.open();}
    catch(IOException e){e.printStackTrace();}
    
    // look at stems for word one
    WordnetStemmer stemmer = new WordnetStemmer(dict);
    List<String> strings = stemmer.findStems(wp.word_one,POS.VERB);
    String lemma = null;
    if (strings.isEmpty())
    	lemma = wp.word_one;
    else lemma = strings.get(0);

    IIndexWord idxWord = dict.getIndexWord(lemma, POS.VERB);

    // add word one, its lemma, POS tag and sense keys
    returnValue.add(wp.word_one);
    returnValue.add(lemma);
    for (int i = 0; i < tSentence.size(); i++) {
      if (tSentence.get(i).value().equals(wp.word_one)) {
        returnValue.add(tSentence.get(i).tag());
        break;
      }
    }
    if (idxWord != null) {
    	for (int i = 0; i <  idxWord.getWordIDs().size(); i++){
      		IWordID wordID = idxWord.getWordIDs().get(i);
      		IWord word = dict.getWord(wordID);
      		returnValue.add(word.getSenseKey().toString());
    	}
    }
    
    // look at stems for word 2
    strings = stemmer.findStems(wp.word_two,POS.VERB);
    if (strings.isEmpty()) 
    	lemma = wp.word_two;
    else lemma = strings.get(0);

    idxWord = dict.getIndexWord(lemma, POS.VERB);

    //add word two, its lemma, POS tag and sense keys
    returnValue.add(wp.word_two);
    returnValue.add(lemma);

    for (int i = 0; i < tSentence.size(); i++) {
      if (tSentence.get(i).value().equals(wp.word_two)) {
        returnValue.add(tSentence.get(i).tag());
        break;
      }
    }
    if (idxWord != null) {
    	for (int i = 0; i <  idxWord.getWordIDs().size(); i++){
		    IWordID wordID = idxWord.getWordIDs().get(i);
      		IWord word = dict.getWord(wordID);
    		returnValue.add(word.getSenseKey().toString());
	   	}
    }
  	return returnValue;
  }

  /**
   * [GetWords description]
   * @param  parse [description]
   * @return       [description]
   */
  public static List<TaggedWord> GetWords(Tree parse) {
  	MaxentTagger tagger;
  	String taggerPath = "models\\english-left3words-distsim.tagger";
  	tagger = new MaxentTagger(taggerPath);
    List<String> list = Arrays.asList("CC", "CD", "DT", "EX", "FW", "IN", "JJ", "JJR", "JJS", "LS", "MD", "NN", "NNS", "NNP", "NNPS", "PDT", "POS", "PRP", "RB", "RBR", "RBS", "RP", "SYM", "TO", "UH", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ", "WDT", "WP", "WRB", "WP$", "PRP$", "ADJP", "ADVP", "CONJP", "FRAG", "INTJ", "LST", "NAC", "NP", "NX", "PP", "PRN", "PRT", "QP", "RRC", "UCP", "VP", "WHADJP", "WHAVP", "WHNP", "WHPP", "X");
    String s = "";
    for (Tree subtree: parse) {
      if (!list.contains(subtree.value())) {
        s += subtree.value() + " ";
      }
  	}
    DocumentPreprocessor tokenizer = new DocumentPreprocessor(new StringReader(s));
    for (List<HasWord> sentence : tokenizer) {
      List<TaggedWord> tSentence = tagger.tagSentence(sentence);
      return tSentence;
    }
    return null;
  }

  /**
   * words, lemmas, part-of-speech tags and all senses of the words of both verb phrases. We take 
   * senses from Word for only verbs and nouns. In order to collect the verb phrases, we use Stanford's 
   * syntactic parser
   */
  static List<String> feature_verbPhrases(Word_Pair wp, List<TaggedWord> tSentence) {
    // form wordnet url
    String wnhome = System.getenv("WNHOME");
    String path = wnhome + File.separator + "dict";
    URL url = null;
    try{ url = new URL("file", null, path); } 
    catch(MalformedURLException e){ e.printStackTrace(); }
    if(url == null) return null;
    
    // construct the dictionary object and open it
    IDictionary dict = new Dictionary(url);
    try {dict.open();}
    catch(IOException e){e.printStackTrace();}
    WordnetStemmer stemmer = new WordnetStemmer(dict);

    List<String> returnValue = new ArrayList<>();
    String parserModel = "models\\englishPCFG.ser.gz";
    LexicalizedParser lp = LexicalizedParser.loadModel(parserModel);
    List<HasWord> sentence = Sentence.toWordList((Sentence.listToString(tSentence,false)));
    
    Tree parse = lp.apply(sentence);
    for (Tree subTree : parse) {
    	if (subTree.label().value().equals("VP")) {
    		Tree temp = subTree.getChild(0).getChild(0);
        if (temp.value().equals(wp.word_one) || temp.value().equals(wp.word_two)) {
        	List<TaggedWord> phraseSent = GetWords(subTree);
        	for (TaggedWord word : phraseSent) {
        		// if word is a verb
        		if (word.tag().contains("VB")) {
        			// find word stem
        			List<String> strings = stemmer.findStems(word.value(),POS.VERB);
        			String lemma = null;
        			if (strings.isEmpty())
        				lemma = word.value();
        			else lemma = strings.get(0);
        			IIndexWord idxWord = dict.getIndexWord(lemma, POS.VERB);
						  //add word, its lemma, POS tag and sense keys
        			returnValue.add(word.value());
	            returnValue.add(lemma);
       				returnValue.add(word.tag());
       				if (idxWord != null) {
     						for (int i = 0; i <  idxWord.getWordIDs().size(); i++){
      							IWordID wordID = idxWord.getWordIDs().get(i);
    	  						IWord iword = dict.getWord(wordID);
	      						returnValue.add(iword.getSenseKey().toString());
    						}
    					}

    				// word is a noun
      			} else if(word.tag().contains("NN")) {
      				List<String> strings = stemmer.findStems(word.value(),POS.NOUN);
      				String lemma = null;
      				if (strings.isEmpty())
      					lemma = word.value();
        			else lemma = strings.get(0);

	            IIndexWord idxWord = dict.getIndexWord(lemma, POS.NOUN);

        			// add word, its lemma, POS tag and sense keys
        			returnValue.add(word.value());
        			returnValue.add(lemma);
        			returnValue.add(word.tag());
        			if (idxWord != null) {
   		     			for (int i = 0; i <  idxWord.getWordIDs().size(); i++) {
    							IWordID wordID = idxWord.getWordIDs().get(i);
    							IWord iword = dict.getWord(wordID);
    							returnValue.add(iword.getSenseKey().toString());
    						}
    					}
  	        } else {
      				List<String> strings = stemmer.findStems(word.value(),null);
      				String lemma = null;
      				if (strings.isEmpty()) 
      					lemma = word.value();
        			else lemma = strings.get(0);
        			//add word, its lemma and POS tag
        			returnValue.add(word.value());
        			returnValue.add(lemma);
        			returnValue.add(word.tag());
      			}
          }
        }
      }
    }
    return returnValue;
  }

  /**
   * words, lemmas, part-of-speech tags and all senses of the subject and object of both verbs.
   */
  static List<String> feature_verbArguments(Word_Pair wp, List<TaggedWord> tSentence) {
  	List<String> returnValue = new ArrayList<String>();
  	DependencyParser parser = DependencyParser.loadFromModelFile("models\\english_UD.gz");
  	GrammaticalStructure gs = parser.predict(tSentence);
  	String wnhome = System.getenv("WNHOME");
    String path = wnhome + File.separator + "dict";
    URL url = null;
    try{ url = new URL("file", null, path); } 
    catch(MalformedURLException e){ e.printStackTrace(); }
    if(url == null) return null;
    
    // construct the dictionary object and open it
    IDictionary dict = new Dictionary(url);
    try {dict.open();}
    catch(IOException e){e.printStackTrace();}

  	List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
  	for (TypedDependency td : tdl) {
  		if (td.gov().value().equals(wp.word_one) || td.gov().value().equals(wp.word_two)) {
  			String relation = td.reln().getShortName();
  			if (relation.contains("obj")){
  				String posTag = null;
  				for (TaggedWord t : tSentence) {
  					if (t.value().equals(td.dep().value())){
  						posTag = t.tag();
  						break;
  					}
  				}
  				String lemma = td.dep().lemma();
  				if (lemma == null) {
  					lemma = td.dep().value();
  				}
  				String entry = "Object_" + td.gov().value() +"=" + td.dep().value() + "," + lemma + "," + posTag;
  				IIndexWord idxWord = dict.getIndexWord(td.dep().value(), POS.NOUN);
  				if (idxWord != null){
  					for (int i = 0; i <  idxWord.getWordIDs().size(); i++){
      					IWordID wordID = idxWord.getWordIDs().get(i);
      					IWord iword = dict.getWord(wordID);
      					entry = entry + "," + iword.getSenseKey().toString();
    				}
    			}
    			returnValue.add(entry);
  			} else if(relation.contains("subj")) {
  				String posTag = null;
  				for (TaggedWord t : tSentence) {
  					if (t.value().equals(td.dep().value())){
  						posTag = t.tag();
  						break;
  					}
  				}
  				String lemma = td.dep().lemma();
  				if (lemma == null) {
  					lemma = td.dep().value();
  				}
  				String entry = "Subject_" + td.gov().value() +"=" + td.dep().value() + "," + lemma + "," + posTag;
  				IIndexWord idxWord = dict.getIndexWord(td.dep().value(), POS.NOUN);
  				if (idxWord != null){
  					for (int i = 0; i <  idxWord.getWordIDs().size(); i++){
      					IWordID wordID = idxWord.getWordIDs().get(i);
      					IWord iword = dict.getWord(wordID);
      					entry = entry + "," + iword.getSenseKey().toString();
    				}
    			}
    			returnValue.add(entry);
  			}
  		}
    }
  	return returnValue;
  }

  /**
   * For this feature, we take the cross product of both events of a pair ev_i-ev_j where 
   * ev_i = [subject_vi] vi [object_vj] and ev_j = [subject_vj] vj [object_vj].
   */
  static List<String> feature_verbsAndArgumentPairs(Word_Pair wp, List<TaggedWord> tSentence) {
  	// List<String> verb_Args = feature_verbArguments(wp.tSentence);
                                  List<String> verb_Args = new ArrayList<String>(); 
    // Delete above line
  	ArrayList<String> ev_i = new ArrayList<String>();
  	ArrayList<String> ev_j = new ArrayList<String>();
  	List<String> returnValue = new ArrayList<>();
  	for (String arg :verb_Args) {
  		if (arg.contains("Subject")) {
  			if (arg.contains(wp.word_one)) {
  				String[] subject = arg.split("=",2);
  				String[] subjDetails = subject[1].split(",",2);
  				ev_i.add(subjDetails[0]);
  				ev_i.add(wp.word_one);
  			}
  			if (arg.contains(wp.word_two)) {
  				String[] subject = arg.split("=",2);
  				String[] subjDetails = subject[1].split(",",2);
  				ev_j.add(subjDetails[0]);
  				ev_j.add(wp.word_two);
  			}
  		} else if (arg.contains("Object")) {
  			if (arg.contains(wp.word_two)) {
  				String[] object = arg.split("=",2);
  				String[] objDetails = object[1].split(",",2);
  				ev_i.add(objDetails[0]);
  				ev_j.add(objDetails[0]);
  			}
  		}
  	}
  	for (String i : ev_i){
  		for (String j: ev_j) {
  			returnValue.add(i+"-"+j);
  		}
  	}
  	return returnValue;
  }

  /**
   * lemmas of all words from the mincontext.
   */
  static List<TaggedWord> feature_contextWords(Word_Pair wp, List<TaggedWord> tSentence) {
  	List<TaggedWord> returnValue = new ArrayList<>();
  	List<Pair> phraseLocations = findPhraseLocations(tSentence);
  	int[] word_locations = new int[2];
  	word_locations[0] = -1;
  	word_locations[1] = -1;

  	for (int i = 0; i < tSentence.size(); i++) {
  		if (wp.word_one.equals(tSentence.get(i).word())) {
  			word_locations[0] = i;
  		}
  		if (wp.word_two.equals(tSentence.get(i).word())) {
  			word_locations[1] = i;
  		}
  	}

  	// Establish bounds for the words in the verb-verb pair (one of the pair may not exist in this sentence)
  	// Add all the words in the sentence between the bounds for each verb to a List and return it.
  	for (int word_location : word_locations) {
  		if (word_location != -1) {
	  		int lowerBound = 0;
	  		int upperBound = tSentence.size();
	  		for (Pair p : phraseLocations) {
	  			if (p.x+p.y > lowerBound && p.x+p.y < word_location) {
	  				lowerBound = p.x+p.y;
	  			} else if (p.x < upperBound && p.x > word_location) {
	  				upperBound = p.x;
	  			}
	  		}

	  		for (int i = lowerBound; i < upperBound; i++) {
	  			returnValue.add(tSentence.get(i));
	  		}
	  	}
  	}
	  return returnValue;
  }

  /**
   * all main verbs and their lemmas from the mincontext.
   */
  static List<TaggedWord> feature_contextMainVerbs(List<TaggedWord> minContext) {
  	List<TaggedWord> returnValue = new ArrayList<>();
  	for (TaggedWord tw : minContext) {
  		if (tw.tag().startsWith("VB")) {
  			returnValue.add(tw);
  		}
  	}
  	String wnhome = System.getenv("WNHOME");
    String path = wnhome + File.separator + "dict";
    URL url = null;
    try{ url = new URL("file", null, path); } 
    catch(MalformedURLException e){ e.printStackTrace(); }
    if(url == null) return null;
    
    // construct the dictionary object and open it
    IDictionary dict = new Dictionary(url);
    try {dict.open();}
    catch(IOException e){e.printStackTrace();}
    WordnetStemmer stemmer = new WordnetStemmer(dict);

  	// Look up lemmas of these verbs and add them to the list.
    for (TaggedWord i : returnValue){
    	List<String> lemList =stemmer.findStems(i.value(),POS.VERB);
    	if (lemList.isEmpty())
    		returnValue.add(i);
    	else
    		returnValue.add(new TaggedWord(lemList.get(0),i.tag()));
    }
  	return returnValue;
  }

  /**
   * the pairs of main verbs from the mincontext. The lemmas are taken from the feature 
   * "Context Main Verbs" and then the pairs on these lemmas are used as this feature.
   */
  static List<Word_Pair> feature_contextMainVerbPairs(List<TaggedWord> contextMainVerbs) {
  	List<Word_Pair> returnValue = new ArrayList<>();
  	for (int i = 0; i < contextMainVerbs.size(); i++) {
  		for (int j = 0; j < contextMainVerbs.size(); j++) {
	  		if (i != j) {
	  			Word_Pair wp = new Word_Pair(contextMainVerbs.get(i).word(), contextMainVerbs.get(j).word());
	  			returnValue.add(wp);
	  		}
	  	}		
  	}
  	return returnValue;
  }

  //=================================================================================
  //=================================================================================

  static void iterateFiles(File[] files) {
    for (File file : files) {
      if (file.isDirectory()) {
        iterateFiles(file.listFiles());
      } else if (file.isFile()) {
        if (file.getPath().endsWith(".txt")) {
          all_files.add(file);
        }
      }
    }
  }

	public static void main(String[] args) throws Exception {
		pw = new PrintWriter(new OutputStreamWriter(System.out, "utf-8"));
		//set Wordnet directory
		

    // Populate the Words and Verb-Verb pair data.
    try {
      Scanner scanner = new Scanner(new File("dictionary.txt"));
      totalNumWords = scanner.nextInt();
      totalSentences = scanner.nextInt();
      while (scanner.hasNextLine()) {
        int i1 = scanner.nextInt();
        int i2 = scanner.nextInt();
        String s = scanner.next();
        Word_Count wc = new Word_Count(s, i1, i2);
        doc_count_words.add(wc);
      }
      scanner.close();

      scanner = new Scanner(new File("verb-verb.txt"));
      while (scanner.hasNextLine()) {
        int i1 = scanner.nextInt();
        int i2 = scanner.nextInt();
        String s1 = scanner.next();
        String s2 = scanner.next();
        s2 = scanner.next();
        Word_Pair wp = new Word_Pair(s1, s2, i1, i2);
        all_verb_pairs.add(wp);
      }
      scanner.close();      
    } catch (Exception e) {
      System.out.println("\nAN EXCEPTION OCCURRED:\n" + e.toString() + "\n");
      System.exit(0);
    }
    
    // Open the file which has to be analyzed.
    File[] files = null;
    if (System.getProperty("os.name").toLowerCase().contains("windows")) 
      files = new File(dirName).listFiles();
    else
      files = new File(uDirName).listFiles();
    iterateFiles(files);

    // The main class for users to run, train, and test the part of speech tagger.
    // http://www-nlp.stanford.edu/nlp/javadoc/javanlp/edu/stanford/nlp/tagger/maxent/MaxentTagger.html
    MaxentTagger tagger = new MaxentTagger(modelFile);

    // A fast, rule-based tokenizer implementation, which produces Penn Treebank style tokenization of English text.
    // http://nlp.stanford.edu/nlp/javadoc/javanlp/edu/stanford/nlp/process/PTBTokenizer.html
    TokenizerFactory<CoreLabel> ptbTokenizerFactory = PTBTokenizer.factory(new CoreLabelTokenFactory(), "untokenizable=noneKeep");

    // Go through each file in the list.
    for (int id = 0; id < all_files.size(); id++) {

      // Open the file.
      BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(all_files.get(id)), "utf-8"));
      
      // Produces a list of sentences from the document.
      // http://nlp.stanford.edu/nlp/javadoc/javanlp/edu/stanford/nlp/process/DocumentPreprocessor.html
      DocumentPreprocessor documentPreprocessor = new DocumentPreprocessor(r);
      documentPreprocessor.setTokenizerFactory(ptbTokenizerFactory);

      // Go through each sentence in the document.
      for (List<HasWord> sentence : documentPreprocessor) {
        
        // Print the sentence
        String sentenceString = Sentence.listToString(sentence, false).toLowerCase();
        pw.println(sentenceString);

        // Tag each sentence, producing a list of tagged words.
        List<TaggedWord> tSentence = tagger.tagSentence(sentence);

        // Print the tagged sentence.
        pw.println(Sentence.listToString(tSentence, false));

      }
    }

    pw.close();
	}
}
