package com.github.yktakaha4.watsonmusic.util;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.function.Function;

import org.apache.lucene.search.spell.LevensteinDistance;
import org.apache.lucene.search.spell.StringDistance;

public class NormalizedStringDistance implements StringDistance {
  private final StringDistance stringDistance = new LevensteinDistance();

  private Function<String, String> normalizer;

  @Override
  public float getDistance(String string1, String string2) {
    if (string1 == null || string2 == null) {
      return 0;
    }
    return stringDistance.getDistance(normalize(string1), normalize(string2));
  }

  private String normalize(String string) {
    String newString = string;
    newString = Normalizer.normalize(newString, Form.NFKC);
    if (normalizer != null) {
      newString = normalizer.apply(newString);
    }
    return newString;
  }

  public Function<String, String> getNormalizer() {
    return normalizer;
  }

  public void setNormalizer(Function<String, String> normalizer) {
    this.normalizer = normalizer;
  }

}
