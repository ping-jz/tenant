package org.example.world;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public final class WorldStart {

  private static Logger logger = LoggerFactory.getLogger(WorldStart.class);

  public static void main(String[] args) throws Exception {
    try {
      AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext();

      child.register(WorldConfig.class);

      child.refresh();
      child.start();

      Runtime.getRuntime().addShutdownHook(new Thread(child::close));
    } catch (Exception e) {
      logger.error("程序启动失败", e);
    }
  }


}
