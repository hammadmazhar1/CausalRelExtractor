import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;

import edu.mit.jwi.*;
import edu.mit.jwi.item.*;
import edu.mit.jwi.morph.*;

public class TimeMLParser{
	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("Usage: java parse_tml <directory> <outputFilename>");
			return;
		}
		
		String directory = args[0];
		PrintWriter pw = null;
		PrintWriter refPw = null;
		try {
			pw = new PrintWriter(new File(args[1]));
			refPw = new PrintWriter(new File("sup_evt_ref.txt"));
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		File[] files = new File(directory).listFiles(new FilenameFilter() {
			public boolean accept(File dir, String filename) {
				return filename.endsWith(".tml");
			}
		});
		String wnhome = System.getProperty("user.dir");
    String path = wnhome + File.separator + "dict";
    URL url = null;
    try{ url = new URL("file", null, path); } 
    catch(MalformedURLException e){ e.printStackTrace(); }
    if(url == null) return ;
    // construct the dictionary object and open it
    IDictionary dict = new Dictionary(url);
    try {dict.open();}
    catch(IOException e){e.printStackTrace();}
    WordnetStemmer stemmer = new WordnetStemmer(dict);

		System.out.println(files.length);
		int events = 0;
		int nonEvents = 0;
		List<String> hypernyms = Arrays.asList("think","cogitate","cerebrate","move","displace","act","move");
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			int instance = 0;

			for (File curFile : files) {
				Document doc = builder.parse(curFile);
				doc.getDocumentElement().normalize();
				HashMap<String,TimeMLEvent> eventMap = new HashMap<String,TimeMLEvent>();
				XPath xPath = XPathFactory.newInstance().newXPath();
				String instanceExp = "/TimeML/MAKEINSTANCE";
				NodeList instanceList = (NodeList) xPath.compile(instanceExp).evaluate(doc,XPathConstants.NODESET);
				for (int i = 0; i < instanceList.getLength(); i++) {
					//System.out.println(i);
					Node iNode = instanceList.item(i);
					if (iNode.getNodeType() == Node.ELEMENT_NODE) {
						Element iElement = (Element) iNode;
						TimeMLEvent event = new TimeMLEvent();
						event.eventID = iElement.getAttribute("eventID");
						event.eventInstID = iElement.getAttribute("eiid");
						event.pos = iElement.getAttribute("pos");
						eventMap.put(event.eventID,event);
					}
					
				}
				String expression = "/TimeML/EVENT";
				NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
				for (int i = 0; i < nodeList.getLength(); i++) {
					Node nNode = nodeList.item(i);
					System.out.println("\nCurrent Element :" + nNode.getNodeName());
					if (nNode.getNodeType() == Node.ELEMENT_NODE) {
						Element eElement = (Element)nNode;
						String cls = eElement.getAttribute("class");
						String stem = eElement.getAttribute("stem");
						String word = eElement.getTextContent();
						String eventID = eElement.getAttribute("eid");
						TimeMLEvent evt = eventMap.get(eventID);
						POS pos = POS.VERB;

						if (evt != null) {
							switch(evt.pos) {
							case "VERB":
								pos = POS.VERB;
								break;
							case "NOUN":
								pos = POS.NOUN;
								break;
							case "ADJECTIVE":
								pos = POS.ADJECTIVE;
								break;
							case "ADVERB":
								pos = POS.ADVERB;
								break;
							}
							if (stem.equals("")) {
								List<String> stemList = null;
								stemList = stemmer.findStems(word, pos);
								if (stemList.isEmpty()) {
									stem = word;
								} else {
									stem = stemList.get(0);
								}
							}	
							String affix = word.replace(stem,"");
							if (affix.equals("")) {
								affix = "null";
							}
							instance++;
							
							refPw.println(instance + " " + word);
							//print lexical features
							if (cls.contains("STATE")) {
								nonEvents++;
								pw.print(instance + " " + "Non-Event" + " " + word + " " + stem + " " + affix + " ");
							}
							else {
								events++;
								pw.print(instance + " " + "Event" + " " + word + " " + stem + " " + affix + " ");
							}
							//print word class features
							pw.print(evt.pos + " ");
							pw.flush();
							IIndexWord idxWord = null;
							idxWord = dict.getIndexWord(stem, pos);
							ArrayList<String> synsets = new ArrayList<String>();
							int hypernym_feature = 0;
							String sense = null;
							if (idxWord != null) {
    						for (int k = 0; k <  idxWord.getWordIDs().size(); k++){

      						IWordID wordID = idxWord.getWordIDs().get(k);
      						IWord iword = dict.getWord(wordID);
      						if (k == 0)
    								sense = iword.getSenseKey().toString();
      						synsets.add(iword.getSynset().getWord(1).toString());
      						System.out.println(iword.getSynset().getWord(1).toString());
    						}
    					}
    					for (String synset : synsets) {
    						for (String hypernym : hypernyms) {
    							if (synset.contains(hypernym)) {
    								hypernym_feature = 1;
    								break;
    							}
    						}
    					}
							//print semantic class features
							if (sense == null) 
								pw.print("null ");
							else
								pw.print(sense + " ");
							//print hypernym features
							pw.print(hypernym_feature+ "\n");
							pw.flush();
							refPw.flush();
						}
					}
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		pw.close();
		refPw.close();
		System.out.println("Verbal Events:" + events + ", Verbal Non-Events:" + nonEvents);
	}
}
class TimeMLEvent {
	String eventID;
	String eventInstID;
	String pos;
	TimeMLEvent() {
		eventID =null;
		eventInstID = null;
		pos = null;
	}
}