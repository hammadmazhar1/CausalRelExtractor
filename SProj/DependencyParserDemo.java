
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.nndep.DependencyParser;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
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
import edu.mit.jwi.*;
import edu.mit.jwi.item.*;
import edu.mit.jwi.morph.*;

import java.io.StringReader;
import java.util.List;
import java.util.Collection;
import java.net.*;
import java.io.*;

/**
 * Demonstrates how to first use the tagger, then use the NN dependency
 * parser. Note that the parser will not work on untagged text.
 *
 * @author Jon Gauthier
 */
public class DependencyParserDemo {
  public static void main(String[] args) {
    String modelPath = "models\\english_UD.gz";
    String taggerPath = "models\\english-left3words-distsim.tagger";
    String parserModel = "models\\englishPCFG.ser.gz";
    
    // for (int argIndex = 0; argIndex < args.length; ) {
    //   switch (args[argIndex]) {
    //     case "-tagger":
    //       taggerPath = args[argIndex + 1];
    //       argIndex += 2;
    //       break;
    //     case "-model":
    //       modelPath = args[argIndex + 1];
    //       argIndex += 2;
    //       break;
    //     default:
    //       throw new RuntimeException("Unknown argument " + args[argIndex]);
    //   }
    // }

    // LexicalizedParser lp = LexicalizedParser.loadModel(parserModel);
    String text = "The monster storm Katrina raged ashore along the Gulf Coast Monday morning. There were early reports of buildings collapsing along the coast.";
    // TreebankLanguagePack tlp = lp.treebankLanguagePack(); // a PennTreebankLanguagePack for English
    // GrammaticalStructureFactory gsf = null;
    // if (tlp.supportsGrammaticalStructures()) {
    //   gsf = tlp.grammaticalStructureFactory();
    // }
    
    String wnhome = System.getenv("WNHOME");
    String path = wnhome + File.separator + "dict";
    URL url = null;
    try{ url = new URL("file", null, path); } 
    catch(MalformedURLException e){ e.printStackTrace(); }
    if(url == null) return;
    
    // construct the dictionary object and open it
    IDictionary dict = new Dictionary(url);
    try {dict.open();}
    catch(IOException e){e.printStackTrace();}
    MaxentTagger tagger = new MaxentTagger(taggerPath);
    

    DependencyParser parser = DependencyParser.loadFromModelFile(modelPath);

    DocumentPreprocessor tokenizer = new DocumentPreprocessor(new StringReader(text));
    System.out.println("\n");
    for (List<HasWord> sentence : tokenizer) {
      List<TaggedWord> tagged = tagger.tagSentence(sentence);
      GrammaticalStructure gs = parser.predict(tagged);

      // Print typed dependencies
      // System.out.println(gs);
      List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
      
      for (TypedDependency td : tdl) {
        System.out.println(td);
        if (td.gov().value().equals("raged") || td.gov().value().equals("collapsing")) {
  			String relation = td.reln().getShortName();
  			if (relation.contains("obj")){
  				String posTag = null;
  				for (TaggedWord t : tagged) {
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
    			System.out.println(entry);
  			} else if(relation.contains("subj")) {
  				String posTag = null;
  				for (TaggedWord t : tagged) {
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
    			System.out.println(entry);
  			}
  		}
      }
      
      // Tree parse = lp.apply(sentence);
      // parse.pennPrint();
      // System.out.println();

      // if (gsf != null) {
      //   GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
      //   Collection tdl = gs.typedDependenciesCCprocessed();
      //   System.out.println(tdl);
      //   System.out.println();
      // }
    }

  }
}
