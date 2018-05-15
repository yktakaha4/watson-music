package com.github.yktakaha4.watsonmusic.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.github.yktakaha4.watsonmusic.model.Source;
import com.github.yktakaha4.watsonmusic.model.TextLink;

@Mapper
public interface TextLinkMapper {
  List<TextLink> selectByTextId(Integer textId);

  List<TextLink> selectByTitle(String title);

  Integer countTextOfContainsTitle(@Param("source") Source source ,@Param("title") String title);

  void insert(TextLink textLink);

  void deleteByTextId(Integer textId);

  void deleteAll();

}
