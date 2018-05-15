package com.github.yktakaha4.watsonmusic.batch.task;

import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.jaudiotagger.tag.FieldKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.github.yktakaha4.watsonmusic.model.Lyrics;
import com.github.yktakaha4.watsonmusic.model.Music;
import com.github.yktakaha4.watsonmusic.model.MusicTag;
import com.github.yktakaha4.watsonmusic.service.LyricsService;
import com.github.yktakaha4.watsonmusic.util.kuromoji.Kuromoji;

@Controller
@StepScope
public class MusicToLyricsProcessor implements ItemProcessor<Music, Pair<Music, Lyrics>> {
  private final LyricsService lyricsService;
  private final Kuromoji kuromoji;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  public MusicToLyricsProcessor(LyricsService lyricsService, Kuromoji kuromoji) {
    this.lyricsService = lyricsService;
    this.kuromoji = kuromoji;
  }

  @Override
  public Pair<Music, Lyrics> process(Music music) throws Exception {
    Optional<MusicTag> maybeTitleTag = music.getMusicTag(FieldKey.TITLE);
    if (!maybeTitleTag.isPresent()) {
      return null;
    }
    String title = maybeTitleTag.get().getValue();

    SortedSet<String> artistNames = new TreeSet<>();
    music.getMusicTag(FieldKey.ALBUM_ARTIST).ifPresent((musicTag) -> {
      artistNames.add(musicTag.getValue());
    });
    music.getMusicTag(FieldKey.ARTIST).ifPresent((musicTag) -> {
      artistNames.add(musicTag.getValue());
      artistNames.addAll(kuromoji.analyzeNouns(musicTag.getValue()).stream()
          .map(kuromojiResult -> kuromojiResult.getCharTerm()).collect(Collectors.toList()));
    });
    if (artistNames.isEmpty()) {
      logger.info("do not have any artist music tags: " + music.getPath());
      return null;
    }

    for (String artistName : artistNames) {
      Optional<Lyrics> maybeLyrics = lyricsService.getLyrics(artistName, title);
      if (maybeLyrics.isPresent()) {
        logger.info("matched lyrics: " + title);
        return Pair.of(music, maybeLyrics.get());
      }
    }

    logger.info("failed get lyrics: " + title + " -> " + artistNames);
    return null;
  }

}
