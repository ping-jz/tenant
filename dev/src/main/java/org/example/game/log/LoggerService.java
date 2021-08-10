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

  /** 下次时间变化(单位:天) */
  private long nextRolloverMillis;
  /** 今天时间，方便获取年月日 */
  private ZonedDateTime today;
  /** 缓存builder */
  private ThreadLocal<StringBuilder> threadLocalSb = ThreadLocal.withInitial(StringBuilder::new);

  public LoggerService() {
    rollover();
  }

  private void rollover() {
    ZonedDateTime dateTime = ZonedDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT,
        ZoneId.systemDefault());
    nextRolloverMillis = dateTime.plusDays(1).toInstant().toEpochMilli();
    today = dateTime;
  }


  @PostConstruct
  public void postConstruct() {
    logger = LoggerContext.getContext().getLogger(this.getClass());
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
    if (nextRolloverMillis < System.currentTimeMillis()) {
      rollover();
    }
    //TODO Is Ok, make it better
    StringBuilder sb = threadLocalSb.get();
    sb.setLength(0);


    sb.append(gameId).append("/sys/");
    fillDataPattern(sb);
    sb.append('/').append(sysName);

    ThreadContext.put("game_sys_log", sb.toString());
  }

  /**
   * @return dataPattern yyyy-MM-dd
   * @since 2021年08月08日 22:07:41
   */
  private StringBuilder fillDataPattern(StringBuilder sb) {
    char split = '-';
    return sb.append(today.getYear()).append(split).append(today.getMonthValue()).append(split)
        .append(today.getDayOfMonth());
  }
}
