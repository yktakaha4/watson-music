package com.github.yktakaha4.watsonmusic.batch;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemWriter;

public class SimpleWriter<T> implements ItemWriter<T>, ItemStream {

  private Consumer<T> consumer;

  public SimpleWriter(Consumer<T> consumer) {
    this.consumer = consumer;
  }

  @Override
  public void write(List<? extends T> items) throws Exception {
    items.stream().forEach(consumer);
  }

  @Override
  public void open(ExecutionContext executionContext) throws ItemStreamException {
  }

  @Override
  public void update(ExecutionContext executionContext) throws ItemStreamException {
  }

  @Override
  public void close() throws ItemStreamException {
    this.consumer = null;
  }

}
