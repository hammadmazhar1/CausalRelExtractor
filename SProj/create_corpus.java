
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

public class create_corpus {
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

  public static void docWordCounter(int id) throws Exception {
    String content = new Scanner(all_files.get(id)).useDelimiter("\\Z").next().toLowerCase();
    content = content.replace(".", "");
    content = content.replace(",", "");

    // Increment document counter for words. Add new words to doc_count_words.
    Set<String> set = new HashSet<String>(Arrays.asList(content.split(" ")));

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
    for (Word_Count wc : doc_count_words) {
      int i = 0;
      Pattern p = Pattern.compile(wc.word);
      Matcher m = p.matcher(content);
      while (m.find()) {
          i++;
      }
      wc.actualIncrement(i);
    }
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

  public static void findVerbPairs(List<TaggedWord> tSentence) {
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
            all_verb_pairs.add(new Word_Pair(s1, s2));
          }
        }
      }
    }

    // Removing duplicate Verb Pairs and incrementing their count.
    Collections.sort(all_verb_pairs);
    int size = all_verb_pairs.size() - 1;
    for (int j = 0; j < size; j++) {
      if (all_verb_pairs.get(j).equals(all_verb_pairs.get(j+1))) {
        all_verb_pairs.get(j).actualIncrement();
        all_verb_pairs.remove(j+1);
        j--;
        size--;
      }
    }
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

        // Print the sentence
        String sentenceString = Sentence.listToString(sentence, false).toLowerCase();
        pw.println(sentenceString);

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
        findVerbPairs(tSentence);

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

    // Printing the Inverse Document Frequency Count
    pw.print("DOCUMENT     ACTUAL     WORD\n");
    Collections.sort(doc_count_words);
    for (int i = 0; i < doc_count_words.size(); i++) {
      pw.println(doc_count_words.get(i).print());
    }
    pw.print("\n\n");

    // Printing the Verb Pairs.
    pw.print("Verb Pairs\n");
    for (Word_Pair wp : all_verb_pairs) {
      pw.println(wp.print());
    }

    // Printing a sample IDF.
    // pw.print("\nIDF\n\t");
    // IDF(all_verb_pairs.get(13));

    // Testing a max function call.
    // pw.println("MAX = " + Double.toString(max(all_verb_pairs.get(3))));

    pw.close();

    // Output the Words to a file called dictionary.txt;
    pw = new PrintWriter(new File("dictionary.txt"));

    for (Word_Count wc : doc_count_words) {
      totalNumWords += wc.actualCount;
    }
    pw.println(totalNumWords);
    pw.println(totalSentences);

    for (int i = 0; i < doc_count_words.size(); i++) {
      pw.print(doc_count_words.get(i).print());
      if (i != doc_count_words.size()-1) {
        pw.print("\n");
      }
    }
    pw.close();

    // Output the Verb-Verb pairs to a file called verb-verb.txt;
    pw = new PrintWriter(new File("verb-verb.txt"));
    for (int i = 0; i < all_verb_pairs.size(); i++) {
      pw.print(all_verb_pairs.get(i).print());
      if (i != all_verb_pairs.size()-1) {
        pw.print("\n");
      }
    }
    pw.close();
  }
}
