package com.github.yktakaha4.watsonmusic;

import java.util.TimeZone;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.env.Environment;

@SpringBootApplication
@ImportResource(value = "classpath:applicationContext.xml")
public class WatsonMusicApplication {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private Environment environment;

  @PostConstruct
  private void initialize() {
    String timeZone = environment.getProperty("app.timezone");
    logger.debug("Timezone: " + timeZone);
    TimeZone.setDefault(TimeZone.getTimeZone(timeZone));
  }

  public static void main(String[] args) {
    SpringApplication.run(WatsonMusicApplication.class, args);
  }

}
