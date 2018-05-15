package com.github.yktakaha4.watsonmusic.batch.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.github.yktakaha4.watsonmusic.model.Artwork;
import com.github.yktakaha4.watsonmusic.model.Source;
import com.github.yktakaha4.watsonmusic.model.Text;
import com.github.yktakaha4.watsonmusic.model.VisualRecognitionResult;
import com.github.yktakaha4.watsonmusic.model.WikiPage;
import com.github.yktakaha4.watsonmusic.service.TextService;
import com.github.yktakaha4.watsonmusic.service.TextTagService;
import com.github.yktakaha4.watsonmusic.service.WikipediaService;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassResult;

@Controller
@StepScope
public class VisualRecognitionResultTextWriter implements ItemWriter<Pair<Artwork, VisualRecognitionResult>> {
  private static final int VISUAL_RECOGNITION_RELEVANCE = 3;
  private static final float VISUAL_RECOGNITION_SCORE_THRESHOLD = 0.75f;

  private final WikipediaService wikipediaService;
  private final TextService textService;
  private final TextTagService textTagService;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  public VisualRecognitionResultTextWriter(WikipediaService wikipediaService, TextService textService,
      TextTagService textTagService) {
    this.wikipediaService = wikipediaService;
    this.textService = textService;
    this.textTagService = textTagService;
  }

  @Override
  public void write(List<? extends Pair<Artwork, VisualRecognitionResult>> pairs) throws Exception {
    pairs.forEach(this::write);
  }

  private void write(Pair<Artwork, VisualRecognitionResult> pair) {
    Artwork artwork = pair.getLeft();
    VisualRecognitionResult visualRecognitionResult = pair.getRight();

    List<ClassResult> classResults = filterRelatedClassResults(visualRecognitionResult.getClassResults());
    if (classResults.isEmpty()) {
      return;
    }

    List<Text> texts = new ArrayList<>();
    List<String> relatedResults = new ArrayList<>();
    List<WikiPage> wikiPages = classResults.stream()
        .map((classResult) -> {
          String name = classResult.getClassName().split("[　 （(]+", -1)[0];
          Float score = classResult.getScore();
          return Pair.of(Pair.of(name, score), wikipediaService.getWikiPage(name));
        }).filter((p) -> {
          return p.getRight().isPresent();
        }).sorted((l, r) -> {
          return r.getLeft().getValue().compareTo(l.getLeft().getValue());
        }).peek((p) -> {
          relatedResults.add(String.format("%s(%s)", p.getLeft().getKey(), p.getLeft().getValue()));
        }).map((p) -> {
          return p.getRight().get();
        }).collect(Collectors.toList());
    if (wikiPages.isEmpty()) {
      Text text = new Text();
      text.setTitle(visualRecognitionResult.getImageHash());
      text.setSource(Source.VISUAL_RECOGNITION);
      text.setContent(
          classResults.stream().map(classResult -> classResult.getClassName()).collect(Collectors.joining(" ")));
      text.setRaw(text.getContent());
      text.setTextLinks(Collections.emptyList());
      texts.add(text);
    } else {
      wikiPages.stream().map(textService::createText).forEach(texts::add);
      logger.info("related wikipages: " + visualRecognitionResult.getImageTitle() + " -> "
          + relatedResults.stream().collect(Collectors.joining(", ")));
    }

    int relevance = VISUAL_RECOGNITION_RELEVANCE;
    for (Text text : texts) {
      textService.manage(text);
      Text newText = textService.getText(text.getSource(), text.getTitle()).get();
      textTagService.addRelation(newText, artwork, relevance);
      relevance++;
    }
  }

  private List<ClassResult> filterRelatedClassResults(List<ClassResult> classResults) {
    if (classResults.isEmpty()) {
      return Collections.emptyList();
    }

    List<ClassResult> sortedClassResults = new ArrayList<>(classResults);
    sortedClassResults.sort((l, r) -> {
      return r.getScore().compareTo(l.getScore());
    });

    List<ClassResult> relatedClassResults = sortedClassResults.stream().filter((classResult) -> {
      return classResult.getScore() > VISUAL_RECOGNITION_SCORE_THRESHOLD;
    }).filter((classResult) -> {
      return sortedClassResults.stream().noneMatch((checkingClassResult) -> {
        boolean hasSameClassName = StringUtils.equalsIgnoreCase(classResult.getClassName(),
            checkingClassResult.getClassName());
        boolean isInTypeHierarchies = StringUtils.equalsAnyIgnoreCase(classResult.getClassName(),
            checkingClassResult.getTypeHierarchy().split("/"));
        return !hasSameClassName && isInTypeHierarchies;
      });
    }).collect(Collectors.toList());

    if (relatedClassResults.isEmpty()) {
      relatedClassResults.add(sortedClassResults.get(0));
    }
    return relatedClassResults;
  }
}
