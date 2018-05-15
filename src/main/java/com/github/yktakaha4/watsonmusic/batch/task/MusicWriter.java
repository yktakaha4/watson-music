package com.github.yktakaha4.watsonmusic.batch.task;

import java.util.List;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.github.yktakaha4.watsonmusic.model.Music;
import com.github.yktakaha4.watsonmusic.service.MusicService;

@Controller
@StepScope
public class MusicWriter implements ItemWriter<Music> {
  private final MusicService musicService;

  @Autowired
  public MusicWriter(MusicService musicService) {
    this.musicService = musicService;
  }

  @Override
  public void write(List<? extends Music> musics) throws Exception {
    musics.forEach(musicService::manage);
  }

}
