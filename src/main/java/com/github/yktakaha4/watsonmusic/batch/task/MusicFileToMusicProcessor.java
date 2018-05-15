package com.github.yktakaha4.watsonmusic.batch.task;

import java.io.File;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.github.yktakaha4.watsonmusic.model.Music;
import com.github.yktakaha4.watsonmusic.service.MusicFileService;

@Controller
@StepScope
public class MusicFileToMusicProcessor implements ItemProcessor<File, Music> {
  private final MusicFileService musicFileService;

  @Autowired
  public MusicFileToMusicProcessor(MusicFileService musicFileService) {
    this.musicFileService = musicFileService;
  }

  @Override
  public Music process(File file) throws Exception {
    return musicFileService.createMusic(file).orElse(null);
  }

}
