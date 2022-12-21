package org.example.proxy;

import org.example.proxy.service.ProxyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 中转服关闭
 *
 * @author zhongjianping
 * @since 2022/12/19 20:35
 */
@Component
public class ProxyStart {

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  /**
   * 关闭服务器
   *
   * @author zhongjianping
   * @since 2022/12/19 21:05
   */
  @EventListener
  public void start(ContextRefreshedEvent refreshedEvent) {
    ApplicationContext context = refreshedEvent.getApplicationContext();
    try {
      ProxyServer proxyService = context.getBean(ProxyServer.class);
      proxyService.start();
    } catch (Exception e) {
      logger.error("启动服务器异常");
    }
  }


  public AnnotationConfigApplicationContext start() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
        ProxyConfiguration.class);
    context.registerShutdownHook();
    return context;
  }
}
