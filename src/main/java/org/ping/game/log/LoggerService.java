package org.ping.game.log;

import javax.annotation.PostConstruct;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LoggerService {

  @Autowired
  private LoggerContext context;

  private Logger logger;

  @PostConstruct
  public void postConstruct() {
    logger = context.getLogger(LoggerService.class.getName());
  }

  public Logger log() {
    return logger;
  }

  public void sysLog(String name) {
  }
}
