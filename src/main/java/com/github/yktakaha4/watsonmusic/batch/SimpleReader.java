package com.github.yktakaha4.watsonmusic.batch;

import java.util.Iterator;
import java.util.function.Supplier;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

public class SimpleReader<T> implements ItemReader<T>, ItemStream {

  private Supplier<Iterator<T>> supplier;
  private Iterator<T> iterator;

  public SimpleReader(Supplier<Iterator<T>> supplier) {
    this.supplier = supplier;
  }

  @Override
  public T read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
    if (iterator.hasNext()) {
      return iterator.next();
    } else {
      return null;
    }
  }

  @Override
  public void open(ExecutionContext executionContext) throws ItemStreamException {
    iterator = supplier.get();
    supplier = null;
  }

  @Override
  public void update(ExecutionContext executionContext) throws ItemStreamException {
  }

  @Override
  public void close() throws ItemStreamException {
    iterator = null;
    supplier = null;
  }

}
