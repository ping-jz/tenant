package org.example.game.log;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import javax.annotation.PostConstruct;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LoggerService {

  private Logger logger;
  private Logger sysLogger;
  @Value("${game.id}")
  private String gameId;

  /** 缓存builder */
  private ThreadLocal<StringBuilder> threadLocalSb = ThreadLocal.withInitial(StringBuilder::new);

  @PostConstruct
  public void postConstruct() {
    logger = LoggerContext.getContext().getLogger(getClass());
    sysLogger = LoggerContext.getContext().getLogger("game_sys");
  }

  public Logger log() {
    initLoggerThreadContext();
    return logger;
  }

  private void initLoggerThreadContext() {
    ThreadContext.put("game_id", gameId);
  }

  public void sysLog() {
    initSysLogContext("test");
    sysLogger.info("asdfasdf");
  }

  private void initSysLogContext(String sysName) {
    //TODO Is Ok, make it better
    StringBuilder sb = threadLocalSb.get();
    sb.setLength(0);
    sb.append(gameId).append('/').append(sysName);
    ThreadContext.put("game_sys_log", sb.toString());
  }
}
