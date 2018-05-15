package com.github.yktakaha4.watsonmusic.api.util;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.yktakaha4.watsonmusic.WebProperties;
import com.github.yktakaha4.watsonmusic.api.ClientActionType;
import com.github.yktakaha4.watsonmusic.api.dj.DiscJockey;
import com.github.yktakaha4.watsonmusic.model.Feedback;
import com.github.yktakaha4.watsonmusic.model.FeedbackType;
import com.github.yktakaha4.watsonmusic.service.FeedbackService;
import com.github.yktakaha4.watsonmusic.service.RequestService;

@Component
public class Mqtt implements MqttCallback {
  private final WebProperties webProperties;
  private final RequestService requestService;
  private final FeedbackService feedbackService;
  private final DiscJockey diskJockey;
  private final Socket socket;

  private static final int QOS = 0;
  private static final int FAILED_COUNT_MAX = 3;

  private Optional<MqttClient> mqttClient = Optional.empty();
  private int failedCount = 0;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  public Mqtt(WebProperties webProperties, RequestService requestService, FeedbackService feedbackService,
      DiscJockey discJockey, Socket socket) {
    this.webProperties = webProperties;
    this.requestService = requestService;
    this.feedbackService = feedbackService;
    this.diskJockey = discJockey;
    this.socket = socket;

    connect(webProperties);
  }

  private void connect(WebProperties webProperties) {
    Optional<MqttClient> maybeMqttClient;
    try {
      MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
      mqttConnectOptions.setUserName("token:" + webProperties.getMqttServerChannelToken());

      MqttClient mqttClient = new MqttClient(webProperties.getMqttServerUrl(), MqttClient.generateClientId(),
          new MemoryPersistence());
      mqttClient.setCallback(this);

      logger.info("connecting mqtt server: url=" + webProperties.getMqttServerUrl() + ", token=*****"
          + StringUtils.right(webProperties.getMqttServerChannelToken(), 3));
      mqttClient.connect(mqttConnectOptions);
      mqttClient.subscribe(String.format("%s/+", webProperties.getMqttServerChannelResource()), QOS);

      maybeMqttClient = Optional.of(mqttClient);
    } catch (Exception e) {
      maybeMqttClient = Optional.empty();
      logger.error("failed to connect mqtt server...", e);
    }
    mqttClient = maybeMqttClient;

    if (mqttClient.isPresent()) {
      logger.info("connected: " + mqttClient.get().getServerURI());
    }
    this.mqttClient = maybeMqttClient;
  }

  @Override
  public void connectionLost(Throwable cause) {
    logger.warn("connection lost...", cause);
    synchronized (this) {
      if (failedCount < FAILED_COUNT_MAX) {
        connect(webProperties);
      }
      failedCount++;
    }
  }

  @Override
  public void messageArrived(String topic, MqttMessage message) throws Exception {
    String payload = new String(message.getPayload(), "UTF-8");
    logger.info("message arrived: topic=" + topic + ", payload=" + payload);

    if (topic.endsWith("request")) {
      try {
        JSONObject jsonObject = new JSONObject(payload);
        JSONObject data = jsonObject.getJSONArray("data").getJSONObject(0);

        requestService.appendRequestByVoice(data.getString("text").replaceAll(" ", ""),
            data.getString("type").toUpperCase(),
            Instant.ofEpochMilli(jsonObject.getLong("ts")).atZone(ZoneId.systemDefault()).toLocalDateTime());
        socket.sendToClients(Arrays.asList(ClientActionType.GET_REQUESTS));
      } catch (Exception e) {
        logger.error("failed parse payload...", e);
      }
    } else if (topic.endsWith("feedback")) {
      try {
        JSONObject jsonObject = new JSONObject(payload);
        JSONObject data = jsonObject.getJSONArray("data").getJSONObject(0);

        diskJockey.getJockingTrack().ifPresent(jockingTrack -> {
          Feedback feedback = feedbackService.createFeedback(jockingTrack.getTrack().getTrackTag(),
              data.getString("type").toUpperCase(),
              FeedbackType.valueOf(data.getString("feedback").toUpperCase()));
          feedback.setCreatedAt(
              Instant.ofEpochMilli(jsonObject.getLong("ts")).atZone(ZoneId.systemDefault()).toLocalDateTime());
          feedbackService.addFeedback(feedback);
        });
      } catch (Exception e) {
        logger.error("failed parse payload...", e);
      }
    } else {
      logger.warn("unknown topic...");
    }
  }

  @Override
  public void deliveryComplete(IMqttDeliveryToken token) {
  }

}
