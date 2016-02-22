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

public class create_verb_verb_pairs_with_sentences {
  static List<File>       all_files         = new ArrayList<>();
  static List<Word_Pair>  all_verb_pairs    = new ArrayList<>();
  static String           dirName           = System.getProperty("user.dir") + "\\textfiles\\test";
  static String           modelFile         = "models\\english-left3words-distsim.tagger";
  static PrintWriter      pw                = null;

  //=================================================================================
  //=================================================================================
  
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
  
  public static void findVerbPairs(List<TaggedWord> tSentence) {
    List<String> verbs = new ArrayList<>();
    
    // Check for verb one.
    for (int i = 0; i < tSentence.size(); i++) {
      if (tSentence.get(i).tag().startsWith("VB")) {
        verbs.add(tSentence.get(i).word());
      }
    }

    // If verbs exist both before and after the discourse marker, form all possible pairs of them.
    for (int i = 0; i < verbs.size(); i++) {
      for (int j = i+1; j < verbs.size(); j++) {
        String s1 = verbs.get(i);
        String s2 = verbs.get(j);

        // if the verbs are different
        if (!s1.toLowerCase().equals(s2.toLowerCase())) {
          Word_Pair wp = new Word_Pair(s1, s2);
          Word_Pair search = find_WP(wp);

          if (search != null) {
            search.sentences.add(Sentence.listToString(tSentence, false).toLowerCase());
          } else {
            all_verb_pairs.add(wp);
            wp.sentences.add(Sentence.listToString(tSentence, false).toLowerCase());
          }
        }
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
    File[] files = new File(dirName).listFiles();
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

      // Open the file.
      BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "utf-8"));
      
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

        // Find pairs of verbs before and after the unambiguous discourse markers.
        findVerbPairs(tSentence);

        pw.println("\n");
      }
    }

    ////////////////////////////////////////////////////////////////////

    // Printing the Verb Pairs.
    pw.print("Verb Pairs\n");
    for (Word_Pair wp : all_verb_pairs) {
      pw.println(wp.print());
    }
    pw.close();

    // Output the Verb-Verb pairs to a file called verb-verb.txt;
    pw = new PrintWriter(new File("input_features.txt"));

    for (int i = 0; i < all_verb_pairs.size(); i++) {
      pw.print(all_verb_pairs.get(i).printWithSentences());
      if (i != all_verb_pairs.size()-1) {
        pw.print("\n");
      }
    }
    pw.close();
  }
}
