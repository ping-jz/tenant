package org.example.game.log;

import javax.annotation.PostConstruct;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LoggerService {

  private Logger logger;
  @Value("${game.id}")
  private String gameId;


  @PostConstruct
  public void postConstruct() {
    logger = LoggerContext.getContext().getLogger(this.getClass());
  }

  public Logger log() {
    initLoggerThreadContext();
    return logger;
  }

  private void initLoggerThreadContext() {
    ThreadContext.put("game_id", gameId);
  }

  public void sysLog(String name) {
  }
}
