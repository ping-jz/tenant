/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.example.game;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import java.net.InetSocketAddress;
import org.example.common.ThreadCommonResource;
import org.example.game.log.LoggerService;
import org.example.util.NettyEventLoopUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * An HTTP server that sends back the content of the received HTTP request in a pretty plaintext
 * form.
 */
@Component
public final class HttpHelloWorldServer implements AutoCloseable {

  @Autowired
  private ThreadCommonResource threadCommonResource;

  @Autowired
  private HttpHelloWorldServerInitializer initializer;

  @Value("${game.port}")
  private int port;
  @Value("${game.id}")
  private String name;
  @Autowired
  private LoggerService loggerService;

  private ChannelFuture channelFuture;

  @EventListener
  public void onStart(ContextStartedEvent refreshedEvent) throws Exception {
    start();
  }


  public boolean start() throws Exception {
    ServerBootstrap b = new ServerBootstrap();
    b.option(ChannelOption.SO_BACKLOG, 1024);
    b.group(threadCommonResource.getBoss(), threadCommonResource.getWorker())
        .channel(NettyEventLoopUtil.getServerSocketChannelClass())
        .option(ChannelOption.SO_REUSEADDR, true)
        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(initializer);

    channelFuture = b.bind(new InetSocketAddress(port)).sync();
    if (port == 0 && channelFuture.isSuccess()) {
      InetSocketAddress address = (InetSocketAddress) channelFuture.channel().localAddress();
      port = address.getPort();
      loggerService.log().info("rpc server start with random port: {}!", port);
    }

    System.err.println("Open your web browser and navigate to " +
        "http" + "://127.0.0.1:" + port + '/');
    return channelFuture.isSuccess();
  }

  @Override
  public void close() throws Exception {
    if (channelFuture != null) {
      loggerService.log().info("closing");
      channelFuture.channel().close();
      channelFuture = null;
    }
  }
}
