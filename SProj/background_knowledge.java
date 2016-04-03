import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Scanner;
import java.util.Set;
import java.net.*;
import java.io.*;
import java.util.NoSuchElementException;

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

public class background_knowledge {
	static int              numFiles          = 0;
  static List<Word_Count> doc_count_words   = new ArrayList<>();
  static List<Word_Pair>  all_verb_pairs    = new ArrayList<>();
	static int              totalNumWords     = 0;
  static int              totalSentences    = 0;
  static String           dirName           = System.getProperty("user.dir") + "\\textfiles\\test";
  static String           uDirName          = System.getProperty("user.dir") + "/textfiles/test";
  static HashMap<Integer, Word_Pair> all_verb_pairs_hashmap = new HashMap<Integer, Word_Pair>();
  
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

  public static Word_Pair find_WP(Word_Pair wp1) {
    for (Word_Pair wp2 : all_verb_pairs) {
      if (wp1.equals(wp2)) {
        return wp2;
      }
    }

    return null;
  }

  public static Word_Pair find_WP(int id) {
    return all_verb_pairs_hashmap.get(id);
  }

  //=================================================================================
  //=================================================================================

  public static double IDF(Word_Pair wp) {
    double one = idf(wp.word_one);
    double two = idf(wp.word_two);
    double three = idf(wp);

  	// Word doesn't exist.
    if (one == -1 || two == -1) {return -1;}
    return one * two * three;
  }

  public static double idf(String word) {
  	Word_Count wc = find_WC(word);
  	if (wc == null) {return -1;}
    double ans = 1.0 + wc.documentCount;
    return numFiles / ans;
  }

  public static double idf(Word_Pair wp) {
    double ans = 1.0 + wp.documentCount;
    return numFiles / ans;
  }

  //=================================================================================
  //=================================================================================

  public static double P(String word) {
    Word_Count wc = find_WC(word);

  	// Word doesn't exist.
    if (wc == null) {return -1;}
    return ((double)wc.actualCount) / ((double)totalNumWords);
  }

  public static double P(Word_Pair wp) {
    return ((double)wp.actualCount) / ((double)totalSentences);
  }

  public static double PMI(Word_Pair wp) {
  	double word_one_p = P(wp.word_one);
  	double word_two_p = P(wp.word_two);
    // System.out.println("pmi => " + Double.toString(word_one_p) + " " + Double.toString(word_two_p));
  	// Word doesn't exist.
  	if (word_one_p == -1 || word_two_p == -1) {return -1;}
    return Math.log(P(wp) / (word_one_p * word_two_p));
  }

  //=================================================================================
  //=================================================================================

  public static double CD(Word_Pair wp) {
  	double _pmi = PMI(wp);
  	// System.out.println("pmi = " + Double.toString(_pmi));
  	double _idf = IDF(wp);
  	// System.out.println("idf = " + Double.toString(_idf));
  	double _max = max(wp);
  	// System.out.println("max = " + Double.toString(_max));
  	
  	// Word doesn't exist.
  	if (_pmi == -1 || _idf == -1) {return -1;}
    return _pmi * _max * _idf;
  }

  //=================================================================================
  //=================================================================================

  public static double max_helper(List<Word_Pair> wp_list) {
    double max = 0.0;

    for (Word_Pair wp : wp_list) {
      if (P(wp) > max) {
        max = P(wp);
      }
    }

    return max;
  }

  public static double max(Word_Pair wp) {
    double p_vi_vj = P(wp);
    double epsilon = 0.01;

    List<Word_Pair> vi_vk = find_WP(wp.word_one);
    List<Word_Pair> vj_vk = find_WP(wp.word_two);

    double max_vi_vk = max_helper(vi_vk);
    double max_vj_vk = max_helper(vj_vk);

    double val1 = p_vi_vj / (max_vi_vk - p_vi_vj + epsilon);
    double val2 = p_vi_vj / (max_vj_vk - p_vi_vj + epsilon);

    return Math.max(val1, val2);
  }

  //=================================================================================
  //=================================================================================

  /**
   * The goal of ECA is to combine the unsupervised causal dependency score (i.e., CD) 
   * with the supervised score of instance I of belonging to the cause class than the 
   * non-cause one (i.e., CI ).
   */
 	public static double ECA(Word_Pair wp) {
 		double factor = 1.0 / all_verb_pairs.size();
 		double cd = CD(wp);
    // System.out.println("cd = " + Double.toString(cd));
  	// Word doesn't exist.
 		if (cd == -1) {return -1;}
    
 		double sum = 0.0;
 		for (int i = 0; i < wp.causal.size(); i++) {
 			sum += C_i(wp, i);
 		}

 		return factor * cd * sum;
 	}

	//=================================================================================
  //=================================================================================

 	/**
 	 * ICA handles the problem of training data sparseness
 	 */
 	public static double ICA(Word_Pair wp) {
 		double factor = 1.0 / all_verb_pairs.size();
 		double cd = CD(wp);

  	// Word doesn't exist.
 		if (cd == -1) {return -1;}

 		double sum = 0.0;
 		for (int i = 0; i < wp.causal.size(); i++) {
 			sum += C_i(wp, i) * ERM(wp, i);
 		}

 		return factor * cd * sum;	
 	}

  //=================================================================================
  //=================================================================================

 	/**
 	 * BCA combines ECA and ICA
 	 */
 	public static double BCA(Word_Pair wp) {
 		double factor = 1.0 / all_verb_pairs.size();
 		double cd = CD(wp);

  	// Word doesn't exist.
 		if (cd == -1) {return -1;}

 		double sum = 0.0;
 		for (int i = 0; i < wp.causal.size(); i++) {
 			sum += C_i(wp, i) * (1.0 + ERM(wp, i));
 		}

 		return factor * cd * sum;
 	}

  //=================================================================================
  //=================================================================================

 	public static double C_i(Word_Pair wp, int i) {
    return wp.causal.get(i) / wp.noncausal.get(i);
 	}
 
 	public static double ERM(Word_Pair wp, int i) {
 		Word_Count w1 = find_WC(wp.word_one);
 		Word_Count w2 = find_WC(wp.word_two);

 		// Word doesn't exist.
 		if (w1 == null || w2 == null) {return -1;}
 		double one = S(w1, 0) + S(w2, 1);
 		double two = S(w1, 1) + S(w2, 0);

 		return -1.0 * Math.max(one, two);
 	}

 	public static double S(Word_Count w, int label) {
 		if (label == 0) {
 			return 0.0;
 		} else {
 			return 0.0;
 		}
 	}

	//=================================================================================
  //=================================================================================

 	public static void populateWords() throws Exception {
 		Scanner scanner = new Scanner(new File("count_words.txt"));
    numFiles = scanner.nextInt();
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
 	}

 	public static void populateVerbVerbPairs() throws Exception {
 		Scanner scanner = new Scanner(new File("input_features.txt"));
		while (scanner.hasNextLine()) {
	  	String verb_pair = scanner.nextLine();
	  	
	  	if (!verb_pair.equals("\n")) {
  			String[] verbs_pair = verb_pair.split(" ",3);
  			int document = Integer.parseInt(scanner.nextLine());
  			int actual = Integer.parseInt(scanner.nextLine());
  			Word_Pair wp = new Word_Pair(verbs_pair[0],verbs_pair[2],document,actual);
  			int sentences = Integer.parseInt(scanner.nextLine());
  			
  			for (int i = 0; i < sentences; i++) {
  				String s = scanner.nextLine();
  				wp.sentences.add(s);
  			}

  			all_verb_pairs.add(wp);
  		}
  	}
 	}

  public static void populateVerbVerbPairsHashMap() throws Exception {
    Scanner scanner = new Scanner(new File("pair_ref_test.txt"));
    while (scanner.hasNextLine()) {
      int id = 0;
      try {
        id = scanner.nextInt();
      } catch (NoSuchElementException e) {
        continue;
      }
      String verb_pair = scanner.next();
      String[] verbs_pair = verb_pair.split("-");
      Word_Pair wp = new Word_Pair(verbs_pair[0],verbs_pair[1]);
      wp = find_WP(wp);
      all_verb_pairs_hashmap.put(id, wp);
    }
  }

 	public static void populateProbabilities() throws Exception {
 		Scanner scanner = new Scanner(new File("..\\Mallet\\ling_causal_res.txt"));
  	while (scanner.hasNextLine()) {
      int id = 0;
      try {
        id = scanner.nextInt();
      } catch (NoSuchElementException e) {
        continue;
      }
      Word_Pair wp = find_WP(id);

      scanner.next();
  		Double causal = scanner.nextDouble();
  		scanner.next();
  		Double noncausal = scanner.nextDouble();

  		if (wp == null) {
      	continue;
      }
      wp.causal.add(causal);
      wp.noncausal.add(noncausal);
  	}
 	}

	//=================================================================================
  //=================================================================================

 	public static void sortVerbVerbByScore() {
 		for (int i = 1; i < all_verb_pairs.size(); i++) {
 			int j = i;
 			while (j > 0 && all_verb_pairs.get(j).score > all_verb_pairs.get(j-1).score) {
 				Word_Pair temp = all_verb_pairs.get(j);
 				all_verb_pairs.set(j, all_verb_pairs.get(j-1));
 				all_verb_pairs.set(j-1, temp);
 				j--;
 			}
 		}
 	}

	//=================================================================================
  //=================================================================================

	public static void main(String[] args) {
		try {
    	populateWords();
      populateVerbVerbPairs();
      populateVerbVerbPairsHashMap();
    	populateProbabilities();
    } catch (Exception e) {
    	e.printStackTrace();
    }

  	Collections.sort(all_verb_pairs);

  	for (Word_Pair wp : all_verb_pairs) {
  		// System.out.println(wp.print());
  		// System.out.println("sentences = " + Integer.toString(wp.sentences.size()));
  		// System.out.println("causal    = " + Integer.toString(wp.causal.size()));
  		// System.out.println("noncausal = " + Integer.toString(wp.noncausal.size()));
  		double temp = ECA(wp);
  		wp.score = temp;
			// if (temp != -1) {
			// 	System.out.println("ECA = " + Double.toString(temp));
			// }
			// System.out.println("\n");
  	}

  	sortVerbVerbByScore();

  	for (Word_Pair wp : all_verb_pairs) {
  		System.out.print(wp.toString());
      for (int i = wp.toString().length(); i < 50; i++) {
        System.out.print(" ");
      }
      System.out.println(wp.score);
  	}
	}
}