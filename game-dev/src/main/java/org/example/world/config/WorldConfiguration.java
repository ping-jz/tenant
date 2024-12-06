package org.example.world.config;

import org.example.common.handler.ConnectionManagerHandler;
import org.example.exec.VirtualThreadExecutorService;
import org.example.net.ConnectionManager;
import org.example.net.DefaultDispatcher;
import org.example.net.handler.DispatcherHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 服务器环境配置
 *
 * @author zhongjianping
 * @since 2024/12/5 18:08
 */
@Configuration
public class WorldConfiguration {

  @Bean
  public ConnectionManager connectionManager() {
    return new ConnectionManager();
  }

  @Bean
  public ConnectionManagerHandler connectionManagerHandler(ConnectionManager manager) {
    return new ConnectionManagerHandler(manager);
  }

  @Bean
  public VirtualThreadExecutorService virtualThreadExecutor() {
    return new VirtualThreadExecutorService();
  }

  @Bean
  public DispatcherHandler dispatcherHandler(DefaultDispatcher defaultDispatcher) {
    return new DispatcherHandler(defaultDispatcher);
  }

}
