package org.example.common.util;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.example.common.ThreadCommonResource;
import org.example.common.model.ServerInfo;


public class DefaultClientBootStrap {

  private ChannelInitializer<Channel> initializer;
  private ThreadCommonResource threadCommonResource;


  public DefaultClientBootStrap(ThreadCommonResource threadCommonResource,
      ChannelInitializer<Channel> initializer) {
    this.initializer = initializer;
    this.threadCommonResource = threadCommonResource;
  }

  public ChannelFuture connect(ServerInfo worldServer) {
    Bootstrap b = new Bootstrap();
    b
        .group(threadCommonResource.getWorker())
        .channel(NettyEventLoopUtil.getClientSocketChannelClass())
        .handler(new LoggingHandler(LogLevel.INFO))
        .handler(initializer);

    return b.connect(worldServer.addr());
  }
}
