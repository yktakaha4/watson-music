package com.github.yktakaha4.watsonmusic.service;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.yktakaha4.watsonmusic.ApplicationException;
import com.github.yktakaha4.watsonmusic.WebProperties;
import com.github.yktakaha4.watsonmusic.model.DiscoverySearchResult;
import com.github.yktakaha4.watsonmusic.util.PathUtils;
import com.ibm.watson.developer_cloud.discovery.v1.Discovery;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@Service
public class DiscoverySearchService {

  private final WebProperties webProperties;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final String url;
  private final String userName;
  private final String password;

  private final String returnFields = StringUtils.joinWith(",", "id", "musicId");

  @Autowired
  public DiscoverySearchService(WebProperties webProperties, PathUtils pathUtils) {
    this.webProperties = webProperties;
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

  public List<DiscoverySearchResult> search(String query, Collection<String> similarDocumentIds,
      Collection<String> ignoreDocumentIds) {
    try {
      WebTarget webTarget = ClientBuilder.newClient()
          .target(url)
          .path(String.format("/v1/environments/%s/collections/%s/query",
              webProperties.getDiscoveryEnvironmentId(), webProperties.getDiscoveryCollectionId()))
          .register(HttpAuthenticationFeature.basic(userName, password))
          .queryParam("version", Discovery.VERSION_DATE_2017_11_07)
          .queryParam("return", returnFields)
          .queryParam("count", webProperties.getDiscoveryQueryCount())
          .queryParam("natural_language_query", "{query}").resolveTemplate("query", query);

      if (!similarDocumentIds.isEmpty()) {
        webTarget = webTarget
            .queryParam("similar", true)
            .queryParam("similar.document_ids", StringUtils.join(similarDocumentIds, ","));
      }

      if (!ignoreDocumentIds.isEmpty()) {
        webTarget = webTarget
            .queryParam("filter", String.format("(%s)",
                ignoreDocumentIds.stream().map(id -> String.format("id::!%s", id))
                    .collect(Collectors.joining(","))));
      }

      logger.info("query: " + webTarget.getUri().toString());

      Builder builder = webTarget
          .property(ClientProperties.FOLLOW_REDIRECTS, true)
          .property(ClientProperties.CONNECT_TIMEOUT, webProperties.getConnectionTimeout())
          .request();

      RetryPolicy retryPolicy = new RetryPolicy()
          .retryIf((Response response) -> response.getStatus() >= 500)
          .retryOn((ProcessingException ex) -> ex.getCause() instanceof SocketTimeoutException)
          .withDelay(webProperties.getRequestInterval(), TimeUnit.SECONDS)
          .withMaxRetries(webProperties.getMaxRetryCount());

      Response response = Failsafe.with(retryPolicy).get(() -> {
        return builder.get();
      });

      return parse(response.readEntity(String.class));
    } catch (Throwable t) {
      logger.error("failed to search with discovery...");
      throw new ApplicationException(t);
    }
  }

  private List<DiscoverySearchResult> parse(String responseString) throws JSONException {
    try {
      JSONArray results = new JSONObject(responseString)
          .getJSONArray("results");

      return IntStream.range(0, results.length()).mapToObj(i -> results.getJSONObject(i)).map((result) -> {
        DiscoverySearchResult discoverySearchResult = new DiscoverySearchResult();
        discoverySearchResult.setDocumentId(result.getString("id"));
        discoverySearchResult.setMusicId(result.getInt("musicId"));
        discoverySearchResult.setScore(result.getJSONObject("result_metadata").getDouble("score"));

        return discoverySearchResult;

      }).collect(Collectors.toList());
    }catch(JSONException e) {
      logger.error("failed parse response: " + new JSONObject(responseString).toString());
      throw e;
    }
  }

}
