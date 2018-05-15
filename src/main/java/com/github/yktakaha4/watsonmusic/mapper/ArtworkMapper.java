package com.github.yktakaha4.watsonmusic.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.github.yktakaha4.watsonmusic.model.Artwork;

@Mapper
public interface ArtworkMapper {
  List<Artwork> selectAll();

  List<Artwork> selectByMusicId(Integer musicId);

  Artwork selectByImageHash(String imageHash);

  void insert(Artwork artwork);

}
