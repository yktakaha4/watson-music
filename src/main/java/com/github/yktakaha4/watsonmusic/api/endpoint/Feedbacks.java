package com.github.yktakaha4.watsonmusic.api.endpoint;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.yktakaha4.watsonmusic.ApplicationException;
import com.github.yktakaha4.watsonmusic.api.dj.DiscJockey;
import com.github.yktakaha4.watsonmusic.api.dj.JockingTrack;
import com.github.yktakaha4.watsonmusic.api.entity.Feedback;
import com.github.yktakaha4.watsonmusic.api.entity.Succeed;
import com.github.yktakaha4.watsonmusic.model.Track;
import com.github.yktakaha4.watsonmusic.service.FeedbackService;

@RestController
@RequestMapping("/api/feedbacks")
public class Feedbacks {
  private final FeedbackService feedbackService;
  private final DiscJockey diskJockey;

  @Autowired
  public Feedbacks(FeedbackService feedbackService, DiscJockey discJockey) {
    this.feedbackService = feedbackService;
    this.diskJockey = discJockey;
  }

  @RequestMapping(method = RequestMethod.POST)
  public Succeed addFeedback(@RequestBody @Validated Feedback feedback,
      @RequestParam(name = "userId", required = true) String userId) {
    Optional<JockingTrack> maybeJockingTrack = diskJockey.getJockingTrack();
    if (maybeJockingTrack.isPresent()) {
      Track track = maybeJockingTrack.get().getTrack();
      com.github.yktakaha4.watsonmusic.model.Feedback feedbackModel = feedbackService
          .createFeedback(track.getTrackTag(), userId, feedback.getFeedbackType());
      feedbackService.addFeedback(feedbackModel);
    } else {
      throw new ApplicationException("track_unplayed");
    }
    return new Succeed();
  }
}
