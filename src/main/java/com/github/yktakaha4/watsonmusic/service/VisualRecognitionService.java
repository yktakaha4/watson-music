package com.github.yktakaha4.watsonmusic.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.github.yktakaha4.watsonmusic.ApplicationException;
import com.github.yktakaha4.watsonmusic.WebProperties;
import com.github.yktakaha4.watsonmusic.mapper.CacheMapper;
import com.github.yktakaha4.watsonmusic.model.Cache;
import com.github.yktakaha4.watsonmusic.model.Source;
import com.github.yktakaha4.watsonmusic.model.VisualRecognitionResult;
import com.github.yktakaha4.watsonmusic.util.IdentifierUtils;
import com.github.yktakaha4.watsonmusic.util.PathUtils;
import com.github.yktakaha4.watsonmusic.util.RequestInterval;
import com.ibm.watson.developer_cloud.service.exception.InternalServerErrorException;
import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassResult;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifiedImages;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyOptions;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@Service
public class VisualRecognitionService {
  private final WebProperties webProperties;
  private final CacheMapper cacheMapper;
  private final IdentifierUtils identifierUtils;
  private final RequestInterval requestInterval;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final String apiKey;

  @Autowired
  public VisualRecognitionService(WebProperties webProperties, CacheMapper cacheMapper, IdentifierUtils identifierUtils,
      PathUtils pathUtils, RequestInterval requestInterval) {
    this.webProperties = webProperties;
    this.cacheMapper = cacheMapper;
    this.identifierUtils = identifierUtils;
    this.requestInterval = requestInterval;

    try {
      Path path = pathUtils.fromWebProps(wp -> wp.getVisualRecognitionCredentials());
      JSONObject credentials = new JSONObject(String.join("", Files.readAllLines(path)));
      this.apiKey = credentials.getString("api_key");
    } catch (JSONException | IOException e) {
      logger.error("credentials file error: " + webProperties.getVisualRecognitionCredentials());
      throw new ApplicationException(e);
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Optional<VisualRecognitionResult> getVisualRecognitionResult(byte[] image, String mimetype) {
    String imageHash = identifierUtils.toHash(image);
    Optional<Cache> maybeCache = Optional
        .ofNullable(cacheMapper.selectBySourceAndRequestKey(Source.VISUAL_RECOGNITION, imageHash));
    Cache cache;
    if (maybeCache.isPresent()) {
      cache = maybeCache.get();
    } else {
      cacheMapper.insert(createCache(imageHash, image, mimetype));
      logger.info("cached visual recognition result: " + imageHash);
      cache = cacheMapper.selectBySourceAndRequestKey(Source.VISUAL_RECOGNITION, imageHash);
    }
    return createVisualRecognitionResult(cache, imageHash);
  }

  private Cache createCache(String imageHash, byte[] image, String mimetype) {
    VisualRecognition visualRecognition = new VisualRecognition(VisualRecognition.VERSION_DATE_2016_05_20);
    visualRecognition.setApiKey(apiKey);

    ClassifiedImages result;
    try (InputStream inputStream = new ByteArrayInputStream(image)) {
      JSONObject parameters = new JSONObject();
      parameters.put("threshold", 0.0);

      ClassifyOptions classifyOptions = new ClassifyOptions.Builder()
          .acceptLanguage(webProperties.getVisualRecognitionLanguage())
          .imagesFilename(imageHash)
          .imagesFileContentType(mimetype)
          .imagesFile(inputStream)
          .parameters(parameters.toString())
          .build();

      RetryPolicy retryPolicy = new RetryPolicy()
          .retryOn((InternalServerErrorException ex) -> ex instanceof InternalServerErrorException)
          .withDelay(webProperties.getRequestInterval(), TimeUnit.SECONDS)
          .withMaxRetries(webProperties.getMaxRetryCount());

      result = Failsafe.with(retryPolicy).get(() -> {
        requestInterval.takeInterval(Source.VISUAL_RECOGNITION);
        return visualRecognition.classify(classifyOptions).execute();
      });
    } catch (IOException | JSONException e1) {
      logger.error("failed visual recognition: " + imageHash);
      throw new ApplicationException(e1);
    }

    Cache cache = new Cache();
    cache.setCacheKey(imageHash);
    cache.setSource(Source.VISUAL_RECOGNITION);
    cache.setRequestAt(LocalDateTime.now());

    String resultString = result.toString();
    try {
      JSONObject jsonObject = new JSONObject(resultString.replaceAll("\\r|\\n", ""));
      cache.setResponse(jsonObject.toString());

    } catch (JSONException e) {
      logger.warn("json parse error: " + cache.getCacheId());
      e.printStackTrace();
      cache.setResponse(resultString);

    }

    return cache;
  }

  private Optional<VisualRecognitionResult> createVisualRecognitionResult(Cache cache, String imageTitle) {
    VisualRecognitionResult visualRecognitionResult = new VisualRecognitionResult();
    visualRecognitionResult.setImageTitle(imageTitle);
    visualRecognitionResult.setImageHash(cache.getCacheKey());

    List<ClassResult> classResults = new ArrayList<>();
    try {
      JSONObject jsonObject = new JSONObject(cache.getResponse());
      if (!jsonObject.has("images")) {
        logger.warn("visual recognition result unfound: " + imageTitle);
        return Optional.empty();
      }
      JSONArray classesJson = jsonObject
          .getJSONArray("images").getJSONObject(0)
          .getJSONArray("classifiers").getJSONObject(0)
          .getJSONArray("classes");
      for (int index = 0; index < classesJson.length(); index++) {
        JSONObject classResultJson = classesJson.getJSONObject(index);

        ClassResult classResult = new ClassResult();
        classResult.setClassName(classResultJson.getString("class"));
        classResult.setTypeHierarchy(classResultJson.has("type_hierarchy") ? classResultJson.getString("type_hierarchy")
            : classResult.getClassName());
        classResult.setScore(Float.valueOf(Double.valueOf(classResultJson.getDouble("score")).toString()));
        classResults.add(classResult);
      }
    } catch (JSONException e) {
      logger.error("json parse error: " + imageTitle);
      e.printStackTrace();
      return Optional.empty();
    }

    visualRecognitionResult.setClassResults(classResults);

    return Optional.of(visualRecognitionResult);
  }

}
