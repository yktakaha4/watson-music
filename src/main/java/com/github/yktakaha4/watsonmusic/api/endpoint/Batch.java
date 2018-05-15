package com.github.yktakaha4.watsonmusic.api.endpoint;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.github.yktakaha4.watsonmusic.api.entity.BatchStatus;
import com.github.yktakaha4.watsonmusic.batch.LearnFeedbacksBatchConfiguration;
import com.github.yktakaha4.watsonmusic.batch.UpdateMusicBatchConfiguration;

@RestController
@RequestMapping("/api/batch")
public class Batch {

  private final JobLauncher jobLauncher;
  private final UpdateMusicBatchConfiguration updateMusicBatchConfiguration;
  private final LearnFeedbacksBatchConfiguration learnFeedbacksBatchConfiguration;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private Optional<JobExecution> updateMusicJobExecution = Optional.empty();
  private Optional<JobExecution> learnFeedbacksJobExecution = Optional.empty();

  @Autowired
  public Batch(JobLauncher jobLauncher, UpdateMusicBatchConfiguration updateMusicBatchConfiguration,
      LearnFeedbacksBatchConfiguration learnFeedbacksBatchConfiguration) {
    this.jobLauncher = jobLauncher;
    this.updateMusicBatchConfiguration = updateMusicBatchConfiguration;
    this.learnFeedbacksBatchConfiguration = learnFeedbacksBatchConfiguration;
  }

  @RequestMapping(path = "/status", method = RequestMethod.GET)
  public synchronized BatchStatus checkStatus() {
    BatchStatus batchStatus = new BatchStatus();
    batchStatus.setUpdateMusicMessage(getMessage(updateMusicJobExecution));
    batchStatus.setLearnFeedbacksMessage(getMessage(learnFeedbacksJobExecution));
    return batchStatus;
  }

  @RequestMapping(path = "/update-music", method = RequestMethod.POST)
  public synchronized BatchStatus updateMusic() throws Exception {
    try {
      if (updateMusicJobExecution.isPresent()) {
        JobExecution jobExecution = updateMusicJobExecution.get();
        if (jobExecution.isRunning()) {
          return checkStatus();
        }
      }

      JobExecution jobExecution = jobLauncher.run(updateMusicBatchConfiguration.main(LocalDateTime.now()),
          new JobParameters());
      updateMusicJobExecution = Optional.of(jobExecution);
      logger.info("start update music job.");
      return checkStatus();
    } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
        | JobParametersInvalidException e) {
      updateMusicJobExecution = Optional.empty();
      logger.error("failed to start update music job...", e);
      throw e;
    }
  }

  @RequestMapping(path = "/learn-feedbacks", method = RequestMethod.POST)
  public synchronized BatchStatus learningFeedbacks() throws Exception {
    try {
      if (learnFeedbacksJobExecution.isPresent()) {
        JobExecution jobExecution = learnFeedbacksJobExecution.get();
        if (jobExecution.isRunning()) {
          return checkStatus();
        }
      }

      JobExecution jobExecution = jobLauncher.run(learnFeedbacksBatchConfiguration.main(LocalDateTime.now()),
          new JobParameters());
      learnFeedbacksJobExecution = Optional.of(jobExecution);
      logger.info("start learn feedbacks job.");
      return checkStatus();
    } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
        | JobParametersInvalidException e) {
      learnFeedbacksJobExecution = Optional.empty();
      logger.error("failed to start learn feedbacks job...", e);
      throw e;
    }
  }

  private synchronized String getMessage(Optional<JobExecution> maybeJobExecution) {
    try {
      if (maybeJobExecution.isPresent()) {
        JobExecution jobExecution = maybeJobExecution.get();
        if (jobExecution.isRunning()) {
          return "Running.";
        } else if (jobExecution.getStatus().equals(org.springframework.batch.core.BatchStatus.COMPLETED)) {
          return "Completed.";
        } else {
          return "Unknown.";
        }
      }
      return "Unset.";
    } catch (Exception e) {
      logger.error("failed get message.", e);

      return "Server Error.";
    }
  }

}
