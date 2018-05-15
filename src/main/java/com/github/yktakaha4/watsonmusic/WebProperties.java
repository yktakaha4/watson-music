package com.github.yktakaha4.watsonmusic;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@PropertySource("classpath:watsonmusic.properties")
@ConfigurationProperties(prefix = "web")
@Data
public class WebProperties {
  private String wikipediaTarget;
  private String lyricsSearchTarget;
  private String lyricsTarget;

  private String visualRecognitionCredentials;
  private String visualRecognitionLanguage;

  private String discoveryCredentials;
  private String discoveryEnvironmentId;
  private String discoveryCollectionId;
  private Integer discoveryQueryCount;

  private Integer connectionTimeout;
  private Integer requestInterval;
  private Integer maxRetryCount;

  private Integer playedAtHour;
  private Integer similarDocumentsCount;
  private Integer ignoreDocumentsCount;

  private String socketTopicActions;

  private String mqttServerUrl;
  private String mqttServerChannelResource;
  private String mqttServerChannelToken;
}
