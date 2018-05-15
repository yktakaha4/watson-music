package com.github.yktakaha4.watsonmusic.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jaudiotagger.tag.FieldKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.yktakaha4.watsonmusic.BatchProperties;
import com.github.yktakaha4.watsonmusic.mapper.MusicTagMapper;
import com.github.yktakaha4.watsonmusic.model.MusicTag;

@Service
public class MusicTagService {
  private final MusicTagMapper musicTagMapper;

  private final List<FieldKey> enrichingMusicTags;

  @Autowired
  public MusicTagService(BatchProperties batchProperties, MusicTagMapper musicTagMapper) {
    this.musicTagMapper = musicTagMapper;
    this.enrichingMusicTags = Stream.of(batchProperties.getEnrichingMusicTags().split(",")).map((tagName) -> {
      return FieldKey.valueOf(tagName);
    }).sorted().distinct().collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<MusicTag> getEnrichingMusicTags() {
    return getByFieldKeyIn(enrichingMusicTags);
  }

  @Transactional(readOnly = true)
  public List<MusicTag> getAllMusicTags() {
    return musicTagMapper.selectAll();
  }

  @Transactional(readOnly = true)
  public List<MusicTag> getByFieldKeyIn(FieldKey... fieldKeys) {
    return getByFieldKeyIn(Arrays.asList(fieldKeys));
  }

  @Transactional(readOnly = true)
  public List<MusicTag> getByFieldKeyIn(Collection<FieldKey> fieldKeys) {
    return getByTagNameIn(fieldKeys.stream().map(f -> f.toString()).toArray(String[]::new));
  }

  @Transactional(readOnly = true)
  public List<MusicTag> getByTagNameIn(String... names) {
    return musicTagMapper.selectByNamesIn(Arrays.asList(names));
  }

}
