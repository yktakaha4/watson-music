package com.github.yktakaha4.watsonmusic.util;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.yktakaha4.watsonmusic.WebProperties;
import com.github.yktakaha4.watsonmusic.model.Source;

@Component
public class RequestInterval {
  private final long interval;
  private final Map<Source, Object> locks = new ConcurrentHashMap<>();
  private final Map<Source, LocalTime> lastRequestTimes = new ConcurrentHashMap<>();
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  public RequestInterval(WebProperties webProperties) {
    this.interval = webProperties.getRequestInterval().longValue();
    Arrays.stream(Source.values()).forEach(source -> locks.put(source, new Object()));
  }

  public void takeInterval(Source source) {
    Source intervalSource = source.getIntervalSource();
    synchronized (locks.get(intervalSource)) {
      if (lastRequestTimes.containsKey(intervalSource)) {
        long elapsed = ChronoUnit.MILLIS.between(lastRequestTimes.get(intervalSource), LocalTime.now());
        long timeout = interval - elapsed;
        if (timeout > 0) {
          logger.info("take interval: " + timeout + " ms");

          try {
            Thread.sleep(timeout);

          } catch (InterruptedException e) {
            logger.warn("interrupted: " + e.getLocalizedMessage());

          }
        }
      }

      lastRequestTimes.put(intervalSource, LocalTime.now());
    }
  }
}
