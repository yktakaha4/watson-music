package com.github.yktakaha4.watsonmusic.mapper;

import java.util.Collection;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.github.yktakaha4.watsonmusic.model.MusicTag;

@Mapper
public interface MusicTagMapper {
  List<MusicTag> selectAll();

  List<MusicTag> selectByMusicId(Integer musicId);

  List<MusicTag> selectByNamesIn(Collection<String> names);

  List<MusicTag> selectByNameAndValue(@Param("name") String name, @Param("value") String value);

  void insert(MusicTag musicTag);

  void deleteByMusicId(Integer musicId);

}
