package com.github.yktakaha4.watsonmusic.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

public class JobLogger implements JobExecutionListener {

  private static final String FORMAT = "##### %s job %s #####";

  private Logger logger = LoggerFactory.getLogger(JobLogger.class);

  @Override
  public void beforeJob(JobExecution jobExecution) {
    logger.info(String.format(FORMAT, "start", jobExecution.getJobInstance().getJobName()));
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    logger.info(String.format(FORMAT, "end", jobExecution.getJobInstance().getJobName()));
  }

}
