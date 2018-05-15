package com.github.yktakaha4.watsonmusic.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.jaudiotagger.tag.FieldKey;

import lombok.Data;

@Data
public class Music implements HasTextTag {
  private Integer musicId;
  private String path;
  private String encoding;
  private Integer trackLength;
  private String fileHash;
  private String textTag;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime missingAt;
  private List<MusicTag> musicTags;
  private List<Artwork> artworks;

  public Optional<MusicTag> getMusicTag(FieldKey fieldKey) {
    return musicTags.stream().filter(musicTag -> musicTag.getName().equals(fieldKey)).findFirst();
  }

  public Optional<String> getMusicTagValue(FieldKey fieldKey) {
    Optional<MusicTag> maybeMusicTag = getMusicTag(fieldKey);
    if (maybeMusicTag.isPresent()) {
      return Optional.of(maybeMusicTag.get().getValue());
    } else {
      return Optional.empty();
    }
  }

}
