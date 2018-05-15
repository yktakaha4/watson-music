package com.github.yktakaha4.watsonmusic.mapper;

import java.util.List;

import com.github.yktakaha4.watsonmusic.model.DocumentSource;

public interface DocumentSourceMapper {
  List<DocumentSource> selectAll();

  List<DocumentSource> selectByMusicId(Integer musicId);

}
