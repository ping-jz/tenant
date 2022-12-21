package org.example.proxy.service;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import java.util.concurrent.TimeUnit;
import org.example.net.DefaultDispatcher;
import org.example.net.Message;
import org.example.net.codec.MessageCodec;
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

public class ProxyTest {

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
  public void connectTest() throws Exception {
    HelloWorldFacade helloWorldFacade = new HelloWorldFacade();
    ProxyClientService client = defaultProxyClient(helloWorldFacade);

    ServerRegister serverRegister = new ServerRegister();
    serverRegister.setId(client.getProxyClientConfig().getId());
    client.register(serverRegister);

    Message message = new Message();
    message.proto(HelloWorldFacade.echo);
    message.packet("hello World");
    client.send(client.getProxyClientConfig().getId(), message);

    TimeUnit.MILLISECONDS.sleep(100);
    Assertions.assertEquals(2, helloWorldFacade.integer.get());
  }

  private ProxyClientService defaultProxyClient(Object... facades) {
    ProxyClientConfig clientConfig = new ProxyClientConfig();
    clientConfig.setId(1);
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
    DefaultDispatcher defaultDispatcher = new DefaultDispatcher(handlerRegistry);
    client.connect(worker, new ChannelInitializer<>() {
      @Override
      protected void initChannel(Channel ch) throws Exception {
        ch.pipeline().addLast(new MessageCodec(commonSerializer)).addLast(defaultDispatcher);
      }
    });
    return client;
  }

  private AnnotationConfigApplicationContext defaultProxyService() {
    ProxyStart start = new ProxyStart();
    AnnotationConfigApplicationContext context = start.start();
    return context;
  }

}
