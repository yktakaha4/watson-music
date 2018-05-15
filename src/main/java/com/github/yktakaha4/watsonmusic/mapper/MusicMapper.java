package com.github.yktakaha4.watsonmusic.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.github.yktakaha4.watsonmusic.model.Music;

@Mapper
public interface MusicMapper {
  List<Music> selectAll();

  List<Music> selectLivingAll();

  List<Music> selectMissingAll();

  Music selectByMusicId(Integer musicId);

  Music selectByPath(String path);

  List<Music> selectByFileHash(String fileHash);

  Integer selectLivingCount();

  void insert(Music music);

  void update(Music music);

  void updateMissingAtAll(LocalDateTime missingAt);

  void updateMissingAt(@Param("musicId") Integer musicId, @Param("missingAt") LocalDateTime missingAt);

}
