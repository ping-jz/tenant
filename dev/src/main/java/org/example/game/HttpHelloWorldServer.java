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
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.example.common.ThreadCommonResource;
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

  private Channel channel;

  @EventListener
  public void onStart(ContextStartedEvent refreshedEvent) throws Exception {
    start();
  }


  public void start() throws Exception {
    ServerBootstrap b = new ServerBootstrap();
    b.option(ChannelOption.SO_BACKLOG, 1024);
    b.group(threadCommonResource.getBoss(), threadCommonResource.getWorker())
        .channel(NioServerSocketChannel.class)
        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(initializer);

    channel = b.bind(port).sync().channel();

    System.err.println("Open your web browser and navigate to " +
        "http" + "://127.0.0.1:" + port + '/');
  }

  @Override
  public void close() throws Exception {
    if (channel != null) {
      System.out.format("%s closing\n", name);
      channel.close();
    }
  }
}
