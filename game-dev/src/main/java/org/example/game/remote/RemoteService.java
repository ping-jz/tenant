package org.example.game.remote;

import static org.example.common.model.ServerInfo.serInfo;
import static org.example.common.model.WorldId.worldId;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import org.example.common.ThreadCommonResource;
import org.example.common.model.ServerInfo;
import org.example.common.net.generated.invoker.RegisterFacadeInvoker;
import org.example.common.util.DefaultClientBootStrap;
import org.example.exec.VirutalExecutors;
import org.example.game.GameConfig;
import org.example.model.AnonymousId;
import org.example.net.Connection;
import org.example.net.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RemoteService {

  private static final Logger logger = LoggerFactory.getLogger(RemoteService.class);

  private GameConfig config;

  private ThreadCommonResource threadCommonResource;
  private ClientInitHandler channelInitializer;
  private ConnectionManager connectionManager;
  private RegisterFacadeInvoker registerFacadeInvoker;

  public RemoteService(GameConfig config, ThreadCommonResource threadCommonResource,
      ClientInitHandler channelInitializer, ConnectionManager connectionManager,
      RegisterFacadeInvoker registerFacadeInvoker) {
    this.config = config;
    this.threadCommonResource = threadCommonResource;
    this.channelInitializer = channelInitializer;
    this.connectionManager = connectionManager;
    this.registerFacadeInvoker = registerFacadeInvoker;
  }

  public void serverStart() {
    connect(serInfo(worldId("world"), new InetSocketAddress("localhost", 8082)));
  }

  public void connect(ServerInfo server) {
    try {
      ChannelFuture f = new DefaultClientBootStrap(threadCommonResource,
          channelInitializer).connect(server).sync();
      if (f.isSuccess()) {
        logger.info("目标服务器：{}，连接成功", server, f.cause());
        registerChannel(server, f.channel());
      } else {
        logger.error("目标服务器：{}，连接失败", server, f.cause());
        VirutalExecutors.commonPool()
            .schedule(() -> connect(server), 3, TimeUnit.SECONDS);
      }
    } catch (Exception e) {
      logger.error("连接：{}异常", server, e);
      VirutalExecutors.commonPool()
          .schedule(() -> connect(server), 3, TimeUnit.SECONDS);
    }
  }

  void registerChannel(ServerInfo server, Channel channel) {
    Connection connection = channel.attr(Connection.CONNECTION).get();
    if (connection.id() instanceof AnonymousId) {
      registerFacadeInvoker.of(connection).serverRegister(config.getId())
          .whenComplete((res, ex) -> {
            if (ex != null || !res) {
              logger.error("本服：{}与目标服务器{}注册，结果：{},", config.getId(),
                  server.id(), res, ex);
            } else {
              logger.info("本服：{}与目标服务器{}，注册成功", config.getId(), server.id());
              connectionManager.bindChannel(server.id(), channel);
            }
          });
    } else {
      logger.error("服务器：{}，尝试重复注册", connection.id());
    }
  }


}

