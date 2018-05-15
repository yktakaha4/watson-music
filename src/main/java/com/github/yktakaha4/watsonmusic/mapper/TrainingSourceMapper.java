package com.github.yktakaha4.watsonmusic.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.github.yktakaha4.watsonmusic.model.TrainingSource;

@Mapper
public interface TrainingSourceMapper {

  public List<TrainingSource> selectAll();

}
