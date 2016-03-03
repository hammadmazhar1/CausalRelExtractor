class Word_Count implements Comparable<Word_Count> {
  public int    actualCount;
  public int    documentCount;
  public String word;

  public Word_Count(String w1) {
    word          = w1.toLowerCase();
    actualCount   = 1;
    documentCount = 1;
  }

  public Word_Count(String w1, int _document, int _actual) {
    word = w1.toLowerCase();
    actualCount = _actual;
    documentCount = _document;
  }

  public String print() {
    return Integer.toString(documentCount) + "            " + Integer.toString(actualCount) + "          " + word;
  }

  public String prettyPrint() {
    String s = Integer.toString(documentCount);
    int temp = documentCount;
    int num = 0;
    while (temp > 0) {
      temp /= 10;
      num++;
    }
    for (int i = 14; i > num; i--) {
      s += " ";
    }

    s += Integer.toString(actualCount);
    temp = actualCount;
    num = 0;
    while (temp > 0) {
      temp /= 10;
      num++;
    }
    for (int i = 14; i > num; i--) {
      s += " ";
    }

    s += word;
    return s;
  }

  public boolean equals(Word_Count wc) {
    return word.equals(wc.word);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Word_Count) {
      Word_Count wc = (Word_Count) o;
      return word.equals(wc.word);
    }
    return false;
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
  public int compareTo(Word_Count another) {
    if (this.word.compareTo(another.word) < 0) {
        return -1;
    } else {
        return 1;
    }
  }
}