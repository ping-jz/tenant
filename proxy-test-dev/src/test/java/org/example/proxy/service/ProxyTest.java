package org.example.proxy.service;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.commons.lang3.RandomUtils;
import org.example.proxy.ProxyClientConfig;
import org.example.proxy.ProxyClientService;
import org.example.proxy.config.ProxyServerConfig;
import org.example.proxy.model.ServerRegister;
import org.example.serde.CommonSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProxyTest {

  private EventLoopGroup boss;
  private EventLoopGroup worker;
  private CommonSerializer commonSerializer;
  private int port;

  @BeforeEach
  public void start() {
    boss = new NioEventLoopGroup(1);
    worker = new NioEventLoopGroup(1);
    port = RandomUtils.nextInt(10000, 60000);
    commonSerializer = new CommonSerializer();
    commonSerializer.registerObject(ServerRegister.class);
  }

  @AfterEach
  public void end() {
    boss.shutdownGracefully();
    worker.shutdownGracefully();
  }

  @Test
  public void connectTest() {
    ProxyService proxyService = defaultProxyService();
    proxyService.start();

    ProxyClientService client = defaultProxyClient();
    client.connect(worker, new ChannelInitializer<>() {
      @Override
      protected void initChannel(Channel ch) throws Exception {

      }
    });

    ServerRegister serverRegister = new ServerRegister();
    serverRegister.setId(client.getProxyClientConfig().getId());
    client.register(serverRegister);
  }

  private ProxyClientService defaultProxyClient() {
    ProxyClientConfig clientConfig = new ProxyClientConfig();
    clientConfig.setAddress("localhost");
    clientConfig.setPort(port);
    ProxyClientService client = new ProxyClientService();
    client.setProxyClientConfig(clientConfig);
    client.setCommonSerializer(commonSerializer);
    return client;
  }

  private ProxyService defaultProxyService() {
    ProxyServerConfig serverConfig = new ProxyServerConfig();
    serverConfig.setId(1);
    serverConfig.setAddress(null);
    serverConfig.setPort(port);
    ProxyService proxyService = new ProxyService();
    proxyService.setBoss(boss);
    proxyService.setWorkers(worker);
    proxyService.setProxyServerConfig(serverConfig);
    proxyService.setCommonSerializer(commonSerializer);
    return proxyService;
  }

}
