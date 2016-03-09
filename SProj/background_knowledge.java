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

public class background_knowledge {
	static List<File>       all_files         = new ArrayList<>();
  static List<Word_Count> doc_count_words   = new ArrayList<>();
  static List<Word_Pair>  all_verb_pairs    = new ArrayList<>();
	static int              totalNumWords     = 0;
  static int              totalSentences    = 0;
  static String           dirName           = System.getProperty("user.dir") + "\\textfiles\\test";
  static String           uDirName          = System.getProperty("user.dir") + "/textfiles/test";
  
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

  public static double IDF(Word_Pair wp) {
    double one = idf(wp.word_one);
    double two = idf(wp.word_two);
    double three = idf(wp);

    if (one == -1 || two == -1) {return -1;}
    return one * two * three;
  }

  public static double idf(String word) {
  	Word_Count wc = find_WC(word);
  	if (wc == null) {return -1;}
    double ans = 1.0 + wc.documentCount;
    return all_files.size() / ans;
  }

  public static double idf(Word_Pair wp) {
    double ans = 1.0 + wp.documentCount;
    return all_files.size() / ans;
  }

  //=================================================================================
  //=================================================================================

  public static double P(String word) {
    Word_Count wc = find_WC(word);
    if (wc == null) {return -1;}
    return ((double)wc.actualCount) / ((double)totalNumWords);
  }

  public static double P(Word_Pair wp) {
    return ((double)wp.actualCount) / ((double)totalSentences);
  }

  public static double PMI(Word_Pair wp) {
  	double word_one_p = P(wp.word_one);
  	double word_two_p = P(wp.word_two);
  	if (word_one_p == -1 || word_two_p == -1) {return -1;}
    return Math.log(P(wp) / (word_one_p * word_two_p));
  }

  //=================================================================================
  //=================================================================================

  public static double CD(Word_Pair wp) {
  	double _pmi = PMI(wp);
  	double _idf = IDF(wp);
  	if (_pmi == -1 || _idf == -1) {return -1;}
    return _pmi * max(wp) * _idf;
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

    double val1 = p_vi_vj / (max_vi_vk - p_vi_vj - epsilon);
    double val2 = p_vi_vj / (max_vj_vk - p_vi_vj - epsilon);

    return Math.max(val1, val2);
  }

  //=================================================================================
  //=================================================================================

 	public static double ECA(Word_Pair wp) {
 		double factor = 1.0 / all_verb_pairs.size();
 		double cd = CD(wp);
 		if (cd == -1) {return -1;}

 		double sum = 0.0;
 		for (int i = 0; i < wp.sentences.size(); i++) {
 			sum += C_i(i);
 		}

 		return factor * cd * sum;
 	}

  //=================================================================================
  //=================================================================================

 	public static double C_i(int index) {
 		return 0;
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

	//=================================================================================
  //=================================================================================

	public static void main(String[] args) {
		try {
			Scanner scanner = new Scanner(new File("count_words.txt"));
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

  		scanner = new Scanner(new File("input_features.txt"));
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
    } catch (Exception e) {
    	e.printStackTrace();
    }

    // Open the file which has to be analyzed.
    File[] files = null;
    if (System.getProperty("os.name").toLowerCase().contains("windows")) 
      files = new File(dirName).listFiles();
    else
      files = new File(uDirName).listFiles();
    iterateFiles(files);

  	Collections.sort(all_verb_pairs);

  	for (Word_Pair wp : all_verb_pairs) {
  		System.out.println(wp.print());
  		double temp = ECA(wp);
			if (temp != -1) {
				System.out.println("ECA = " + Double.toString(temp));
			}
			System.out.println("\n");
  	}
	}
}