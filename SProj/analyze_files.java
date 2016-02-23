
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

import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

//=================================================================================
//=================================================================================

public class analyze_files {
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

  public static double IDF(Word_Pair wp) {
    double one = idf(wp.word_one);
    double two = idf(wp.word_two);
    double three = idf(wp);

    // pw.print(wp.print() + "\n\t");
    // pw.println(Double.toString(one) + " * " + Double.toString(two) + " * " + Double.toString(three));
    return one * two * three;
  }

  public static double idf(String word) {
    double ans = 1.0 + find_WC(word).documentCount;
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
    return ((double)wc.actualCount) / ((double)totalNumWords);
  }

  public static double P(Word_Pair wp) {
    return ((double)wp.actualCount) / ((double)totalSentences);
  }

  public static double PMI(Word_Pair wp) {
    return Math.log(P(wp) / (P(wp.word_one) * P(wp.word_two)));
  }

  //=================================================================================
  //=================================================================================

  public static double CD(Word_Pair wp) {
    return PMI(wp) * max(wp) * IDF(wp);
  }

  //=================================================================================
  //=================================================================================

  public static double max_helper(List<Word_Pair> wp_list) {
    double max = 0.0;

    for (Word_Pair wp : wp_list) {
      // pw.println(wp.print());
      // pw.println(Double.toString(P(wp)) + "\n");
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

  public static double PS_I(Word_Pair wp, List<TaggedWord> tSentence, List<Pair> phraseLocations) {
    /*
    / Assuming one (non-)causal phrase in a sentence
    */
    int pos_i = 1;  // distance of verb from phrase
    int pos_j = 1;  // distance of verb from phrase
    int C_p   = 0;  // number of verbs before the phrase
    int C_q   = 0;  // number of verbs after the phrase

    for (int i = 0; i < phraseLocations.get(0).x; i++) {
      TaggedWord tw = tSentence.get(i);
      if (tw.tag().startsWith("VB")) {
          C_p++;

          if (tw.word().equals(wp.word_one) || tw.word().equals(wp.word_two)) {
            pos_i = 1;
          } else {
            pos_i++;
          }
      }
    }

    for (int i = tSentence.size()-1; i > phraseLocations.get(0).x + phraseLocations.get(0).y; i--) {
      TaggedWord tw = tSentence.get(i);
      if (tw.tag().startsWith("VB")) {
        C_q++;

        if (tw.word().equals(wp.word_one) || tw.word().equals(wp.word_two)) {
          pos_j = 1;
        } else {
          pos_j++;
        }
      }
    }

    double returnValue = -Math.log((pos_i + pos_j) / (2.0 * (C_p + C_q)));
    return returnValue;
  }

  //=================================================================================
  //=================================================================================

  public static Word_Pair f_I(List<TaggedWord> tSentence) {
    List<Pair> phraseLocations = findPhraseLocations(tSentence);
    List<Word_Count> verbs = new ArrayList<>();

    // If sentence doesn't contain a (non-)causal phrase, return null.
    if (phraseLocations.size() == 0) {
      return null;  
    }
    
    List<Word_Pair> verb_verb_pairs = findVerbPairs(tSentence);
    double maxValue = -99999999.0;
    Word_Pair returnValue = null;

    for (Word_Pair wp : verb_verb_pairs) {
      double value  = 0.0;
      double _CD    = CD(wp);
      double _PS_I  = PS_I(wp, tSentence, phraseLocations); 

      if (P(wp) == 0.0) {
        value = _PS_I;
      } else {
        value = _CD * _PS_I;
      }

      if (value > maxValue) {
        maxValue = value;
        returnValue = wp;
      }
    }

    return returnValue;
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

  public static void main(String[] args) throws Exception {
    pw = new PrintWriter(new OutputStreamWriter(System.out, "utf-8"));

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

        // Find the most dependant pair.
        Word_Pair wp = f_I(tSentence);
        if (wp != null) {
          pw.println("WP   = " + wp.word_one + " - " + wp.word_two);
          pw.println("CD   = " + Double.toString(CD(wp)));
          pw.println("PS_I = " + Double.toString(PS_I(wp, tSentence, findPhraseLocations(tSentence))) + "\n");
        } else {
          pw.println("wp   = NULL\nCD   = NULL\nPS_I = NULL\n");
        }
      }
    }

    // // Printing the total number of words
    // pw.println("\ntotalNumWords = " + Integer.toString(totalNumWords));
    // // Printing the total number of sentences
    // pw.println("totalSentences = " + Integer.toString(totalSentences) + "\n");

    // // Printing the Inverse Document Frequency Count
    // pw.print("DOCUMENT     ACTUAL     WORD\n");
    // Collections.sort(doc_count_words);
    // for (int i = 0; i < doc_count_words.size(); i++) {
    //   pw.println(doc_count_words.get(i).print());
    // }
    // pw.print("\n\n");

    // // Printing the Verb Pairs.
    // pw.print("Verb Pairs\n");
    // for (Word_Pair wp : all_verb_pairs) {
    //   pw.println(wp.print());
    // }

    // Testing a max function call.
    // pw.println("MAX = " + Double.toString(max(all_verb_pairs.get(3))));

    pw.close();
  }
}
