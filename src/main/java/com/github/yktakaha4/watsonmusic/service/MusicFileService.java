package com.github.yktakaha4.watsonmusic.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.yktakaha4.watsonmusic.BatchProperties;
import com.github.yktakaha4.watsonmusic.model.Artwork;
import com.github.yktakaha4.watsonmusic.model.Music;
import com.github.yktakaha4.watsonmusic.model.MusicTag;
import com.github.yktakaha4.watsonmusic.util.CharacterEncoder;
import com.github.yktakaha4.watsonmusic.util.IdentifierUtils;
import com.github.yktakaha4.watsonmusic.util.PathUtils;

@Service
public class MusicFileService {
  private final BatchProperties batchProperties;
  private final IdentifierUtils identifierUtils;
  private final PathUtils pathUtils;
  private final CharacterEncoder characterEncoder;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  public MusicFileService(BatchProperties batchProperties, IdentifierUtils identifierUtils, PathUtils pathUtils,
      CharacterEncoder characterEncoder) {
    this.batchProperties = batchProperties;
    this.identifierUtils = identifierUtils;
    this.pathUtils = pathUtils;
    this.characterEncoder = characterEncoder;
  }

  @Transactional(readOnly = true)
  public Optional<Music> createMusic(File file) {
    Music music = new Music();
    music.setPath(pathUtils.fromBasePath(file.toPath()).toString());

    try {
      music.setFileHash(identifierUtils.createFileHash(file));
    } catch (IOException e) {
      logger.warn("failed create file hash: " + music.getPath(), e);
      return Optional.empty();
    }
    music.setTextTag(identifierUtils.newTag());

    AudioFile audioFile;
    try {
      audioFile = AudioFileIO.read(file);
    } catch (IOException | CannotReadException | TagException | ReadOnlyFileException | InvalidAudioFrameException e) {
      logger.warn("failed read mp3 file: " + music.getPath(), e);
      return Optional.empty();
    }

    AudioHeader audioHeader = audioFile.getAudioHeader();
    music.setTrackLength(audioHeader.getTrackLength());
    music.setEncoding(audioHeader.getEncodingType());

    Tag tag = audioFile.getTag();
    List<MusicTag> musicTags = Arrays.stream(FieldKey.values()).map((fieldKey) -> {
      return Pair.of(fieldKey, tag.getFirst(fieldKey));
    }).filter((pair) -> {
      return StringUtils.isNoneBlank(pair.getRight());
    }).map((pair) -> {
      MusicTag musicTag = new MusicTag();
      musicTag.setName(pair.getLeft());
      musicTag.setValue(pair.getRight());
      musicTag.setTextTag(identifierUtils.newTag());
      return musicTag;
    }).collect(Collectors.toList());

    {
      List<String> sources = musicTags.stream().map(musicTag -> musicTag.getValue()).collect(Collectors.toList());
      List<String> hints = Arrays.asList(music.getPath().split(batchProperties.getMusicfilePathSeparator()));
      Pair<Charset, Charset> encodings = characterEncoder.detect(sources, hints);
      musicTags.forEach(musicTag -> {
        musicTag.setValue(new String(musicTag.getValue().getBytes(encodings.getLeft()), encodings.getRight()));
      });
      logger.debug(String.format("%s, %s, %s, %s", encodings,
          musicTags.stream().map(t -> t.getValue()).collect(Collectors.toList()), sources, hints));
    }

    music.setMusicTags(musicTags);

    List<Artwork> artworks = tag.getArtworkList().stream().map((tagArtwork) -> {
      Artwork artwork = new Artwork();
      byte[] binaryData = tagArtwork.getBinaryData();
      artwork.setImage(binaryData);
      artwork.setImageHash(identifierUtils.toHash(binaryData));
      artwork.setMimetype(tagArtwork.getMimeType());
      artwork.setTextTag(identifierUtils.newTag());
      return artwork;
    }).collect(Collectors.toList());

    music.setArtworks(artworks);

    return Optional.of(music);
  }

}
