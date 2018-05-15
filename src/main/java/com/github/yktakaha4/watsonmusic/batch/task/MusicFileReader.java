package com.github.yktakaha4.watsonmusic.batch.task;

import java.io.File;
import java.nio.file.Paths;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
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

import com.github.yktakaha4.watsonmusic.BatchProperties;

@Controller
@StepScope
public class MusicFileReader implements ItemReader<File>, ItemStream {
  private final BatchProperties batchProperties;

  private Iterator<File> iterator;

  @Autowired
  public MusicFileReader(BatchProperties batchProperties) {
    this.batchProperties = batchProperties;
  }

  @Override
  public File read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
    return iterator.hasNext() ? iterator.next() : null;
  }

  @Override
  public void open(ExecutionContext executionContext) throws ItemStreamException {
    File file = Paths.get(batchProperties.getMusicfilePath()).toFile();
    String[] extensions = batchProperties.getMusicfileExtensions().split(",");

    iterator = FileUtils.iterateFiles(file, extensions, true);
  }

  @Override
  public void update(ExecutionContext executionContext) throws ItemStreamException {
  }

  @Override
  public void close() throws ItemStreamException {
    iterator = null;
  }

}
