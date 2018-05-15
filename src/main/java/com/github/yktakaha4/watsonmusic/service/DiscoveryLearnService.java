package com.github.yktakaha4.watsonmusic.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.yktakaha4.watsonmusic.ApplicationException;
import com.github.yktakaha4.watsonmusic.WebProperties;
import com.github.yktakaha4.watsonmusic.model.Source;
import com.github.yktakaha4.watsonmusic.model.TrainingSource;
import com.github.yktakaha4.watsonmusic.util.PathUtils;
import com.github.yktakaha4.watsonmusic.util.RequestInterval;
import com.ibm.watson.developer_cloud.discovery.v1.Discovery;
import com.ibm.watson.developer_cloud.discovery.v1.model.AddTrainingDataOptions;
import com.ibm.watson.developer_cloud.discovery.v1.model.DeleteAllTrainingDataOptions;
import com.ibm.watson.developer_cloud.discovery.v1.model.TrainingExample;
import com.ibm.watson.developer_cloud.discovery.v1.model.TrainingQuery;
import com.ibm.watson.developer_cloud.service.exception.InternalServerErrorException;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@Service
public class DiscoveryLearnService {
  private final RequestInterval requestInterval;

  private final WebProperties webProperties;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final String url;
  private final String userName;
  private final String password;

  @Autowired
  public DiscoveryLearnService(WebProperties webProperties,
      PathUtils pathUtils, RequestInterval requestInterval) {
    this.webProperties = webProperties;
    this.requestInterval = requestInterval;
    try {
      Path path = pathUtils.fromWebProps(wp -> wp.getDiscoveryCredentials());
      JSONObject credentials = new JSONObject(String.join("", Files.readAllLines(path)));
      this.url = credentials.getString("url");
      this.userName = credentials.getString("username");
      this.password = credentials.getString("password");
    } catch (JSONException | IOException e) {
      logger.error("credentials file error: " + webProperties.getDiscoveryCredentials());
      throw new ApplicationException(e);
    }
  }

  public void deleteAllLearingData() {
    Discovery discovery = createDiscovery();
    DeleteAllTrainingDataOptions deleteAllTrainingDataOptions = new DeleteAllTrainingDataOptions.Builder(
        webProperties.getDiscoveryEnvironmentId(), webProperties.getDiscoveryCollectionId()).build();

    Failsafe.with(createRetryPolicy()).run(() -> {
      requestInterval.takeInterval(Source.DISCOVERY);
      discovery.deleteAllTrainingData(deleteAllTrainingDataOptions).execute();
    });
    logger.info("delete all training data");
  }

  public void learn(List<TrainingSource> trainingSources) {
    List<AddTrainingDataOptions> addTrainingDataOptionsList = trainingSources.stream()
        .collect(Collectors.groupingBy(data -> data.getQuery())).entrySet().stream().sorted((l, r) -> {
          return l.getKey().compareTo(r.getKey());
        }).map(entry -> {
          List<TrainingExample> trainingExamples = entry.getValue().stream().map(trainingData -> {
            TrainingExample trainingExample = new TrainingExample();
            trainingExample.setDocumentId(trainingData.getDocumentId());
            trainingExample.setCrossReference(trainingData.getMusicId().toString());
            trainingExample.setRelevance(trainingData.getRelevance());
            return trainingExample;
          }).collect(Collectors.toList());
          return new AddTrainingDataOptions.Builder(webProperties.getDiscoveryEnvironmentId(),
              webProperties.getDiscoveryCollectionId())
                  .naturalLanguageQuery(entry.getKey())
                  .examples(trainingExamples)
                  .build();
        }).collect(Collectors.toList());
    logger.info("training data size: " + addTrainingDataOptionsList.size());

    Discovery discovery = createDiscovery();
    for (AddTrainingDataOptions addTrainingDataOptions : addTrainingDataOptionsList) {
      TrainingQuery trainingQuery = Failsafe.with(createRetryPolicy()).get(() -> {
        requestInterval.takeInterval(Source.DISCOVERY);
        return discovery.addTrainingData(addTrainingDataOptions).execute();
      });
      logger.info("succeed to training: query=" + trainingQuery.getNaturalLanguageQuery() + ", documents="
          + trainingQuery.getExamples().size());
    }
    logger.info("trainig successful.");
  }

  private Discovery createDiscovery() {
    Discovery discovery = new Discovery(Discovery.VERSION_DATE_2017_11_07);
    discovery.setEndPoint(url);
    discovery.setUsernameAndPassword(userName, password);
    return discovery;
  }

  private RetryPolicy createRetryPolicy() {
    return new RetryPolicy()
        .retryOn((InternalServerErrorException ex) -> ex instanceof InternalServerErrorException)
        .withDelay(webProperties.getRequestInterval(), TimeUnit.SECONDS)
        .withMaxRetries(webProperties.getMaxRetryCount());
  }

}
