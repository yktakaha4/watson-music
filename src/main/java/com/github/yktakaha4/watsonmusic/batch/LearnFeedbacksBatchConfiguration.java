package com.github.yktakaha4.watsonmusic.batch;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.github.yktakaha4.watsonmusic.batch.task.LearningTasklet;

@Configuration
@EnableBatchProcessing
public class LearnFeedbacksBatchConfiguration {
  @Autowired
  private JobBuilderFactory jobBuilderFactory;
  @Autowired
  private StepBuilderFactory stepBuilderFactory;

  @Autowired
  private LearningTasklet learningTasklet;

  public Job main(LocalDateTime localDateTime) {
    return jobBuilderFactory.get("learning-" + localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .incrementer(new RunIdIncrementer())
        .listener(new JobLogger())
        .flow(learning())
        .end()
        .build();
  }

  public Step learning() {
    return stepBuilderFactory.get("learning")
        .tasklet(learningTasklet)
        .build();
  }

}
