package com.github.yktakaha4.watsonmusic.batch.task;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.github.yktakaha4.watsonmusic.BatchProperties;
import com.github.yktakaha4.watsonmusic.model.Document;
import com.github.yktakaha4.watsonmusic.service.DocumentService;

@Controller
@StepScope
public class DocumentWriter implements ItemWriter<Document> {
  private final DocumentService documentService;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final Boolean shouldOutputDocumentFiles;
  private final String documentFilePath;

  @Autowired
  public DocumentWriter(BatchProperties batchProperties, DocumentService documentService) {
    this.documentService = documentService;
    this.shouldOutputDocumentFiles = batchProperties.getShouldOutputDocumentFiles();
    this.documentFilePath = batchProperties.getDocumentFilePath();
  }

  @Override
  public void write(List<? extends Document> documents) throws Exception {
    documents.forEach(documentService::manage);
    if (shouldOutputDocumentFiles) {
      for (Document document : documents) {
        Files.write(Paths.get(documentFilePath, String.format("docid_%s.json", document.getDocumentId())),
            document.getContent().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      }
      logger.info("export document files: " + documents.size() + " files to " + documentFilePath);
    }
  }

}
