package com.github.yktakaha4.watsonmusic.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.yktakaha4.watsonmusic.ApplicationException;
import com.github.yktakaha4.watsonmusic.WebProperties;
import com.github.yktakaha4.watsonmusic.model.Document;
import com.github.yktakaha4.watsonmusic.model.DocumentOperationStatus;
import com.github.yktakaha4.watsonmusic.model.Source;
import com.github.yktakaha4.watsonmusic.util.PathUtils;
import com.github.yktakaha4.watsonmusic.util.RequestInterval;
import com.ibm.watson.developer_cloud.discovery.v1.Discovery;
import com.ibm.watson.developer_cloud.discovery.v1.model.AddDocumentOptions;
import com.ibm.watson.developer_cloud.discovery.v1.model.DeleteDocumentOptions;
import com.ibm.watson.developer_cloud.discovery.v1.model.DocumentAccepted;
import com.ibm.watson.developer_cloud.discovery.v1.model.UpdateDocumentOptions;
import com.ibm.watson.developer_cloud.http.HttpMediaType;
import com.ibm.watson.developer_cloud.service.exception.InternalServerErrorException;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@Service
public class DiscoveryDocumentService {
  private final String fileNameFormat = "docid_%s.json";

  private final WebProperties webProperties;
  private final RequestInterval requestInterval;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final String url;
  private final String userName;
  private final String password;

  @Autowired
  public DiscoveryDocumentService(WebProperties webProperties, PathUtils pathUtils, RequestInterval requestInterval) {
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

  public void addDocument(Document document) throws ApplicationException {
    try {
      Discovery discovery = createDiscovery();
      DocumentAccepted documentAccepted;
      try (InputStream inputStream = IOUtils.toInputStream(document.getContent(), StandardCharsets.UTF_8)) {
        AddDocumentOptions addDocumentOptions = new AddDocumentOptions.Builder()
            .environmentId(webProperties.getDiscoveryEnvironmentId())
            .collectionId(webProperties.getDiscoveryCollectionId())
            .fileContentType(HttpMediaType.APPLICATION_JSON)
            .filename(String.format(fileNameFormat, document.getDocumentId()))
            .file(inputStream)
            .build();

        documentAccepted = Failsafe.with(createRetryPolicy()).get(() -> {
          requestInterval.takeInterval(Source.DISCOVERY);
          return discovery.addDocument(addDocumentOptions).execute();
        });
      }
      document.setDiscoveryDocId(documentAccepted.getDocumentId());
      document.setPublishedAt(LocalDateTime.now());
      document.setOperationStatus(DocumentOperationStatus.NO_OP);
      logger.info("add document to discovery: " + document.getDiscoveryDocId());
    } catch (Throwable t) {
      logger.error("failed to add document to discovery...");
      throw new ApplicationException(t);
    }
  }

  public void updateDocument(Document document) throws ApplicationException {
    try {
      Discovery discovery = createDiscovery();
      try (InputStream inputStream = IOUtils.toInputStream(document.getContent(), StandardCharsets.UTF_8)) {
        UpdateDocumentOptions updateDocumentOptions = new UpdateDocumentOptions.Builder()
            .environmentId(webProperties.getDiscoveryEnvironmentId())
            .collectionId(webProperties.getDiscoveryCollectionId())
            .documentId(document.getDiscoveryDocId())
            .fileContentType(HttpMediaType.APPLICATION_JSON)
            .filename(String.format(fileNameFormat, document.getDocumentId()))
            .file(inputStream)
            .build();

        Failsafe.with(createRetryPolicy()).run(() -> {
          requestInterval.takeInterval(Source.DISCOVERY);
          discovery.updateDocument(updateDocumentOptions).execute();
        });
      }
      document.setPublishedAt(LocalDateTime.now());
      document.setOperationStatus(DocumentOperationStatus.NO_OP);
      logger.info("update document to discovery: " + document.getDiscoveryDocId());
    } catch (Throwable t) {
      logger.error("failed to update document to discovery...");
      throw new ApplicationException(t);
    }
  }

  public void deleteDocument(Document document) throws ApplicationException {
    try {
      Discovery discovery = createDiscovery();
      try (InputStream inputStream = IOUtils.toInputStream(document.getContent(), StandardCharsets.UTF_8)) {
        DeleteDocumentOptions deleteDocumentOptions = new DeleteDocumentOptions.Builder()
            .environmentId(webProperties.getDiscoveryEnvironmentId())
            .collectionId(webProperties.getDiscoveryCollectionId())
            .documentId(document.getDiscoveryDocId())
            .build();

        Failsafe.with(createRetryPolicy()).run(() -> {
          requestInterval.takeInterval(Source.DISCOVERY);
          discovery.deleteDocument(deleteDocumentOptions).execute();
        });
      }
      document.setPublishedAt(null);
      document.setOperationStatus(DocumentOperationStatus.NO_OP);
      logger.info("delete document from discovery: " + document.getDiscoveryDocId());
    } catch (Throwable t) {
      logger.error("failed to delete document to discovery...");
      throw new ApplicationException(t);
    }
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
