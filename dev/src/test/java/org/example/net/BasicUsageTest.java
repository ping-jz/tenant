package org.example.net;

import java.util.concurrent.TimeUnit;
import org.example.common.ThreadCommonResource;
import org.example.net.client.RpcClient;
import org.example.net.client.RpcClient.ClientHandlerInitializer;
import org.example.net.codec.MessageCodec;
import org.example.net.server.RpcServer;
import org.example.net.server.RpcServer.ServerHandlerInitializer;
import org.example.serde.CommonSerializer;
import org.example.serde.Serializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicUsageTest {

  private Logger logger = LoggerFactory.getLogger(BasicUsageTest.class);

  private static ThreadCommonResource resource;

  private SerTestHandler serHandler;
  private RpcServer rpcServer;
  private RpcClient rpcClient;
  private String address;

  private final int invokeTimes = 5;


  @BeforeAll
  static void beforeAll() {
    resource = new ThreadCommonResource();
  }

  @AfterAll
  static void afterAll() {
    if (resource != null) {
      resource.close();
    }
  }

  @BeforeEach
  void start() throws Exception {
    rpcServer = new RpcServer();

    Serializer<Object> serializer = new CommonSerializer();
    ServerHandlerInitializer initializer = new ServerHandlerInitializer(
        serHandler = new SerTestHandler());
    initializer.codec(new MessageCodec(serializer));
    rpcServer.handler(initializer);
    rpcServer.start(resource);
    address = rpcServer.ip() + ':' + rpcServer.port();

    ClientHandlerInitializer clientHandler = new ClientHandlerInitializer(new CliTestHandler());
    clientHandler.codec(new MessageCodec(serializer));
    rpcClient = new RpcClient();
    rpcClient.handler(clientHandler);
    rpcClient.init(resource.getBoss());
  }

  @AfterEach
  void close() {
    if (rpcServer != null) {
      rpcServer.close();
    }

    if (rpcClient != null) {
      rpcClient.close();
    }
  }

  @Test
  public void testOneway() throws Exception {
    for (int i = 0; i < invokeTimes; i++) {
      rpcClient.invoke(address, new Message().packet("Hello World"));
    }
    TimeUnit.MILLISECONDS.sleep(100);

    Assertions.assertTrue(rpcClient.getConnection(address).isActive());
    Assertions.assertEquals(invokeTimes, serHandler.invokeTimes());
    Assertions.assertEquals(1, serHandler.connectionCount());
  }

  @Test
  public void testSync() {
    throw new UnsupportedOperationException("Make me passed");
  }

  @Test
  public void testFuture() throws Exception {
    String helloWorld = "Hello World";
    long timeOut = 1000;
    for (int i = 0; i < invokeTimes; i++) {
      InvokeFuture messageFuture = rpcClient.invokeWithFuture(address,
          new Message().proto(1).packet(helloWorld), timeOut);
      Message message = messageFuture.waitResponse(timeOut);
      Assertions.assertNotNull(message);
      Assertions.assertTrue(message.isSuc());
      Assertions.assertEquals(helloWorld, message.packet());
      Assertions.assertEquals(-1, message.proto());
    }

    Assertions.assertTrue(rpcClient.getConnection(address).isActive());
    Assertions.assertEquals(invokeTimes, serHandler.invokeTimes());
    Assertions.assertEquals(1, serHandler.connectionCount());
  }

  @Test
  public void testFutureTimeOut() throws Exception {
    String helloWorld = "Hello World";
    long timeOut = 1000;
    for (int i = 0; i < invokeTimes; i++) {
      InvokeFuture messageFuture = rpcClient.invokeWithFuture(address,
          new Message().proto(1).packet(helloWorld), 1);
      Message message = messageFuture.waitResponse(timeOut);
      Assertions.assertNotNull(message);
      Assertions.assertFalse(message.isSuc());
      Assertions.assertEquals(message.status(), MessageStatus.TIMEOUT.status());
    }

    Assertions.assertTrue(rpcClient.getConnection(address).isActive());
    Assertions.assertEquals(1, serHandler.connectionCount());
  }

  @Test
  public void testCallback() {
    throw new UnsupportedOperationException("Make me passed");
  }


}
