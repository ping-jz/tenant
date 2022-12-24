package org.example.proxy.service;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.example.net.ConnectionManager;
import org.example.net.DefaultDispatcher;
import org.example.net.Message;
import org.example.net.codec.MessageCodec;
import org.example.net.handler.DispatcherHandler;
import org.example.net.handler.HandlerRegistry;
import org.example.proxy.ProxyStart;
import org.example.proxy.client.ProxyClientConfig;
import org.example.proxy.client.ProxyClientService;
import org.example.proxy.model.ServerRegister;
import org.example.serde.CommonSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class MultiClientProxyTest {

  private EventLoopGroup boss;
  private EventLoopGroup worker;
  private CommonSerializer commonSerializer;
  private AnnotationConfigApplicationContext context;

  @BeforeEach
  public void start() {
    boss = new NioEventLoopGroup(1);
    worker = new NioEventLoopGroup(1);
    context = defaultProxyService();
    commonSerializer = context.getBean(CommonSerializer.class);
  }

  @AfterEach
  public void end() {
    boss.shutdownGracefully();
    worker.shutdownGracefully();
    context.close();
  }

  @Test
  public void clientsTest() throws Exception {
    HelloWorldFacade oneFacade = new HelloWorldFacade();
    ProxyClientService mainClient = defaultProxyClient(1, oneFacade);
    ServerRegister oneServerRegister = new ServerRegister();
    oneServerRegister.setId(mainClient.getProxyClientConfig().getId());
    mainClient.register(oneServerRegister);

    List<HelloWorldFacade> facadeList = new ArrayList<>();
    List<ProxyClientService> clients = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      HelloWorldFacade twoFacade = new HelloWorldFacade();
      ProxyClientService twoClient = defaultProxyClient(i + 10, twoFacade);
      ServerRegister twoServerRegister = new ServerRegister();
      twoServerRegister.setId(twoClient.getProxyClientConfig().getId());
      twoClient.register(twoServerRegister);

      clients.add(twoClient);
      facadeList.add(twoFacade);
    }

    for (ProxyClientService clientService : clients) {
      for (int i = 0; i < 100; i++) {
        Message message = new Message();
        message.proto(HelloWorldFacade.echo);
        message.packet("hello World");
        mainClient.send(clientService.getProxyClientConfig().getId(), message);
      }
    }

    TimeUnit.MILLISECONDS.sleep(100);
    Assertions.assertEquals(100 * clients.size(), oneFacade.integer.get());
    for (HelloWorldFacade facade : facadeList) {
      Assertions.assertEquals(100, facade.integer.get());
    }
  }

  @Test
  public void broadCastTest() throws Exception {
    HelloWorldFacade oneFacade = new HelloWorldFacade();
    ProxyClientService mainClient = defaultProxyClient(1, oneFacade);
    ServerRegister oneServerRegister = new ServerRegister();
    oneServerRegister.setId(mainClient.getProxyClientConfig().getId());
    mainClient.register(oneServerRegister);

    List<HelloWorldFacade> facadeList = new ArrayList<>();
    List<Integer> clients = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      HelloWorldFacade twoFacade = new HelloWorldFacade();
      ProxyClientService twoClient = defaultProxyClient(i + 10, twoFacade);
      ServerRegister twoServerRegister = new ServerRegister();
      twoServerRegister.setId(twoClient.getProxyClientConfig().getId());
      twoClient.register(twoServerRegister);

      clients.add(twoClient.getProxyClientConfig().getId());
      facadeList.add(twoFacade);
    }

    for (int i = 0; i < 100; i++) {
      Message message = new Message();
      message.proto(HelloWorldFacade.echo);
      message.packet("hello World");
      mainClient.send(clients, message);
    }

    TimeUnit.MILLISECONDS.sleep(100);
    Assertions.assertEquals(100 * clients.size(), oneFacade.integer.get());
    for (HelloWorldFacade facade : facadeList) {
      Assertions.assertEquals(100, facade.integer.get());
    }
  }

  private ProxyClientService defaultProxyClient(int clientId, Object... facades) {
    ProxyClientConfig clientConfig = new ProxyClientConfig();
    clientConfig.setId(clientId);
    clientConfig.proxyId(110);
    clientConfig.setAddress("localhost");
    clientConfig.setPort(55555);

    ProxyClientService client = new ProxyClientService();
    client.setProxyClientConfig(clientConfig);
    client.setCommonSerializer(commonSerializer);

    HandlerRegistry handlerRegistry = new HandlerRegistry();
    for (Object o : facades) {
      handlerRegistry.registerHandlers(o);
    }
    ChannelHandler handler = new DispatcherHandler(new DefaultDispatcher(handlerRegistry));
    client.connect(worker, new ChannelInitializer<>() {
      @Override
      protected void initChannel(Channel ch) {
        ch.pipeline()
            .addLast(new MessageCodec(commonSerializer))
            .addLast(new ConnectionManager())
            .addLast(handler);
      }
    });
    return client;
  }

  private AnnotationConfigApplicationContext defaultProxyService() {
    ProxyStart start = new ProxyStart();
    return start.start();
  }

}
