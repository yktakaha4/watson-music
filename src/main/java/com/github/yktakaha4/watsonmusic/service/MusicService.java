package com.github.yktakaha4.watsonmusic.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.yktakaha4.watsonmusic.mapper.ArtworkMapper;
import com.github.yktakaha4.watsonmusic.mapper.MusicArtworkMapper;
import com.github.yktakaha4.watsonmusic.mapper.MusicMapper;
import com.github.yktakaha4.watsonmusic.mapper.MusicTagMapper;
import com.github.yktakaha4.watsonmusic.model.Artwork;
import com.github.yktakaha4.watsonmusic.model.Music;
import com.github.yktakaha4.watsonmusic.model.MusicTag;

@Service
public class MusicService {
  private final MusicMapper musicMapper;
  private final MusicTagMapper musicTagMapper;
  private final MusicArtworkMapper musicArtworkMapper;
  private final ArtworkMapper artworkMapper;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  public MusicService(MusicMapper musicMapper, MusicTagMapper musicTagMapper, MusicArtworkMapper musicArtworkMapper,
      ArtworkMapper artworkMapper) {
    this.musicMapper = musicMapper;
    this.musicTagMapper = musicTagMapper;
    this.musicArtworkMapper = musicArtworkMapper;
    this.artworkMapper = artworkMapper;
  }

  @Transactional(readOnly=true)
  public Integer getLivingMusicCount() {
    return musicMapper.selectLivingCount();
  }

  @Transactional
  public void missingAll() {
    LocalDateTime localDateTime = LocalDateTime.now();
    musicMapper.updateMissingAtAll(localDateTime);
  }

  @Transactional
  public void manage(Music music) {
    final LocalDateTime localDateTime = LocalDateTime.now();

    boolean shouldInsert = false;
    boolean shouldUpdate = false;
    boolean shouldUpdateDetails = false;

    Optional<Music> maybeCurrentMusic = getByPath(music.getPath());
    if (maybeCurrentMusic.isPresent()) {
      Music currentMusic = maybeCurrentMusic.get();
      if (!StringUtils.equals(music.getFileHash(), currentMusic.getFileHash())) {
        shouldUpdate = true;
        shouldUpdateDetails = true;
      }
    } else {
      maybeCurrentMusic = getByFileHash(music.getFileHash()).stream().findFirst();
      if (maybeCurrentMusic.isPresent()) {
        shouldUpdate = true;
      } else {
        shouldInsert = true;
      }
    }

    Integer musicId;
    if (shouldInsert) {
      Music newMusic = new Music();
      newMusic.setPath(music.getPath());
      newMusic.setEncoding(music.getEncoding());
      newMusic.setTrackLength(music.getTrackLength());
      newMusic.setFileHash(music.getFileHash());
      newMusic.setTextTag(music.getTextTag());
      newMusic.setCreatedAt(localDateTime);
      newMusic.setUpdatedAt(localDateTime);
      newMusic.setMissingAt(null);

      musicMapper.insert(newMusic);
      logger.info("insert music: " + music.getPath());

      musicId = getByPath(newMusic.getPath()).get().getMusicId();

    } else if (shouldUpdate) {
      Music currentMusic = maybeCurrentMusic.get();
      currentMusic.setPath(music.getPath());
      currentMusic.setEncoding(music.getEncoding());
      currentMusic.setTrackLength(music.getTrackLength());
      currentMusic.setFileHash(music.getFileHash());
      currentMusic.setTextTag(music.getTextTag());
      currentMusic.setUpdatedAt(localDateTime);
      currentMusic.setMissingAt(null);

      musicMapper.update(currentMusic);
      logger.info("update music: " + music.getPath());

      musicId = currentMusic.getMusicId();

    } else {
      musicMapper.updateMissingAt(maybeCurrentMusic.get().getMusicId(), null);
      logger.info("already managed: " + music.getPath());
      return;
    }

    if (shouldUpdateDetails || shouldInsert) {
      if (shouldUpdateDetails) {
        musicArtworkMapper.deleteByMusicId(musicId);
        musicTagMapper.deleteByMusicId(musicId);
      }

      // Artwork
      music.getArtworks().forEach((artwork) -> {
        String imageHash = artwork.getImageHash();
        Integer artworkId;
        Optional<Artwork> maybeArtwork = getArtworkByImageHash(imageHash);
        if (maybeArtwork.isPresent()) {
          artworkId = maybeArtwork.get().getArtworkId();

        } else {
          Artwork newArtwork = new Artwork();
          newArtwork.setMimetype(artwork.getMimetype());
          newArtwork.setImage(artwork.getImage());
          newArtwork.setImageHash(artwork.getImageHash());
          newArtwork.setTextTag(artwork.getTextTag());
          newArtwork.setCreatedAt(localDateTime);
          newArtwork.setUpdatedAt(localDateTime);

          artworkMapper.insert(newArtwork);
          artworkId = getArtworkByImageHash(imageHash).get().getArtworkId();
        }

        musicArtworkMapper.insert(musicId, artworkId);
      });

      // MusicTag
      Stream.iterate(0, i -> i + 1).limit(music.getMusicTags().size()).forEach((index) -> {
        MusicTag musicTag = music.getMusicTags().get(index);
        MusicTag newMusicTag = new MusicTag();
        newMusicTag.setMusicId(musicId);
        newMusicTag.setSeq(index);
        newMusicTag.setName(musicTag.getName());
        newMusicTag.setValue(musicTag.getValue());
        newMusicTag.setTextTag(musicTag.getTextTag());

        musicTagMapper.insert(newMusicTag);
      });
    } else {
      logger.info("skip update detail: " + music.getPath());

    }
  }

  @Transactional(readOnly = true)
  public Optional<Music> getByMusicIdIgnoreArtworks(Integer musicId) {
    return Optional.ofNullable(musicMapper.selectByMusicId(musicId)).map(this::fillMusicTags);
  }

  @Transactional(readOnly = true)
  public Optional<Music> getByMusicId(Integer musicId) {
    return Optional.ofNullable(musicMapper.selectByMusicId(musicId)).map(this::fillLists);
  }

  @Transactional(readOnly = true)
  public Optional<Music> getByPath(String path) {
    return Optional.ofNullable(musicMapper.selectByPath(path)).map(this::fillLists);
  }


  @Transactional
  public void setMissingAt(Integer musicId, LocalDateTime localDateTime) {
    musicMapper.updateMissingAt(musicId, localDateTime);
  }

  @Transactional(readOnly = true)
  public List<Music> getAllMusic() {
    return musicMapper.selectAll().stream().map(this::fillLists).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<Music> getAllLivingMusic() {
    return musicMapper.selectLivingAll().stream().map(this::fillLists).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<Music> getAllMissingMusic() {
    return musicMapper.selectMissingAll().stream().map(this::fillLists).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  private List<Music> getByFileHash(String fileHash) {
    return musicMapper.selectByFileHash(fileHash).stream().map(this::fillLists).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  private Optional<Artwork> getArtworkByImageHash(String imageHash) {
    return Optional.ofNullable(artworkMapper.selectByImageHash(imageHash));
  }

  @Transactional(readOnly = true)
  private Music fillLists(Music music) {
    fillArtworks(music);
    fillMusicTags(music);
    return music;
  }

  @Transactional(readOnly = true)
  private Music fillArtworks(Music music) {
    music.setArtworks(artworkMapper.selectByMusicId(music.getMusicId()));
    return music;
  }

  @Transactional(readOnly = true)
  private Music fillMusicTags(Music music) {
    music.setMusicTags(musicTagMapper.selectByMusicId(music.getMusicId()));
    return music;
  }

}
