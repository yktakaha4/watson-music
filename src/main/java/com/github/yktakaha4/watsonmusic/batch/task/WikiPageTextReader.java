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

import com.github.yktakaha4.watsonmusic.model.Source;
import com.github.yktakaha4.watsonmusic.model.Text;
import com.github.yktakaha4.watsonmusic.service.TextService;

@Controller
@StepScope
public class WikiPageTextReader implements ItemReader<Text>, ItemStream {
  private final TextService textService;

  private Iterator<Text> iterator;

  @Autowired
  public WikiPageTextReader(TextService textService) {
    this.textService = textService;
  }

  @Override
  public Text read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
    return iterator.hasNext() ? iterator.next() : null;
  }

  @Override
  public void open(ExecutionContext executionContext) throws ItemStreamException {
    iterator = textService.getTextsBySource(Source.WIKIPEDIA).iterator();
  }

  @Override
  public void update(ExecutionContext executionContext) throws ItemStreamException {
  }

  @Override
  public void close() throws ItemStreamException {
    iterator = null;
  }

}
