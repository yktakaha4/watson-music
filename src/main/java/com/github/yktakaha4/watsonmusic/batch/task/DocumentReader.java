package com.github.yktakaha4.watsonmusic.batch.task;

import java.util.Iterator;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.github.yktakaha4.watsonmusic.model.Document;
import com.github.yktakaha4.watsonmusic.service.DocumentService;

@Controller
@StepScope
public class DocumentReader implements ItemReader<Document>, ItemStream {
  private final DocumentService documentService;

  private Iterator<Document> iterator;

  @Autowired
  public DocumentReader(DocumentService documentService) {
    this.documentService = documentService;
  }

  @Override
  public Document read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
    return iterator.hasNext() ? iterator.next() : null;
  }

  @Override
  public void open(ExecutionContext executionContext) throws ItemStreamException {
    iterator = documentService.getAllDocuments().iterator();
  }

  @Override
  public void update(ExecutionContext executionContext) throws ItemStreamException {
  }

  @Override
  public void close() throws ItemStreamException {
    iterator = null;
  }

}
