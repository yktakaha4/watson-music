package com.github.yktakaha4.watsonmusic.batch;

import java.util.function.Function;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;

public class SimpleProcessor<I,O> implements ItemProcessor<I,O>, ItemStream {
  private Function<I, O> processor;

  public SimpleProcessor(Function<I, O> processor) {
    this.processor = processor;
  }

  @Override
  public O process(I item) throws Exception {
    return processor.apply(item);
  }

  @Override
  public void open(ExecutionContext executionContext) throws ItemStreamException {
  }

  @Override
  public void update(ExecutionContext executionContext) throws ItemStreamException {
  }

  @Override
  public void close() throws ItemStreamException {
    this.processor = null;
  }

}
