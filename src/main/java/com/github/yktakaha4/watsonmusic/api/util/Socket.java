package com.github.yktakaha4.watsonmusic.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.yktakaha4.watsonmusic.WebProperties;

@Component
public class Socket {
  private final WebProperties webProperties;
  private final SimpMessagingTemplate simpMessagingTemplate;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  public Socket(WebProperties webProperties, SimpMessagingTemplate simpMessagingTemplate) {
    this.webProperties = webProperties;
    this.simpMessagingTemplate = simpMessagingTemplate;
  }

  public void sendToClients(Object object) {
    try {
      String jsonString = new ObjectMapper().writeValueAsString(object);
      simpMessagingTemplate.convertAndSend(webProperties.getSocketTopicActions(), jsonString);
      logger.info("succeed send to clients: " + jsonString);
    } catch (Exception e) {
      logger.error("failed send to clients...", e);
    }
  }
}
