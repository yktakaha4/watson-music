package com.github.yktakaha4.watsonmusic.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.github.yktakaha4.watsonmusic.model.Source;
import com.github.yktakaha4.watsonmusic.model.Text;

@Mapper
public interface TextMapper {
  List<Text> selectAll();

  Text selectByTextId(Integer textId);

  List<Text> selectBySource(Source source);

  Text selectBySourceAndTitle(@Param("source") Source source, @Param("title") String title);

  Integer count(Source source);

  void insert(Text text);

  void deleteAll();

}
