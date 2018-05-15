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

import com.github.yktakaha4.watsonmusic.model.Music;
import com.github.yktakaha4.watsonmusic.service.MusicService;

@Controller
@StepScope
public class LivingMusicReader implements ItemReader<Music>, ItemStream {
  private final MusicService musicService;

  private Iterator<Music> iterator;

  @Autowired
  public LivingMusicReader(MusicService musicService) {
    this.musicService = musicService;
  }

  @Override
  public Music read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
    return iterator.hasNext() ? iterator.next() : null;
  }

  @Override
  public void open(ExecutionContext executionContext) throws ItemStreamException {
    iterator = musicService.getAllLivingMusic().iterator();
  }

  @Override
  public void update(ExecutionContext executionContext) throws ItemStreamException {
  }

  @Override
  public void close() throws ItemStreamException {
    iterator = null;
  }

}
