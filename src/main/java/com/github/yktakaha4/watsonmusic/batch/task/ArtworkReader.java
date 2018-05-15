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

import com.github.yktakaha4.watsonmusic.model.Artwork;
import com.github.yktakaha4.watsonmusic.service.ArtworkService;

@Controller
@StepScope
public class ArtworkReader implements ItemReader<Artwork>, ItemStream {
  private final ArtworkService artworkService;

  private Iterator<Artwork> iterator;

  @Autowired
  public ArtworkReader(ArtworkService artworkService) {
    this.artworkService = artworkService;
  }

  @Override
  public Artwork read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
    return iterator.hasNext() ? iterator.next() : null;
  }

  @Override
  public void open(ExecutionContext executionContext) throws ItemStreamException {
    iterator = artworkService.getAllArtworks().iterator();
  }

  @Override
  public void update(ExecutionContext executionContext) throws ItemStreamException {
  }

  @Override
  public void close() throws ItemStreamException {
    iterator = null;
  }

}
