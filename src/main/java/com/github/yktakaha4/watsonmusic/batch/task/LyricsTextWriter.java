package com.github.yktakaha4.watsonmusic.batch.task;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.github.yktakaha4.watsonmusic.model.Lyrics;
import com.github.yktakaha4.watsonmusic.model.Music;
import com.github.yktakaha4.watsonmusic.model.Source;
import com.github.yktakaha4.watsonmusic.model.Text;
import com.github.yktakaha4.watsonmusic.service.TextService;
import com.github.yktakaha4.watsonmusic.service.TextTagService;
import com.github.yktakaha4.watsonmusic.util.IdentifierUtils;

@Controller
@StepScope
public class LyricsTextWriter implements ItemWriter<Pair<Music, Lyrics>> {
  private static final int LYRICS_RELEVANCE = 0;

  private final TextService textService;
  private final TextTagService textTagService;
  private final IdentifierUtils identifierUtils;

  @Autowired
  public LyricsTextWriter(TextService textService, TextTagService textTagService, IdentifierUtils identifierUtils) {
    this.textService = textService;
    this.textTagService = textTagService;
    this.identifierUtils = identifierUtils;
  }

  @Override
  public void write(List<? extends Pair<Music, Lyrics>> pairs) throws Exception {
    pairs.forEach(this::write);
  }

  private void write(Pair<Music, Lyrics> pair) {
    Music music = pair.getLeft();
    Lyrics lyrics = pair.getRight();

    String identifier = identifierUtils.identifier(lyrics.getTitle(), lyrics.getArtist());
    Text text = new Text();
    text.setSource(Source.LYRICS);
    text.setTitle(identifier);
    text.setContent(lyrics.getLyric());
    text.setRaw(lyrics.getLyric());
    text.setTextLinks(Collections.emptyList());

    textService.manage(text);
    Text newText = textService.getText(Source.LYRICS, identifier).get();
    textTagService.addRelation(newText, music, LYRICS_RELEVANCE);

  }

}
