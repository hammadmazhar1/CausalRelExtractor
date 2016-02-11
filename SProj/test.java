
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.trees.GrammaticalStructure;

import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

import java.io.StringReader;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;


public class test {
  static MaxentTagger tagger;
  public static void GetWords(Tree parse) {
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
      for (TaggedWord tw : tSentence) {
        System.out.println(tw.word() + " / " + tw.tag());
      }
    }
  }

  public static List<Tree> GetVerbPhrases(Tree parse)
  {
      List<Tree> phraseList = new ArrayList<Tree>();
      for (Tree subtree: parse)
      {
        if(subtree.label().value().equals("VP"))
        {
          Tree temp = subtree.getChild(0).getChild(0);
          if (temp.value().equals("collapsing") || temp.value().equals("raged")) {
            GetWords(subtree);
            phraseList.add(subtree);
            subtree.pennPrint();
            System.out.println("\n\n");
          }
        }
      }

      return phraseList;
  }

  public static void main(String[] args) {
    String taggerPath = "models\\english-left3words-distsim.tagger";
    String parserModel = "models\\englishPCFG.ser.gz";

    tagger = new MaxentTagger(taggerPath);
    LexicalizedParser lp = LexicalizedParser.loadModel(parserModel);
    System.out.println("\n");
    String text = "The monster storm Katrina raged ashore along the Gulf Coast Monday morning. There were early reports of buildings collapsing along the coast.";
    // String text = "raged ashore along the Gulf Coast Monday morning. collapsing along the coast.";

    TreebankLanguagePack tlp = lp.treebankLanguagePack(); // a PennTreebankLanguagePack for English
    GrammaticalStructureFactory gsf = null;
    if (tlp.supportsGrammaticalStructures()) {
      gsf = tlp.grammaticalStructureFactory();
    }
    
    DocumentPreprocessor tokenizer = new DocumentPreprocessor(new StringReader(text));
    for (List<HasWord> sentence : tokenizer) {
      System.out.println(Sentence.listToString(sentence, false));
      Tree parse = lp.apply(sentence);
      GetVerbPhrases(parse);
    }
  }
}
