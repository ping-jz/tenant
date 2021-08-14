package org.example.common;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.NettyRuntime;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.EventExecutor;
import java.util.concurrent.ThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 公用线程池
 *
 * @author ZJP
 * @since 2021年06月26日 14:53:18
 **/
@Component
public class ThreadCommonResource implements AutoCloseable {

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  /** BOSS线程 */
  private final NioEventLoopGroup boss;
  /** 工作线程 */
  private final NioEventLoopGroup worker;
  /** 逻辑线程 */
  private final EventExecutor[] executors;

  public ThreadCommonResource() {
    boss = new NioEventLoopGroup(1, new NamedThreadFactory("BOSS"));
    worker = new NioEventLoopGroup(NettyRuntime.availableProcessors() / 2,
        new NamedThreadFactory("WORKER"));
    executors = new EventExecutor[NettyRuntime.availableProcessors() / 2];

    ThreadFactory gameThreadFactory = new NamedThreadFactory("GAME");
    for (int i = 0; i < executors.length; i++) {
      executors[i] = new DefaultEventExecutor(gameThreadFactory);
    }
  }

  @Override
  public void close() {
    worker.shutdownGracefully();
    boss.shutdownGracefully();
    for (EventExecutor executor : executors) {
      executor.shutdownGracefully();
    }
    logger.info("threadCommonResource closing");
  }


  public NioEventLoopGroup getBoss() {
    return boss;
  }

  public NioEventLoopGroup getWorker() {
    return worker;
  }

  public EventExecutor[] getExecutors() {
    return executors;
  }
}
