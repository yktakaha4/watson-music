package com.github.yktakaha4.watsonmusic.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.yktakaha4.watsonmusic.mapper.FeedbackMapper;
import com.github.yktakaha4.watsonmusic.mapper.TrainingSourceMapper;
import com.github.yktakaha4.watsonmusic.model.Feedback;
import com.github.yktakaha4.watsonmusic.model.FeedbackType;
import com.github.yktakaha4.watsonmusic.model.TrainingSource;
import com.github.yktakaha4.watsonmusic.util.IdentifierUtils;

@Service
public class FeedbackService {
  private final FeedbackMapper feedbackMapper;
  private final TrainingSourceMapper trainingSourceMapper;
  private final IdentifierUtils identifierUtils;

  public FeedbackService(FeedbackMapper feedbackMapper, TrainingSourceMapper trainingSourceMapper,
      IdentifierUtils identifierUtils) {
    this.feedbackMapper = feedbackMapper;
    this.trainingSourceMapper = trainingSourceMapper;
    this.identifierUtils = identifierUtils;
  }

  @Transactional(readOnly = true)
  public List<TrainingSource> getTrainingSources() {
    return trainingSourceMapper.selectAll();
  }

  @Transactional
  public List<Feedback> getFeedbacks() {
    return feedbackMapper.selectAll();
  }

  @Transactional
  public void addFeedback(Feedback feedback) {
    feedbackMapper.insert(feedback);
  }

  @Transactional
  public void markFeedbackedByTag(String feedbackTag, LocalDateTime feedbackedAt) {
    feedbackMapper.updateFeedbackedAtByTag(feedbackTag, feedbackedAt);
  }

  public Feedback createFeedback(String trackTag, String userTag, FeedbackType feedbackType) {
    Feedback feedback = new Feedback();
    feedback.setFeedbackTag(identifierUtils.newTag());
    feedback.setFeedbackType(feedbackType);
    feedback.setTrackTag(trackTag);
    feedback.setUserTag(userTag);
    feedback.setCreatedAt(LocalDateTime.now());
    return feedback;
  }

}
