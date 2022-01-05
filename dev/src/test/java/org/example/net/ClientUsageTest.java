package org.example.net;

import org.example.common.ThreadCommonResource;
import org.example.net.client.ReqClient;
import org.example.net.server.ReqServer;
import org.example.serde.CommonSerializer;
import org.example.serde.Serializer;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ClientUsageTest {

  private static ThreadCommonResource resource;

  private ConnTestHandler serHandler;
  private ReqServer rpcServer;
  private ReqClient rpcClient;
  private String address;

  private final int invokeTimes = 5;


  @BeforeAll
  public static void beforeAll() {
    resource = new ThreadCommonResource();
  }

  @AfterAll
  public static void afterAll() {
    if (resource != null) {
      resource.close();
    }
  }

  @BeforeEach
  public void start() throws Exception {
    rpcServer = new ReqServer();

    Serializer<Object> serializer = new CommonSerializer();
    rpcServer.handler(serHandler = new ConnTestHandler("ser"));
    rpcServer.codec(serializer);
    rpcServer.start(resource);
    address = rpcServer.ip() + ':' + rpcServer.port();

    rpcClient = new ReqClient();
    rpcClient.codec(serializer);
    rpcClient.handler(new ConnTestHandler("cli"));
    rpcClient.init(resource.getBoss());
  }

  @AfterEach
  public void close() {
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
      rpcClient.invoke(address, Message.of(-1).packet("Hello World"));
    }
    TimeUnit.MILLISECONDS.sleep(100);

    assertTrue(rpcClient.getConnection(address).isActive());
    assertEquals(invokeTimes, serHandler.invokeTimes());
    assertEquals(1, serHandler.connectionCount());
  }

  @Test
  public void testCallback() throws Exception {
    String helloWorld = "Hello World";
    long timeOut = 1000;
    List<CountDownLatch> latches = new ArrayList<>(invokeTimes);
    for (int i = 0; i < invokeTimes; i++) {
      CountDownLatch latch = new CountDownLatch(1);
      Message request = Message.of().proto(1).packet(helloWorld);
      rpcClient.invokeWithCallBack(address, request
          , (Message msg) -> {
            assertNotNull(msg);
            assertTrue(msg.isSuc());
            assertEquals(helloWorld, msg.packet());
            latch.countDown();
          }, timeOut);

      latches.add(latch);
    }

    for (CountDownLatch latch : latches) {
      assertTrue(latch.await(timeOut, TimeUnit.MILLISECONDS));
    }

    assertTrue(rpcClient.getConnection(address).isActive());
    assertEquals(invokeTimes, serHandler.invokeTimes());
    assertEquals(1, serHandler.connectionCount());
  }


}
