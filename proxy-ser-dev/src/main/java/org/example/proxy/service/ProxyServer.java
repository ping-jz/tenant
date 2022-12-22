package org.example.proxy.service;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.apache.commons.lang3.StringUtils;
import org.example.net.DefaultDispatcher;
import org.example.net.handler.ConnectionCreate;
import org.example.proxy.codec.ProxyMessageHandler;
import org.example.proxy.config.ProxyServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 代理服务(主要负责消息中转)
 *
 * @author zhongjianping
 * @since 2022/12/12 23:00
 */
@Component
public class ProxyServer {

  /** 日志 */
  private Logger logger = LoggerFactory.getLogger(this.getClass());
  @Autowired
  private ProxyMessageHandler proxyMessageHandler;
  @Autowired
  private DefaultDispatcher dispatcher;
  @Autowired
  private ProxyService proxyService;
  /** 网络主线程 */
  private EventLoopGroup boss;
  /** 网络工作线程 */
  private EventLoopGroup workers;

  public ProxyServer() {
  }


  public ChannelFuture start() {
    if (boss == null) {
      boss = new NioEventLoopGroup(1);
    }

    if (workers == null) {
      workers = new NioEventLoopGroup();
    }

    ServerBootstrap bootstrap = new ServerBootstrap().channel(NioServerSocketChannel.class)
        .option(ChannelOption.SO_BACKLOG, 1024).option(ChannelOption.SO_REUSEADDR, true)
        .option(ChannelOption.TCP_NODELAY, true).handler(new LoggingHandler(LogLevel.INFO))
        .group(boss, workers).childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) {
            ch.pipeline().addLast(proxyMessageHandler).addLast(new ConnectionCreate())
                .addLast(dispatcher);
          }
        });

    ProxyServerConfig proxyServerConfig = proxyService.getProxyServerConfig();
    ChannelFuture channelFuture;
    try {
      if (StringUtils.isNoneEmpty(proxyServerConfig.getAddress())) {
        channelFuture = bootstrap.bind(proxyServerConfig.getAddress(), proxyServerConfig.getPort())
            .sync();
      } else {
        channelFuture = bootstrap.bind(proxyServerConfig.getPort()).sync();
      }

      logger.info("[Proxy Server] binding on:{}", channelFuture.channel().localAddress());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return channelFuture;
  }


  public void close() {
    if (boss != null) {
      boss.shutdownGracefully();
      boss = null;
    }
    if (workers != null) {
      workers.shutdownGracefully();
      workers = null;
    }

    logger.info("[Proxy Server] close");
  }


  public EventLoopGroup getBoss() {
    return boss;
  }

  public void setBoss(EventLoopGroup boss) {
    this.boss = boss;
  }

  public EventLoopGroup getWorkers() {
    return workers;
  }

  public void setWorkers(EventLoopGroup workers) {
    this.workers = workers;
  }

  public ProxyMessageHandler getProxyMessageHandler() {
    return proxyMessageHandler;
  }

  public void setProxyMessageHandler(ProxyMessageHandler proxyMessageHandler) {
    this.proxyMessageHandler = proxyMessageHandler;
  }

  public DefaultDispatcher getDispatcher() {
    return dispatcher;
  }

  public void setDispatcher(DefaultDispatcher dispatcher) {
    this.dispatcher = dispatcher;
  }

  public ProxyService getProxyService() {
    return proxyService;
  }

  public void setProxyService(ProxyService proxyService) {
    this.proxyService = proxyService;
  }
}
