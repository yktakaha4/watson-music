package com.github.yktakaha4.watsonmusic.batch.task;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import com.github.yktakaha4.watsonmusic.model.Feedback;
import com.github.yktakaha4.watsonmusic.model.TrainingSource;
import com.github.yktakaha4.watsonmusic.service.DiscoveryLearnService;
import com.github.yktakaha4.watsonmusic.service.FeedbackService;

@Controller
@StepScope
public class LearningTasklet implements Tasklet {
  private final FeedbackService feedbackService;
  private final DiscoveryLearnService discoveryLearnService;

  @Autowired
  public LearningTasklet(FeedbackService feedbackService, DiscoveryLearnService discoveryLearnService) {
    this.feedbackService = feedbackService;
    this.discoveryLearnService = discoveryLearnService;
  }

  @Override
  @Transactional
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
    List<Feedback> feedbacks = feedbackService.getFeedbacks();

    discoveryLearnService.deleteAllLearingData();
    List<TrainingSource> trainingSources = feedbackService.getTrainingSources();
    discoveryLearnService.learn(trainingSources);

    LocalDateTime feedbackedAt = LocalDateTime.now();
    feedbacks.forEach(feedback -> {
      feedbackService.markFeedbackedByTag(feedback.getFeedbackTag(), feedbackedAt);
    });

    return RepeatStatus.FINISHED;
  }

}
