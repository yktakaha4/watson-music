package com.github.yktakaha4.watsonmusic.mapper;

import java.util.List;

import javax.ws.rs.QueryParam;

import org.apache.ibatis.annotations.Mapper;

import com.github.yktakaha4.watsonmusic.model.Document;
import com.github.yktakaha4.watsonmusic.model.DocumentOperationStatus;

@Mapper
public interface DocumentMapper {
  List<Document> selectAll();

  List<Document> selectPublished();

  Document selectByDocumentId(Integer documentId);

  Document selectByMusicId(Integer musicId);

  void insert(Document document);

  void update(Document document);

  void delete(Integer documentId);

  void updateOperationStatusByMusicId(@QueryParam("musicId") Integer musicId,
      @QueryParam("operationStatus") DocumentOperationStatus documentOperationStatus);
}
