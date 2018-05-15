package com.github.yktakaha4.watsonmusic.batch.task;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.spell.StringDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.yktakaha4.watsonmusic.model.MusicTag;
import com.github.yktakaha4.watsonmusic.model.Source;
import com.github.yktakaha4.watsonmusic.model.Text;
import com.github.yktakaha4.watsonmusic.model.TextLink;
import com.github.yktakaha4.watsonmusic.model.WikiPage;
import com.github.yktakaha4.watsonmusic.service.MusicTagService;
import com.github.yktakaha4.watsonmusic.service.TextService;
import com.github.yktakaha4.watsonmusic.service.TextTagService;
import com.github.yktakaha4.watsonmusic.service.WikipediaService;
import com.github.yktakaha4.watsonmusic.util.NormalizedStringDistance;
import com.github.yktakaha4.watsonmusic.util.kuromoji.Kuromoji;

@Component
@StepScope
public class WikiPageTextWriter implements ItemWriter<String>, ItemStream {
  private static final int NEST_MAX = 1;
  private static final double STRING_DISTANCE_THRESHOLD = 0.75;

  private final WikipediaService wikipediaService;
  private final TextService textService;
  private final TextTagService textTagService;
  private final MusicTagService musicTagService;
  private final Kuromoji kuromoji;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private Set<String> taggedTitles = Collections.synchronizedSet(new HashSet<>());
  private List<MusicTag> musicTags;

  @Autowired
  public WikiPageTextWriter(WikipediaService wikipediaService, TextService textService, TextTagService textTagService,
      MusicTagService musicTagService, Kuromoji kuromoji) {
    this.wikipediaService = wikipediaService;
    this.textService = textService;
    this.textTagService = textTagService;
    this.musicTagService = musicTagService;
    this.kuromoji = kuromoji;
  }

  @Override
  public void write(List<? extends String> titles) throws Exception {
    titles.forEach(title -> write(title, 0));
  }

  private void write(String title, int nest) {
    StringDistance stringDistance = new NormalizedStringDistance();
    synchronized (taggedTitles) {
      if (taggedTitles.contains(title)) {
        return;
      }
      taggedTitles.add(title);
    }

    List<MusicTag> relatedMusicTags = musicTags.stream().filter((musicTag) -> {
      Set<String> terms = new HashSet<>();
      terms.add(musicTag.getValue());
      terms.addAll(kuromoji.analyzeNouns(musicTag.getValue()).stream().map((kuromojiResult) -> {
        return kuromojiResult.getCharTerm();
      }).filter(charTerm -> charTerm.length() > 3).collect(Collectors.toList()));
      Double distance = terms.stream().mapToDouble((term) -> {
        return stringDistance.getDistance(title, term);
      }).max().orElse(0);
      return distance > STRING_DISTANCE_THRESHOLD;
    }).collect(Collectors.toList());

    if (!relatedMusicTags.isEmpty()) {
      Optional<WikiPage> maybeWikiPage = wikipediaService.getWikiPage(title);
      if (maybeWikiPage.isPresent()) {
        Text text = manage(maybeWikiPage.get());
        relatedMusicTags.forEach((musicTag) -> {
          int relevance = calcRelevance(musicTag, nest);
          textTagService.addRelation(text, musicTag.getTextTag(), relevance);
        });

        Map<String, Long> loggingResults = relatedMusicTags.stream().collect(Collectors.groupingBy((musicTag) -> {
          return musicTag.getValue();
        }, Collectors.counting()));
        logger.info("related music tags: " + title + " -> " + loggingResults);

        if (nest < NEST_MAX) {
          List<String> textLinkTitles = text.getTextLinks().stream().map(tl -> tl.getTitle())
              .collect(Collectors.toList());
          logger.info("search text links: " + String.join(", ", textLinkTitles));
          for (String textLinkTitle : textLinkTitles) {
            write(textLinkTitle, nest + 1);
          }
        }
      }
    }
  }

  private Text manage(WikiPage wikiPage) {
    Text text = new Text();
    text.setTitle(wikiPage.getTitle());
    text.setSource(Source.WIKIPEDIA);
    text.setContent(wikiPage.getContent());
    text.setRaw(wikiPage.getRaw());
    text.setTextLinks(wikiPage.getLinkTitles().stream().map((linkTitle) -> {
      TextLink textLink = new TextLink();
      textLink.setTitle(linkTitle.getLeft());
      textLink.setLinkType(linkTitle.getRight());
      return textLink;
    }).collect(Collectors.toList()));

    textService.manage(text);
    return textService.getText(text.getSource(), text.getTitle()).get();
  }

  private int calcRelevance(MusicTag musicTag, int nest) {
    int relevance;
    switch (musicTag.getName()) {
    case ALBUM:
    case ALBUM_ARTIST:
    case TITLE:
      relevance = 0;
      break;
    case ARTIST:
      relevance = 1;
      break;
    case GENRE:
      relevance = 3;
      break;
    case YEAR:
      relevance = 5;
      break;
    default:
      if (StringUtils.isNumeric(musicTag.getValue())) {
        relevance = 10;
      } else {
        relevance = 3;
      }
    }
    return relevance + nest;
  }

  @Override
  public void open(ExecutionContext executionContext) throws ItemStreamException {
    musicTags = musicTagService.getEnrichingMusicTags();
    taggedTitles.clear();
  }

  @Override
  public void update(ExecutionContext executionContext) throws ItemStreamException {
  }

  @Override
  public void close() throws ItemStreamException {
    musicTags = null;
    taggedTitles.clear();
  }

}
