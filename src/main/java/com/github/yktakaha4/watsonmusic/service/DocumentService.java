package com.github.yktakaha4.watsonmusic.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.yktakaha4.watsonmusic.mapper.DocumentMapper;
import com.github.yktakaha4.watsonmusic.mapper.DocumentSourceMapper;
import com.github.yktakaha4.watsonmusic.model.Document;
import com.github.yktakaha4.watsonmusic.model.DocumentOperationStatus;
import com.github.yktakaha4.watsonmusic.model.DocumentSource;

@Service
public class DocumentService {
  private final DocumentMapper documentMapper;
  private final DocumentSourceMapper documentSourceMapper;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  public DocumentService(DocumentMapper documentMapper, DocumentSourceMapper documentSourceMapper) {
    this.documentMapper = documentMapper;
    this.documentSourceMapper = documentSourceMapper;
  }

  @Transactional(readOnly = true)
  public List<Document> getAllDocuments() {
    return documentMapper.selectAll();
  }

  @Transactional(readOnly = true)
  public List<Document> getPublishedDocuments() {
    return documentMapper.selectPublished();
  }

  @Transactional(readOnly = true)
  public List<DocumentSource> getDocumentSourcesByMusicId(Integer musicId) {
    return documentSourceMapper.selectByMusicId(musicId);
  }

  @Transactional(readOnly = true)
  public Optional<Document> getDocumentByMusicId(Integer musicId) {
    return Optional.ofNullable(documentMapper.selectByMusicId(musicId));
  }

  @Transactional
  public void publish(Document document) {
    if (document.getOperationStatus().isNoOp()) {
      Optional<Document> maybeCurrentDocument = Optional
          .ofNullable(documentMapper.selectByDocumentId(document.getDocumentId()));
      if (maybeCurrentDocument.isPresent()) {
        Document currentDocument = maybeCurrentDocument.get();
        switch (currentDocument.getOperationStatus()) {
        case SHOULD_ADD:
        case SHOULD_UPDATE:
          documentMapper.update(document);
          return;
        case SHOULD_DELETE:
          documentMapper.delete(document.getDocumentId());
          return;
        default:
          break;
        }
      }
    }
    logger.warn("inconsistent document: " + document.getDiscoveryDocId());
  }

  @Transactional
  public void manage(Document document) {
    final LocalDateTime localDateTime = LocalDateTime.now();
    boolean shouldInsert = false;
    boolean shouldUpdate = false;

    Optional<Document> maybeCurrentDocument = getDocumentByMusicId(document.getMusicId());
    Document currentDocument = null;
    if (maybeCurrentDocument.isPresent()) {
      // 既存ドキュメント
      currentDocument = maybeCurrentDocument.get();
      if (!StringUtils.equalsIgnoreCase(document.getContentHash(), currentDocument.getContentHash())) {
        // 内容に更新あり
        shouldUpdate = true;
      }
    } else {
      // 新規ドキュメント
      shouldInsert = true;
    }

    if (shouldInsert || shouldUpdate) {
      Document newDocument = new Document();
      newDocument.setMusicId(document.getMusicId());
      newDocument.setContent(document.getContent());
      newDocument.setContentHash(document.getContentHash());
      if (shouldInsert) {
        // 新規
        newDocument.setOperationStatus(DocumentOperationStatus.SHOULD_ADD);
        newDocument.setCreatedAt(localDateTime);
        newDocument.setUpdatedAt(localDateTime);

        documentMapper.insert(newDocument);
        logger.info("insert: documentId=" + documentMapper.selectByMusicId(newDocument.getMusicId()).getDocumentId());
      } else if (shouldUpdate) {
        // 更新
        newDocument.setDocumentId(currentDocument.getDocumentId());
        newDocument.setDiscoveryDocId(currentDocument.getDiscoveryDocId());
        newDocument.setPublishedAt(currentDocument.getPublishedAt());
        newDocument.setOperationStatus(DocumentOperationStatus.SHOULD_UPDATE);
        newDocument.setCreatedAt(currentDocument.getCreatedAt());
        newDocument.setUpdatedAt(localDateTime);

        documentMapper.update(newDocument);
        logger.info("update: documentId=" + newDocument.getDocumentId());
      }
    } else {
      // 新規でも更新でもない
      logger.info("already managed: documentId=" + currentDocument.getDocumentId());
    }
  }

  @Transactional
  public void markShouldDeleteByMusicId(Integer musicId) {
    documentMapper.updateOperationStatusByMusicId(musicId, DocumentOperationStatus.SHOULD_DELETE);
  }

}
