import java.net.*;
import java.io.*;
import edu.mit.jwi.*;
import edu.mit.jwi.item.*;
import edu.mit.jwi.morph.*;
import java.util.List;


public class test {
	public static void main(String[] args) {
		 // construct the URL to the Wordnet dictionary directory
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
    //look at stems
    WordnetStemmer stemmer = new WordnetStemmer(dict);
    List<String> strings = stemmer.findStems("collapsing",POS.VERB);
    for (int i = 0 ; i < strings.size();i++) {
    	System.out.println(strings.get(i));
    }
    // look up first sense of the word "dog"
    IIndexWord idxWord = dict.getIndexWord("rage", POS.VERB);
    for (int i = 0; i < idxWord.getWordIDs().size();i++){
    	IWordID wordID = idxWord.getWordIDs().get(i);
    	IWord word = dict.getWord(wordID);
    	System.out.println("Sense Key = " + word.getSenseKey().toString());
    	System.out.println("Id = " + wordID);
    	System.out.println("Lemma = " + word.getLemma());
    	System.out.println("Gloss = " + word.getSynset().getGloss());
	}
	}
}