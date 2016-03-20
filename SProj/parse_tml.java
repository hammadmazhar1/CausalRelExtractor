import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.io.*;
import java.util.*;

public class TimeMLParser{
	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("Usage: java parse_tml <directory>");
			return;
		}
		String directory = args[1];
		File[] files = new File(directory).listFiles("*.tml");
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		for (File curFile : files) {
			Document doc = builder.parse(curFile);
			doc.getDocumentElement().normalize();

			XPath xPath = XPathFactory.newInstance().newXPath();

			String expression = "/TimeMl/EVENT";
			NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node nNode = nodeList.item(i);
				System.out.println("\nCurrent Element :" + nNode.getNodeName());
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					System.out.println("Event type:" + eElement.getAttribute("class"));
					System.out.println("Event stem:" + eElement.getAttribute("stem"));
				}
			}
		}
	}
}