package org.example.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.example.common.ThreadCommonResource;
import org.example.net.client.ReqClient;
import org.example.net.handler.HandlerRegistry;
import org.example.net.server.ReqServer;
import org.example.serde.CommonSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CrossDispatcherTest {

  private static ThreadCommonResource resource;
  private static final int ECHO = 1;
  private static final int DO_NOTHING = 2;
  private static final int REQ_RES = 3;

  private ReqServer rpcServer;
  private ReqClient rpcClient;
  private String address;
  private CrossHelloWorldFacade crossFacade;
  private GameHelloWorldFacade gameFacade;

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
    crossFacade = new CrossHelloWorldFacade();
    HandlerRegistry serverRegistry = new HandlerRegistry();
    serverRegistry.registeHandlers(serverRegistry.findHandler(crossFacade));
    rpcServer.handler(new DispatcherHandler(new CrossDispatcher(serverRegistry)));
    rpcServer.codec(new CommonSerializer());
    rpcServer.start(resource);
    address = rpcServer.ip() + ':' + rpcServer.port();


    rpcClient = new ReqClient();
    gameFacade = new GameHelloWorldFacade();
    HandlerRegistry cliRegistry = new HandlerRegistry();
    cliRegistry.registeHandlers(cliRegistry.findHandler(gameFacade));
    rpcClient.codec(new CommonSerializer());
    rpcClient.handler(new DispatcherHandler(new CrossDispatcher(cliRegistry)));
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
      rpcClient.invoke(address, Message.of(CrossDispatcherTest.DO_NOTHING));
    }
    TimeUnit.MILLISECONDS.sleep(100);

    assertTrue(rpcClient.getConnection(address).isActive());
    assertEquals(invokeTimes, crossFacade.invokeTimes());
  }

  @Test
  public void testCallback() throws Exception {
    String helloWorld = "Hello World";
    long timeOut = 1000;
    List<CountDownLatch> latches = new ArrayList<>(invokeTimes);
    for (int i = 0; i < invokeTimes; i++) {
      CountDownLatch latch = new CountDownLatch(1);
      Message request = Message.of().proto(ECHO).packet(helloWorld);
      rpcClient.invokeWithCallBack(address, request
          , (String msg) -> {
            assertEquals(helloWorld, msg);
            latch.countDown();
          }, timeOut);

      latches.add(latch);
    }

    for (CountDownLatch latch : latches) {
      assertTrue(latch.await(timeOut, TimeUnit.MILLISECONDS));
    }

    assertTrue(rpcClient.getConnection(address).isActive());
    assertEquals(invokeTimes, crossFacade.invokeTimes());
  }

  @Test
  public void testHandler() throws Exception {
    String helloWorld = "Hello World";
    long timeOut = 100;
    for (int i = 0; i < invokeTimes; i++) {
      Message request = Message.of().proto(CrossDispatcherTest.REQ_RES).packet(helloWorld);
      rpcClient.invoke(address, request);
    }

    TimeUnit.MILLISECONDS.sleep(timeOut);

    assertTrue(rpcClient.getConnection(address).isActive());
    assertEquals(invokeTimes, crossFacade.invokeTimes());
    assertEquals(invokeTimes, gameFacade.invokeTimes());
  }

  /**
   * 世界你好，门面
   *
   * @author ZJP
   * @since 2021年07月22日 21:58:02
   **/
  @Facade
  private static class CrossHelloWorldFacade {


    private AtomicInteger invokeTimes = new AtomicInteger();

    public int invokeTimes() {
      return invokeTimes.get();
    }

    /**
     * 回声
     *
     * @param str 内容
     * @since 2021年07月22日 21:58:45
     */

    @ReqMethod(ECHO)
    public Object echo(Object str) {
      invokeTimes.incrementAndGet();
      return str;
    }

    @ReqMethod(DO_NOTHING)
    public void doNothing() {
      invokeTimes.incrementAndGet();
    }

    @ReqMethod(REQ_RES)
    public Object req(Object obj) {
      invokeTimes.incrementAndGet();
      return obj;
    }
  }

  @Facade
  private static class GameHelloWorldFacade {

    private AtomicInteger invokeTimes = new AtomicInteger();

    public int invokeTimes() {
      return invokeTimes.get();
    }

    @ReqMethod(-REQ_RES)
    public Object req(Object obj) {
      invokeTimes.incrementAndGet();
      return obj;
    }
  }
}
