package com.github.yktakaha4.watsonmusic.batch.task;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.github.yktakaha4.watsonmusic.model.Document;
import com.github.yktakaha4.watsonmusic.model.Music;
import com.github.yktakaha4.watsonmusic.service.DocumentService;
import com.github.yktakaha4.watsonmusic.service.MusicService;

@Controller
@StepScope
public class DocumentMarkShouldDeleteTasklet implements Tasklet {
  private final MusicService musicService;
  private final DocumentService documentService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  public DocumentMarkShouldDeleteTasklet(MusicService musicService, DocumentService documentService) {
    this.musicService = musicService;
    this.documentService = documentService;
  }

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
    for (Music music : musicService.getAllMissingMusic()) {
      Optional<Document> maybeDocument = documentService.getDocumentByMusicId(music.getMusicId());
      if (maybeDocument.isPresent()) {
        documentService.markShouldDeleteByMusicId(music.getMusicId());
        logger.info("delete: documentId=" + maybeDocument.get().getDocumentId());
      }
    }
    return RepeatStatus.FINISHED;
  }

}
