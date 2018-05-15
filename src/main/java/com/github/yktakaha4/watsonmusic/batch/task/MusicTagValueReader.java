package com.github.yktakaha4.watsonmusic.batch.task;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.jaudiotagger.tag.FieldKey;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.yktakaha4.watsonmusic.service.MusicTagService;
import com.github.yktakaha4.watsonmusic.util.kuromoji.Kuromoji;

@Component
@StepScope
public class MusicTagValueReader implements ItemReader<String>, ItemStream {
  private final MusicTagService musicTagService;
  private final Kuromoji kuromoji;

  private Iterator<String> iterator;

  @Autowired
  public MusicTagValueReader(MusicTagService musicTagService, Kuromoji kuromoji) {
    this.musicTagService = musicTagService;
    this.kuromoji = kuromoji;
  }

  @Override
  public String read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
    return iterator.hasNext() ? iterator.next() : null;
  }

  @Override
  public void open(ExecutionContext executionContext) throws ItemStreamException {
    List<String> tagValues = musicTagService.getEnrichingMusicTags().stream().flatMap((musicTag) -> {
      String tagValue = musicTag.getValue();

      List<String> values = new ArrayList<>();
      if (musicTag.getName().equals(FieldKey.YEAR)) {
        tagValue += "年";
        values.add(tagValue);
      } else if (musicTag.getName().equals(FieldKey.ARTIST) || musicTag.getName().equals(FieldKey.ALBUM_ARTIST)) {
        values.add(tagValue);
        values.addAll(
            kuromoji.analyzeNouns(tagValue)
                .stream()
                .map(kuromojiResult -> kuromojiResult.getCharTerm())
                .collect(Collectors.toList()));
      } else {
        values.add(tagValue);
      }
      return values.stream();
    }).sorted().distinct().collect(Collectors.toList());

    iterator = tagValues.iterator();
  }

  @Override
  public void update(ExecutionContext executionContext) throws ItemStreamException {
  }

  @Override
  public void close() throws ItemStreamException {
    iterator = null;
  }

}
