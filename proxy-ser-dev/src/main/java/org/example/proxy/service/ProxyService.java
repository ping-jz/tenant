package org.example.proxy.service;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.example.proxy.config.ProxyServerConfig;
import org.example.proxy.model.ServerRegister;
import org.example.proxy.register.RegisterCodec;
import org.example.serde.CommonSerializer;
import org.example.serde.NettyByteBufUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 代理服务(主要负责消息中转)
 *
 * @author zhongjianping
 * @since 2022/12/12 23:00
 */
public class ProxyService {

  /** 日志 */
  private Logger logger = LoggerFactory.getLogger(this.getClass());
  /** 服务器配置 */
  private ProxyServerConfig proxyServerConfig;
  /** 所有链接， 服务ID -> Netty网络链接 */
  private Map<Integer, Channel> channels;
  /** 通用序列化 */
  private CommonSerializer commonSerializer;
  /** 网络主线程 */
  private EventLoopGroup boss;
  /** 网络工作线程 */
  private EventLoopGroup workers;


  public ProxyService() {
    channels = new ConcurrentHashMap<>();
    commonSerializer = new CommonSerializer();
    registerSerializer(commonSerializer);
  }

  public void registerSerializer(CommonSerializer commonSerializer) {
    commonSerializer.registerObject(ServerRegister.class);
  }

  /**
   * 服务链接中转服的首条首条协议必须是注册协议，否则当作恶意链接关闭
   *
   * @param channel 网络链接
   * @since 2022/12/12 22:58
   */
  public boolean register(Channel channel, ByteBuf buf) {
    int length = buf.readInt();
    int source = NettyByteBufUtil.readInt32(buf);
    int target = NettyByteBufUtil.readInt32(buf);
    ServerRegister register = commonSerializer.read(buf);

    int id = proxyServerConfig.getId();
    if (id != target) {
      logger.error("[proxy],源:{}_{}, 注册目标:{}, 当前服:{}, 注册目标错误",
          channel.remoteAddress(), source, target, id);
      return false;
    }

    Channel oldChannel = channels.putIfAbsent(register.getId(), channel);
    if (oldChannel != null) {
      logger.error("[proxy]源:{}-{}, 注册目标:{}, 当前服:{}, 注册目标错误", channel.remoteAddress(),
          source, target, id);
      return false;
    }

    return true;
  }

  public ChannelFuture start() {
    if (boss == null) {
      boss = new NioEventLoopGroup(1);
    }

    if (workers == null) {
      workers = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());
    }

    ProxyService proxyService = this;
    ServerBootstrap bootstrap = new ServerBootstrap().channel(NioServerSocketChannel.class)
        .option(ChannelOption.SO_BACKLOG, 1024)
        .option(ChannelOption.SO_REUSEADDR, true)
        .option(ChannelOption.TCP_NODELAY, true)
        .handler(new LoggingHandler(LogLevel.INFO))
        .group(boss, workers).childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) {
            ch.pipeline()
                .addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, Integer.BYTES))
                .addLast(new IdleStateHandler(0, 0, 10)).addLast(new RegisterCodec(proxyService));
            //TODO IDLE_STATE
            //TODO REGISTER
            //TODO LOGIC
          }
        });

    ChannelFuture channelFuture;
    try {
      if (StringUtils.isNoneEmpty(proxyServerConfig.getAddress())) {
        channelFuture = bootstrap.bind(proxyServerConfig.getAddress(),
            proxyServerConfig.getPort()).sync();
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


  public ProxyServerConfig getProxyServerConfig() {
    return proxyServerConfig;
  }

  public void setProxyServerConfig(ProxyServerConfig proxyServerConfig) {
    this.proxyServerConfig = proxyServerConfig;
  }

  public Map<Integer, Channel> getChannels() {
    return channels;
  }

  public void setChannels(Map<Integer, Channel> channels) {
    this.channels = channels;
  }

  public CommonSerializer getCommonSerializer() {
    return commonSerializer;
  }

  public void setCommonSerializer(CommonSerializer commonSerializer) {
    this.commonSerializer = commonSerializer;
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


}
