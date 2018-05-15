package com.github.yktakaha4.watsonmusic.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.github.yktakaha4.watsonmusic.model.TextTag;

@Mapper
public interface TextTagMapper {
  TextTag select(@Param("textId") Integer textId, @Param("tag") String tag);

  List<TextTag> selectByTextId(Integer textId);

  List<TextTag> selectByTag(String tag);

  void insert(TextTag textTag);

  void deleteByTextId(Integer textId);

  void deleteByTag(String tag);

  void deleteAll();

}
