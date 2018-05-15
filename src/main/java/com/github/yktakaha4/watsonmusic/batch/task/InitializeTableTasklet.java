package com.github.yktakaha4.watsonmusic.batch.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.github.yktakaha4.watsonmusic.service.MusicService;
import com.github.yktakaha4.watsonmusic.service.TextLinkService;
import com.github.yktakaha4.watsonmusic.service.TextService;
import com.github.yktakaha4.watsonmusic.service.TextTagService;

@Controller
@StepScope
public class InitializeTableTasklet implements Tasklet {
  private final MusicService musicService;
  private final TextService textService;
  private final TextLinkService textLinkService;
  private final TextTagService textTagService;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  public InitializeTableTasklet(MusicService musicService, TextService textService, TextLinkService textLinkService,
      TextTagService textTagService) {
    this.musicService = musicService;
    this.textService = textService;
    this.textLinkService = textLinkService;
    this.textTagService = textTagService;
  }

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
    musicService.missingAll();
    logger.info("set missing all music");

    textService.removeAll();
    textLinkService.removeAll();
    textTagService.removeAll();
    logger.info("remove all records from text, text_link, text_tag");

    return RepeatStatus.FINISHED;
  }

}
