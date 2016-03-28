import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Scanner;
import java.util.Set;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

//=================================================================================
//=================================================================================

public class create_explicit_corpus {
  static List<File>       all_files         = new ArrayList<>();
  static List<Word_Pair>  all_verb_pairs    = new ArrayList<>();
  static List<Word_Count> doc_count_words   = new ArrayList<>();
  static List<String>     phrases_all       = Arrays.asList("because", "for this reason", "for that reason", "consequently", "as a consequence of", "as a result of", "but", "in short", "in other words", "whereas", "on the other hand", "nevertheless", "nonetheless", "in spite of", "in contrast", "however", "even", "though", "despite the fact", "conversely", "although");
  static List<Integer>    length_phrases_all= Arrays.asList(1, 3, 3, 1, 4, 4, 1, 2, 3, 1, 4, 1, 1, 3, 2, 1, 1, 1, 3, 1, 1);
  static String           dirName           = System.getProperty("user.dir") + "\\textfiles\\train";
  static String           uDirName           = System.getProperty("user.dir") + "/textfiles/train";
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

    // Word doesn't exist.
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

  /**
   * Find a Word_Count from the doc_count_words List
   * @param  wc1 [The Word_Count which is being searched for]
   * @return     [The Word_Count from the List, if it exists, or null]
   */
  public static Word_Count find_WC(Word_Count wc1) {
    for (Word_Count wc2 : doc_count_words) {
      if (wc1.equals(wc2)) {
        return wc2;
      }
    }
    return null;
  }

  /**
   * Find a Word_Pair from the all_verb_pairs List
   * @param  wp1 [The Word_Pair which is being searched for]
   * @return     [The Word_Pair from the List, if it exists, or null]
   */
  public static Word_Pair find_WP(Word_Pair wp1) {
    for (Word_Pair wp2 : all_verb_pairs) {
      if (wp1.equals(wp2)) {
        return wp2;
      }
    }
    return null;
  }

  //=================================================================================
  //=================================================================================

  public static String removePunctuation(String content) {
    content = content.replace(".", " ");
    content = content.replace(",", " ");
    content = content.replace("?", " ");
    content = content.replace("!", " ");
    content = content.replace(";", " ");
    content = content.replace("-", " ");
    content = content.replace("_", " ");
    content = content.replace("`", " ");
    content = content.replace("=", " ");
    content = content.replace("@", " ");
    content = content.replace("#", " ");
    content = content.replace("$", " ");
    content = content.replace("%", " ");
    content = content.replace("^", " ");
    content = content.replace("&", " ");
    content = content.replace("*", " ");
    content = content.replace("(", " ");
    content = content.replace(")", " ");
    content = content.replace("[", " ");
    content = content.replace("]", " ");
    content = content.replace("{", " ");
    content = content.replace("}", " ");
    content = content.replace("\'", " ");
    content = content.replace("\"", " ");
    return content;
  }

  public static String docWordCounter(int id) throws Exception {
    String content = new Scanner(all_files.get(id)).useDelimiter("\\Z").next().toLowerCase();
    content = removePunctuation(content);

    // Increment document counter for words. Add new words to doc_count_words.
    List<String> words = Arrays.asList(content.split("\\s+"));
    Set<String> set = new HashSet<String>(words);

    for (String s : set) {
      Word_Count temp = new Word_Count(s);
      int index = doc_count_words.indexOf(temp);
      if (index != -1) {
        doc_count_words.get(index).documentIncrement();
      } else {
        doc_count_words.add(temp);
        temp.actualIncrement(-1);
      }
    }

    // Increment actual counter for words.
    for (String s : words) {
      Word_Count wc = new Word_Count(s);
      Word_Count search = find_WC(wc);

      if (search != null) {
        search.actualIncrement();
      }
    }

    return content;
  }

  //=================================================================================
  //=================================================================================
  
  /**
   * Find locations of unambiguous discourse markers (both causal and non-causal) in the given sentence.
   * @param tSentence [The input sentence to the function]
   * @return          [List of locations of discourse markers along with their lengths]
   */
  public static List<Pair> findPhraseLocations(List<TaggedWord> tSentence) {
    List<Pair> phraseLocations = new ArrayList<>();

    // Go through each tagged word in the sentence.
    for (int i = 0; i < tSentence.size(); i++) {

      for (int k = 0; k < phrases_all.size(); k++) {
        List<String> phrase = Arrays.asList(phrases_all.get(k).split("\\s+"));

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
            Pair p = new Pair(i, length_phrases_all.get(k));
            p.z = k;
            phraseLocations.add(p);
          }
        }    
      }
    }

    return phraseLocations;
  }

  /**
   * Make pairs of verbs which appear before and after unambiguous discourse markers.
   * @param  tSentence [The input sentence]
   * @return           [A list of verb-verb pairs]
   */
  public static List<Word_Pair> findVerbPairs(List<TaggedWord> tSentence,String origSent) {
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

          // Printing
          if (tSentence.get(j).tag().equals("VB")) {
            pw.println("\tTAG_BEFORE: " + tSentence.get(j).tag() + " \tWORD: " + tSentence.get(j).word());  
          } else {
            pw.println("\tTAG_BEFORE: " + tSentence.get(j).tag() + "\tWORD: " + tSentence.get(j).word());  
          }
        }
      }

      // Check for verbs occurring after the unambiguous discourse marker.
      for (int j = phraseLocations.get(i).x+phraseLocations.get(i).y; j < end; j++) {
        if (tSentence.get(j).tag().startsWith("VB")) {
          verbsAfter.add(tSentence.get(j).word());
          
          // Printing
          if (tSentence.get(j).tag().equals("VB")) {
            pw.println("\tTAG_AFTER : " + tSentence.get(j).tag() + " \tWORD: " + tSentence.get(j).word());  
          } else {
            pw.println("\tTAG_AFTER : " + tSentence.get(j).tag() + "\tWORD: " + tSentence.get(j).word());  
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
            Word_Pair search = find_WP(wp);

            if (search != null) {
              search.actualIncrement();
            } else {
              all_verb_pairs.add(wp);
              search = wp;
            }

            search.sentences.add(origSent);
            int z = phraseLocations.get(i).z;
            if (z < 6) {
              search.sentences_tags.add("causal");

              // Pair(1, 0) means that the second word in the Word_Pair is the cause, and the first is the effect
              if (z == 0 || z == 4 || z == 5) { // because, as a consequence of, as a result of
                if (s1.toLowerCase().equals(search.word_one)) {
                  search.sentences_event_roles.add(new Pair(1, 0));
                } else {
                  search.sentences_event_roles.add(new Pair(0, 1));
                }
              } else {
                if (s1.toLowerCase().equals(search.word_one)) {
                  search.sentences_event_roles.add(new Pair(0, 1));
                } else {
                  search.sentences_event_roles.add(new Pair(1, 0));
                }
              }
            } else {
              search.sentences_tags.add("non-causal");
              search.sentences_event_roles.add(new Pair(-1, -1));
            }
            returnValue.add(search);
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
    
    // Get list of all files which have to be parsed in order to construct the (non-)Causal verb-pairs.
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

      // Print each file's name.
      String fileName = all_files.get(id).getPath();
      pw.print("\n***\n" + fileName + "\n***\n");

      // Check for occurrences for the (non-)causal strings in the current document, increment the occurrence counter for use in IDF function.
      // Find out which (non-)causal string to check for in the current document
      docWordCounter(id);

      // Open the file.
      BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "utf-8"));
      
      // Produces a list of sentences from the document.
      // http://nlp.stanford.edu/nlp/javadoc/javanlp/edu/stanford/nlp/process/DocumentPreprocessor.html
      DocumentPreprocessor documentPreprocessor = new DocumentPreprocessor(r);
      documentPreprocessor.setTokenizerFactory(ptbTokenizerFactory);

      // Go through each sentence in the document.
      for (List<HasWord> sentence : documentPreprocessor) {
        totalSentences++;
        String origSent = Sentence.listToString(sentence, false);
        String content = Sentence.listToString(sentence, false).toLowerCase();
        content = removePunctuation(content);
        
        List<String> words = Arrays.asList(content.split("\\s+"));
        sentence.clear();
        for (String word : words) {
          sentence.add(new Word(word));
        }

        // Print the sentence
        content = Sentence.listToString(sentence, false).toLowerCase();
        pw.println(content);

        // Tag each sentence, producing a list of tagged words.
        List<TaggedWord> tSentence = tagger.tagSentence(sentence);

        // Print the tagged sentence.
        pw.println(Sentence.listToString(tSentence, false));

        // Make a backup of all_verb_pairs so we can check for differences and increment document count.
        List<Word_Pair> oldList = new ArrayList<>();
        List<Integer> oldActualCounts = new ArrayList<>();
        for (Word_Pair wp : all_verb_pairs) {
          oldList.add(wp);
          int i = wp.actualCount;
          oldActualCounts.add(i);
        }

        // Find pairs of verbs before and after the unambiguous discourse markers.
        findVerbPairs(tSentence, origSent);

        // Incrementing the document count for the pairs.
        for (int i = 0; i < oldList.size(); i++) {
          if (oldList.get(i).actualCount != oldActualCounts.get(i)) {
            oldList.get(i).documentIncrement();
          }
        }

        pw.println("\n");
      }
    }

    // Total number of words.
    totalNumWords = 0;
    ////////////////////////////////////////////////////////////////////
    
    Collections.sort(doc_count_words);
    Collections.sort(all_verb_pairs);
    if (doc_count_words.get(0).word.equals("")) {doc_count_words.remove(0);}

    pw.close();

    // Output the Words to a file called count_words.txt;
    pw = new PrintWriter(new File("count_words.txt"));

    for (Word_Count wc : doc_count_words) {
      totalNumWords += wc.actualCount;
    }
    pw.println(totalNumWords);
    pw.println(totalSentences);

    for (int i = 0; i < doc_count_words.size(); i++) {
      pw.print(doc_count_words.get(i).prettyPrint());
      if (i != doc_count_words.size()-1) {
        pw.print("\n");
      }
    }
    pw.close();

    // Output the Verb-Verb pairs to a file called count_verb_verb.txt;
    pw = new PrintWriter(new File("count_verb_verb.txt"));
    for (int i = 0; i < all_verb_pairs.size(); i++) {
      pw.print(all_verb_pairs.get(i).print());
      if (i != all_verb_pairs.size()-1) {
        pw.print("\n");
      }
    }
    pw.close();
    
    // pw = new PrintWriter(new File("input_features_tagged_all.txt"));
    // for (int i = 0; i < all_verb_pairs.size(); i++) {
    //   if (all_verb_pairs.get(i).word_one.equals("")) {
    //     continue;
    //   } else {
    //     pw.println(all_verb_pairs.get(i).toString());
    //   }
    // }
    // pw.close();
    // pw = new PrintWriter(new File("input_features_tagged_all2.txt"));
    // for (int i = 0; i < all_verb_pairs.size(); i++) {
    //   if (all_verb_pairs.get(i).word_one.equals("")) {
    //     continue;
    //   } else {
    //     pw.println(all_verb_pairs.get(i).printWithSentences());
    //   }
    // }
    // pw.close();

////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////

    all_verb_pairs.clear();
    List<Word_Pair> temp = new ArrayList<>();

    // Go through each file in the list.
    for (int id = 0; id < all_files.size(); id++) {
      String fileName = all_files.get(id).getPath();
      BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "utf-8"));
      
      DocumentPreprocessor documentPreprocessor = new DocumentPreprocessor(r);
      documentPreprocessor.setTokenizerFactory(ptbTokenizerFactory);

      for (List<HasWord> sentence : documentPreprocessor) {
        String content = Sentence.listToString(sentence, false).toLowerCase();
        content = removePunctuation(content);
        
        List<String> words = Arrays.asList(content.split("\\s+"));
        sentence.clear();
        for (String word : words) {
          sentence.add(new Word(word));
        }

        List<TaggedWord> tSentence = tagger.tagSentence(sentence);
        
        // Find the most dependant pair.
        Word_Pair wp = f_I(tSentence);
        if (wp != null) {
          temp.add(wp);
        }
      }
    }

    Collections.sort(temp);
    
    // pw = new PrintWriter(new File("input_features_tagged_reduced.txt"));
    // for (int i = 0; i < temp.size(); i++) {
    //   pw.println(temp.get(i).toString());
    // }
    // pw.close();

    // Output the Verb-Verb pairs to a file called input_features_tagged.txt;
    pw = new PrintWriter(new File("input_features_tagged.txt"));
    for (int i = 0; i < temp.size(); i++) {
      Word_Pair wp = temp.get(i);
      if (wp.word_one.equals("")) {
        continue;
      } else {
        pw.print(wp.printWithSentences());
        pw.flush();
      }
    }
    pw.close();
  }
}
