
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

import java.io.StringReader;
import java.util.List;
import java.util.Collection;

/**
 * Demonstrates how to first use the tagger, then use the NN dependency
 * parser. Note that the parser will not work on untagged text.
 *
 * @author Jon Gauthier
 */
public class DependencyParserDemo {
  public static void main(String[] args) {
    String modelPath = DependencyParser.DEFAULT_MODEL;
    String taggerPath = "english-left3words-distsim.tagger";
    String parserModel = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";
    
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
