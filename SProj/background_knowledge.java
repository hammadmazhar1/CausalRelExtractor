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
  static HashMap<Integer, Word_Pair>  all_verb_pairs_hashmap  = new HashMap<Integer, Word_Pair>();
  static HashMap<Integer, String>     labels                  = new HashMap<Integer, String>();
  static HashMap<Integer, Double>     ranking_scores          = new HashMap<Integer, Double>();
  
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
 		double one = wp.cause_effect_one.get(i).x + wp.cause_effect_two.get(i).y;
 		double two = wp.cause_effect_one.get(i).y + wp.cause_effect_two.get(i).x;

    // not using Log means the negative sign multiplication isn't needed?
 		// return -1.0 * Math.max(one, two);
    return Math.max(one, two); 
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
    System.out.println("Populating verb-verb pair hashmap ");
    while (scanner.hasNextLine()) {
      int id = 0;
      try {
        id = scanner.nextInt();
      } catch (NoSuchElementException e) {
        continue;
      }
      String verb_pair = scanner.next();
      String sent = scanner.nextLine();
      String[] verbs_pair = verb_pair.split("-");
      Word_Pair wp = new Word_Pair(verbs_pair[0],verbs_pair[1]);
      Word_Pair wp2 = find_WP(wp);
      wp.sentences.add(sent);
      wp.hashmap_key = id;
      all_verb_pairs_hashmap.put(id, wp);
      //System.out.println(id);
    }
  }

 	public static void populateProbabilities() throws Exception {
 		Scanner scanner = new Scanner(new File("ling_causal_res.txt"));
  	while (scanner.hasNextLine()) {
      int id = 0;
      try {
        id = scanner.nextInt();
      } catch (NoSuchElementException e) {
        continue;
      }
      Word_Pair wp = find_WP(id);
      Word_Pair wp2 = find_WP(wp);
      scanner.next();
  		Double causal = scanner.nextDouble();
  		scanner.next();
  		Double noncausal = scanner.nextDouble();

  		if (wp == null) {
      	continue;
      }
      wp.causal.add(causal);
      wp.noncausal.add(noncausal);
      wp2.causal.add(causal);
      wp2.noncausal.add(noncausal);
  	}
 	}

  public static void populateCauseEffectProbabilities() throws Exception {
    Scanner scanner = new Scanner(new File("cause_effect_res.txt"));
    int id = 0;
    System.out.println("Populating cause effect probabilities");
    while (scanner.hasNextLine()) {
      id++;
      Word_Pair wp = find_WP(id);
      if (wp !=null) { 
        Word_Pair wp2 = find_WP(wp);
        try {
          scanner.next();
        } catch (NoSuchElementException e) {
          continue;
        }
        scanner.next();
        Double effect = scanner.nextDouble();
        scanner.next();
        Double cause = scanner.nextDouble();
        wp.cause_effect_one.add(new dPair(cause, effect));
        wp2.cause_effect_one.add(new dPair(cause, effect));

        scanner.next();
        scanner.next();
        effect = scanner.nextDouble();
        scanner.next();
        cause = scanner.nextDouble();
        wp.cause_effect_two.add(new dPair(cause, effect));
        wp2.cause_effect_two.add(new dPair(cause, effect));
        //System.out.println(id);
      } else {
        return;
      }
    }
  }

	//=================================================================================
  //=================================================================================
  public static void populateEventProbabilities() throws Exception{
     Scanner scanner = new Scanner(new File("ling_event_res.txt"));
    int id = 0;
    System.out.println("Populating event probabilities");
    while (scanner.hasNextLine()) {
      id++;
      Word_Pair wp = find_WP(id);
      if (wp != null) {
        Word_Pair wp2 = find_WP(wp);

        try {
          scanner.next();
        } catch (NoSuchElementException e) {
          continue;
        }
        scanner.next();
        Double event = scanner.nextDouble();
        scanner.next();
        Double nonevent = scanner.nextDouble();
        wp.evt_nevt_one.add(new dPair(event, nonevent));
        wp2.evt_nevt_one.add(new dPair(event, nonevent));
        scanner.next();
        scanner.next();
        event = scanner.nextDouble();
        scanner.next();
        nonevent = scanner.nextDouble();
        wp.evt_nevt_two.add(new dPair(event, nonevent));
        wp2.evt_nevt_two.add(new dPair(event, nonevent));
        //System.out.println(id);
      } else 
        return;
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

  public static void rank() {
    int rank = 0;
    List<Integer> ranks = new ArrayList<>();
    ranks.add(0);
    for (int i = 1; i < all_verb_pairs.size(); i++) {
      if (all_verb_pairs.get(i).score < all_verb_pairs.get(i-1).score) {
        rank++;
      }
      ranks.add(rank);
    }

    int size = ranks.size();

    for (int i = 0; i < ranks.size(); i++) {
      all_verb_pairs.get(i).score = (((double)size) - (double)(ranks.get(i))) / ((double)size);
      ranking_scores.put(all_verb_pairs.get(i).hashmap_key, all_verb_pairs.get(i).score);
    }
  }

  public static double sum(List<Double> list) {
    double sum = 0.0;
    for (Double d : list) {
      sum += d;
    }
    return sum;
  }

  /**
   * Modification of basic linear program
   */
  public static void Z_1() {
    labels.clear();
     for (Word_Pair wp : all_verb_pairs_hashmap.values()) {
      if (wp.causal.get(0) > wp.noncausal.get(0)) {
        labels.put(wp.hashmap_key, "causal");
      } else {
        labels.put(wp.hashmap_key, "noncausal");
      }
    }
  }

  /**
   * Modification of linear program with knowledge base 1. To be used after rank().
   */
  public static void Z_KB_1() {
    labels.clear();
    for (Word_Pair wp : all_verb_pairs_hashmap.values()) {
      Word_Pair wp2 = find_WP(wp);
      if (wp.causal.get(0)*wp2.score > wp.noncausal.get(0)*(1-wp2.score)) {
        labels.put(wp.hashmap_key, "causal");
      } else {
        labels.put(wp.hashmap_key, "noncausal");
      }
    }
  }
  /**
   * Modification of linear program with Z3.
   */
  public static void Z_3() {
    labels.clear();
    for (Word_Pair wp : all_verb_pairs_hashmap.values()) {
      double causal_1 = wp.causal.get(0)*wp.evt_nevt_one.get(0).x*wp.evt_nevt_two.get(0).x;
      double causal_2 = wp.causal.get(0)*wp.evt_nevt_one.get(0).x*wp.evt_nevt_two.get(0).y;
      double causal_3 = wp.causal.get(0)*wp.evt_nevt_one.get(0).y*wp.evt_nevt_two.get(0).x;
      double ncausal  = wp.noncausal.get(0)*wp.evt_nevt_one.get(0).y*wp.evt_nevt_two.get(0).y;
      if (causal_1 > ncausal || causal_2 > ncausal || causal_3 > ncausal) {
        labels.put(wp.hashmap_key,"causal");
      } else {
        labels.put(wp.hashmap_key,"noncausal");
      }
    }
  }
  public static void Z_KB_1andZ_3() {
    labels.clear();
    for (Word_Pair wp : all_verb_pairs_hashmap.values()) {
      Word_Pair wp2 = find_WP(wp);
      double causal_1 = wp.causal.get(0)*wp.evt_nevt_one.get(0).x*wp.evt_nevt_two.get(0).x +wp.causal.get(0)*wp2.score;
      double causal_2 = wp.causal.get(0)*wp.evt_nevt_one.get(0).x*wp.evt_nevt_two.get(0).y +wp.causal.get(0)*wp2.score;
      double causal_3 = wp.causal.get(0)*wp.evt_nevt_one.get(0).y*wp.evt_nevt_two.get(0).x +wp.causal.get(0)*wp2.score;
      double ncausal  = wp.noncausal.get(0)*wp.evt_nevt_one.get(0).y*wp.evt_nevt_two.get(0).y +wp.noncausal.get(0)*(1-wp2.score);
      if (causal_1 > ncausal || causal_2 > ncausal || causal_3 > ncausal) {
        labels.put(wp.hashmap_key,"causal");
      } else {
        labels.put(wp.hashmap_key,"noncausal");
      }
    }
  }
  //=================================================================================
  //=================================================================================

  public static String prettyPrint(int arg, int length) {
    String s = Integer.toString(arg);
    int temp = arg;
    int num = 0;
    while (temp > 0) {
      temp /= 10;
      num++;
    }
    for (int i = length; i > num; i--) {
      s += " ";
    }
    return s;
  }

	//=================================================================================
  //=================================================================================

	public static void main(String[] args) throws Exception {
		try {
    	populateWords();
      populateVerbVerbPairs();
      populateVerbVerbPairsHashMap();
    	populateProbabilities();
      populateCauseEffectProbabilities();
      populateEventProbabilities();
    } catch (Exception e) {
    	e.printStackTrace();
    }

  	Collections.sort(all_verb_pairs);

  	for (Word_Pair wp : all_verb_pairs) {
  		double temp = ECA(wp);
  		wp.score = temp;
		}

  	sortVerbVerbByScore();

    System.out.println("ECA\n");
  	/*for (Word_Pair wp : all_verb_pairs) {
  		System.out.print(wp.toString());
      for (int i = wp.toString().length(); i < 40; i++) {
        System.out.print(" ");
      }
      System.out.println(wp.score);
  	}*/

    // ICA
    Collections.sort(all_verb_pairs);

    for (Word_Pair wp : all_verb_pairs) {
      wp.score = 0.0;
      double temp = ICA(wp);
      wp.score = temp;
    }

    sortVerbVerbByScore();

    System.out.println("\nICA\n");
    /*for (Word_Pair wp : all_verb_pairs) {
      //System.out.print(wp.toString());
      for (int i = wp.toString().length(); i < 40; i++) {
        System.out.print(" ");
      }
      System.out.println(wp.score);
    }*/

    // BCA
    Collections.sort(all_verb_pairs);

    for (Word_Pair wp : all_verb_pairs) {
      wp.score = 0.0;
      double temp = BCA(wp);
      wp.score = temp;
    }

    sortVerbVerbByScore();

    System.out.println("\nBCA\n");
    /*for (Word_Pair wp : all_verb_pairs) {
      //System.out.print(wp.toString());
      for (int i = wp.toString().length(); i < 40; i++) {
        System.out.print(" ");
      }
      System.out.println(wp.score);
    }*/

    // Output the Knowledge Base to a file called knowledge_base_causal.txt;
    PrintWriter pw = new PrintWriter(new File("knowledge_base_causal.txt"));
    for (int i = 0; i < all_verb_pairs.size(); i++) {
      Word_Pair wp = all_verb_pairs.get(i);
      pw.print(prettyPrint(wp.hashmap_key, 7) + Double.toString(wp.score));
      if (i != all_verb_pairs.size()-1) {
        pw.print("\n");
      }
    }
    pw.close();

    // Output the Categories of verb-verb pairs Knowledge Base to a file called knowledge_base_2.txt;
    pw = new PrintWriter(new File("knowledge_base_2.txt"));
    int one_third = all_verb_pairs.size() / 3;
    for (int i = 0; i < all_verb_pairs.size(); i++) {
      Word_Pair wp = all_verb_pairs.get(i);
      if (i <= one_third) {
        pw.print(prettyPrint(wp.hashmap_key, 7) + Integer.toString(1));
      } else if (i <= 2*one_third) {
        pw.print(prettyPrint(wp.hashmap_key, 7) + Integer.toString(2));
      } else {
        pw.print(prettyPrint(wp.hashmap_key, 7) + Integer.toString(3));
      }
      
      if (i != all_verb_pairs.size()-1) {
        pw.print("\n");
      }
    }
    pw.close();

    // Calculate rank scores
    rank();

    // Output the Ranked Knowledge Base to a file called knowledge_base_1.txt;
    pw = new PrintWriter(new File("knowledge_base_1.txt"));
    for (int i = 0; i < all_verb_pairs.size(); i++) {
      Word_Pair wp = all_verb_pairs.get(i);
      pw.print(prettyPrint(wp.hashmap_key, 7) + Double.toString(wp.score));
      if (i != all_verb_pairs.size()-1) {
        pw.print("\n");
      }
    }
    pw.close();
    Scanner scanner = new Scanner(System.in);
    System.out.println("Please select the objective function to use for classification:");
    System.out.println("1. Z1 (Simple Causal Classifier).");
    System.out.println("2. ZKB1 (Causal Classifier with Background Knowledge ");
    System.out.println("3. Z3 (Linguisitic Event based classification");
    System.out.println("4. ZKB1 + Z3 (Background knowledge combined with Linguistic Event classifcation");
    int input = scanner.nextInt();
    switch(input){
      case 1:
        Z_1();
        break;
      case 2:
        Z_KB_1();
        break;
      case 3:
        Z_3();
        break;
      case 4:
        Z_KB_1andZ_3();
        break; 
    }
    pw = new PrintWriter(new File("Final_Classifier_Output.txt"));
    Set<Integer> labelKeys = labels.keySet();
    for (int i : labelKeys) {
      pw.print(i + " " + labels.get(i) + "\n");
    }
    System.out.println("Output written to Final_Classifier_Output.txt");
    pw.close();
	}
}