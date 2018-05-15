package com.github.yktakaha4.watsonmusic.batch.task;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.github.yktakaha4.watsonmusic.model.Document;
import com.github.yktakaha4.watsonmusic.service.DiscoveryDocumentService;
import com.github.yktakaha4.watsonmusic.service.DocumentService;
import com.github.yktakaha4.watsonmusic.service.MusicService;

@Controller
@StepScope
public class DocumentPublisher implements ItemWriter<Document> {
  private final DiscoveryDocumentService discoveryDocumentService;
  private final DocumentService documentService;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  public DocumentPublisher(MusicService musicService, DiscoveryDocumentService discoveryDocumentService,
      DocumentService documentService) {
    this.discoveryDocumentService = discoveryDocumentService;
    this.documentService = documentService;
  }

  @Override
  public void write(List<? extends Document> documents) throws Exception {
    documents.forEach(this::write);
  }

  private void write(Document document) {
    Consumer<Document> consumer = null;
    if (document.getOperationStatus().shouldAdd()) {
      consumer = discoveryDocumentService::addDocument;
    } else if (document.getOperationStatus().shouldUpdate()) {
      consumer = discoveryDocumentService::updateDocument;
    } else if (document.getOperationStatus().shouldDelete()) {
      consumer = discoveryDocumentService::deleteDocument;
    } else {
      logger.info("noop document: " + document.getDiscoveryDocId());
    }

    Optional.ofNullable(consumer).ifPresent((c) -> {
      c.accept(document);
      documentService.publish(document);
    });
  }

}
