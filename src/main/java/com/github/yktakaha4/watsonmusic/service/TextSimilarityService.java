package com.github.yktakaha4.watsonmusic.service;

import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.yktakaha4.watsonmusic.mapper.TextLinkMapper;
import com.github.yktakaha4.watsonmusic.mapper.TextMapper;
import com.github.yktakaha4.watsonmusic.model.Source;
import com.github.yktakaha4.watsonmusic.model.Text;
import com.github.yktakaha4.watsonmusic.model.TextSimilarity;
import com.github.yktakaha4.watsonmusic.model.TextSimilarityDetail;

@Component
public class TextSimilarityService {
  private final TextMapper textMapper;
  private final TextLinkMapper textLinkMapper;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private Double k1;
  private Double b;
  private Double avgdl;

  @Autowired
  public TextSimilarityService(TextMapper textMapper, TextLinkMapper textLinkMapper) {
    this.textMapper = textMapper;
    this.textLinkMapper = textLinkMapper;
  }

  public void initialize() {
    initialize(2.0, 0.75);
  }

  public void initialize(double k1, double b) {
    this.k1 = k1;
    this.b = b;
    this.avgdl = FastMath.ceil(textMapper.selectBySource(Source.WIKIPEDIA)
        .stream().mapToInt(this::calcWordCount).average().orElse(0));
    logger.info(String.format("text similarity service initialized: k1 = %s, b = %s, avgdl = %s", k1, b,
        avgdl));
  }

  public TextSimilarity getTextSimilarity(Text text) {
    Objects.requireNonNull(k1);
    Objects.requireNonNull(b);
    Objects.requireNonNull(avgdl);

    String content = text.getContent();
    double wordCount = calcWordCount(text);
    double docCount = textMapper.count(text.getSource());

    TextSimilarity textSimilarity = new TextSimilarity();
    textSimilarity.setWord(text.getTitle());
    textSimilarity.setDetails(text.getTextLinks().stream().map((textLink) -> {
      String term = textLink.getTitle();

      double termCount = calcTermCount(term, content);
      double containsCount = calcContainsCount(text.getSource(), term);

      double tf = termCount / wordCount;
      double idf = FastMath.log(docCount / containsCount);
      double tfIdf = tf * idf;

      double dl = calcDocumentLength(content);
      double bm25 = calcBm25(tf, tfIdf, dl);

      TextSimilarityDetail detail = new TextSimilarityDetail();
      detail.setWord(term);
      detail.setTfIdf(tfIdf);
      detail.setBm25(bm25);

      return detail;
    }).collect(Collectors.toList()));

    return textSimilarity;
  }

  private int calcWordCount(Text text) {
    return textLinkMapper.selectByTextId(text.getTextId()).stream().mapToInt((textLink) -> {
      return calcTermCount(textLink.getTitle(), text.getContent());
    }).sum();
  }

  private int calcTermCount(String term, String content) {
    return content.split(Pattern.quote(term), -1).length - 1;
  }

  private int calcContainsCount(Source source, String term) {
    return textLinkMapper.countTextOfContainsTitle(source, term);
  }

  private double calcDocumentLength(String text) {
    return ((double) text.length()) / avgdl;
  }

  private double calcBm25(double tf, double idf, double dl) {
    return (tf * idf * (k1 + 1)) / (k1 * (1 - b + (b * dl)) + tf);
  }

}
