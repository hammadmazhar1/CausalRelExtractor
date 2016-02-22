import java.util.List;
import java.util.ArrayList;

public class Word_Pair implements Comparable<Word_Pair> {
  public int          actualCount;
  public int          documentCount;
  public String       word_one;
  public String       word_two;
  public List<String> sentences;

  public Word_Pair(String w1, String w2) {
    w1 = w1.toLowerCase();
    w2 = w2.toLowerCase();
    if (w1.compareTo(w2) < 0) {
      word_one = w1;
      word_two = w2;
    } else {
      word_one = w2;
      word_two = w1;
    }

    actualCount = 1;
    documentCount = 1;
    sentences = new ArrayList<>();
  }

  public Word_Pair(String w1, String w2, int _document, int _actual) {
    w1 = w1.toLowerCase();
    w2 = w2.toLowerCase();
    if (w1.compareTo(w2) < 0) {
      word_one = w1;
      word_two = w2;
    } else {
      word_one = w2;
      word_two = w1;
    }

    actualCount = _actual;
    documentCount = _document;
    sentences = new ArrayList<>();
  }

  public String toString() {
    return word_one + " - " + word_two;
  }

  public String print() {
    return Integer.toString(documentCount) + "\t" + Integer.toString(actualCount) + "\t" + toString();
  }

  public String printWithSentences() {
    String temp = toString() + "\n";
    temp += Integer.toString(sentences.size()) + "\n";
    for (int i = 0; i < sentences.size(); i++) {
      temp += sentences.get(i) + "\n";      
    }
    return temp;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Word_Pair) {
      Word_Pair wp = (Word_Pair) o;
      return word_one.equals(wp.word_one) && word_two.equals(wp.word_two);
    }
    return false;
  }

  public boolean equals(Word_Pair wp) {
    return word_one.equals(wp.word_one) && word_two.equals(wp.word_two);
  }

  public void documentIncrement() {
    documentCount++;
  }

  public void documentIncrement(int inc) {
    documentCount += inc;
  }

  public void actualIncrement() {
    actualCount++;
  }

  public void actualIncrement(int inc) {
    actualCount += inc;
  }

  @Override
  public int compareTo(Word_Pair another) {
    if (this.toString().compareTo(another.toString()) < 0) {
        return -1;
    } else {
        return 1;
    }
  }
}
