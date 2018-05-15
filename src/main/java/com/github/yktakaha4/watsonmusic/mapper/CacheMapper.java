package com.github.yktakaha4.watsonmusic.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.github.yktakaha4.watsonmusic.model.Cache;
import com.github.yktakaha4.watsonmusic.model.Source;

@Mapper
public interface CacheMapper {

  Cache selectBySourceAndRequestKey(@Param("source") Source source, @Param("cacheKey") String cacheKey);

  void insert(Cache cache);

}
