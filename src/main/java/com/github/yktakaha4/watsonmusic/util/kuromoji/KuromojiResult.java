package com.github.yktakaha4.watsonmusic.util.kuromoji;

public class KuromojiResult {
  private String charTerm;
  private String reading;
  private String partOfSpeech;

  public String getCharTerm() {
    return charTerm;
  }

  public void setCharTerm(String charTerm) {
    this.charTerm = charTerm;
  }

  public String getReading() {
    return reading;
  }

  public void setReading(String reading) {
    this.reading = reading;
  }

  public String getPartOfSpeech() {
    return partOfSpeech;
  }

  public void setPartOfSpeech(String partOfSpeech) {
    this.partOfSpeech = partOfSpeech;
  }

}
