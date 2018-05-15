package com.github.yktakaha4.watsonmusic;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@PropertySource(value = "classpath:watsonmusic.properties", encoding = "UTF-8")
@ConfigurationProperties(prefix = "batch")
@Data
public class BatchProperties {
  private String musicfilePath;
  private String musicfilePathSeparator;
  private String musicfileExtensions;

  private Boolean shouldOutputDocumentFiles;
  private String documentFilePath;

  private String characterEncoderCharsets;

  private String userDictionaryFilePath;
  private String userDictionarySourcesPath;
  private String stopWordsDictionaryFilePath;
  private String enrichingMusicTags;

  private Integer bigChunk;
  private Integer mediumChunk;
  private Integer smallChunk;

}
