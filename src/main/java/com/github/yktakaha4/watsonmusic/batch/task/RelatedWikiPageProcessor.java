package com.github.yktakaha4.watsonmusic.batch.task;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.github.yktakaha4.watsonmusic.model.LinkType;
import com.github.yktakaha4.watsonmusic.model.Text;
import com.github.yktakaha4.watsonmusic.model.TextSimilarity;
import com.github.yktakaha4.watsonmusic.model.WikiPage;
import com.github.yktakaha4.watsonmusic.service.TextSimilarityService;
import com.github.yktakaha4.watsonmusic.service.WikipediaService;

@Controller
@StepScope
public class RelatedWikiPageProcessor implements ItemProcessor<Text, Pair<Text, List<WikiPage>>>, ItemStream {
  private static final double SIMILARITY_THRESHOLD = 0.05;
  private static final double SIMIRARITY_BIAS_INFOBOX = SIMILARITY_THRESHOLD * 0.99;
  private static final double SIMIRARITY_BIAS_NAVBOX = SIMILARITY_THRESHOLD * 0.9;
  private static final int RELATED_MAX = 5;
  private final WikipediaService wikipediaService;
  private final TextSimilarityService textSimilarityService;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  public RelatedWikiPageProcessor(WikipediaService wikipediaService, TextSimilarityService textSimilarityService) {
    this.wikipediaService = wikipediaService;
    this.textSimilarityService = textSimilarityService;
  }

  @Override
  public Pair<Text, List<WikiPage>> process(Text text) throws Exception {
    List<String> infoboxTitles = text.getTextLinks().stream().filter(tl -> tl.getLinkType().equals(LinkType.INFOBOX))
        .map(tl -> tl.getTitle()).collect(Collectors.toList());
    List<String> navboxTitles = text.getTextLinks().stream().filter(tl -> tl.getLinkType().equals(LinkType.NAVBOX))
        .map(tl -> tl.getTitle()).collect(Collectors.toList());

    logger.debug("text: " + text.getTitle());
    TextSimilarity textSimilarity = textSimilarityService.getTextSimilarity(text);
    List<String> relatedDetails = new ArrayList<>();
    List<WikiPage> relatedWikiPages = textSimilarity.getDetails().stream().map((detail) -> {
      // リンクの種別に応じて類似度に加点
      double bias = 0;
      if (infoboxTitles.contains(detail.getWord())) {
        bias = SIMIRARITY_BIAS_INFOBOX;
      } else if (navboxTitles.contains(detail.getWord())) {
        bias = SIMIRARITY_BIAS_NAVBOX;
      }
      // TF-IDF or BM25を選択
      logger.debug("word: " + detail.getWord() + ", value: " + detail.getBm25() + ", bias: " + bias);
      return Pair.of(detail.getWord(), detail.getBm25() + bias);
    }).sorted((l, r) -> {
      return r.getValue().compareTo(l.getValue());
    }).filter((pair) -> {
      return pair.getValue() > SIMILARITY_THRESHOLD;
    }).map((pair) -> {
      return Pair.of(pair, wikipediaService.getWikiPage(pair.getLeft()));
    }).limit(RELATED_MAX).filter((pair) -> {
      return pair.getRight().isPresent();
    }).peek((pair) -> {
      relatedDetails.add(pair.getKey().toString());
    }).map((pair) -> {
      return pair.getRight().get();
    }).collect(Collectors.toList());

    if (!relatedDetails.isEmpty()) {
      logger.info("related wikipages: " + text.getTitle() + " -> "
          + relatedDetails.stream().collect(Collectors.joining(", ")));
    }

    return Pair.of(text, relatedWikiPages);
  }

  @Override
  public void open(ExecutionContext executionContext) throws ItemStreamException {
    textSimilarityService.initialize();
    logger.info("similarity threshold: relevance > " + SIMILARITY_THRESHOLD + " and top " + RELATED_MAX + " pages");
  }

  @Override
  public void update(ExecutionContext executionContext) throws ItemStreamException {
  }

  @Override
  public void close() throws ItemStreamException {
  }

}
