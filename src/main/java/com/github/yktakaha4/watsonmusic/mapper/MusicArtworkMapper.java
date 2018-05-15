package com.github.yktakaha4.watsonmusic.mapper;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

@Component
public interface MusicArtworkMapper {

  void insert(@Param("musicId") Integer musicId, @Param("artworkId") Integer artworkId);

  void deleteByMusicId(Integer musicId);

}
