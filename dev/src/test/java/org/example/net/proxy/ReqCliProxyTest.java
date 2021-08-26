package org.example.net.proxy;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.example.common.ThreadCommonResource;
import org.example.net.CrossDispatcher;
import org.example.net.DispatcherHandler;
import org.example.net.Facade;
import org.example.net.HelloWorld;
import org.example.net.ReqMethod;
import org.example.net.client.ReqClient;
import org.example.net.handler.HandlerRegistry;
import org.example.net.server.ReqServer;
import org.example.serde.CommonSerializer;
import org.example.serde.MarkSerializer;
import org.example.serde.Serializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReqCliProxyTest {

  private static ThreadCommonResource resource;

  private SerHelloWorldFacade serFacade;
  private CliHelloWorldFacade cliFacade;
  private ReqServer rpcServer;
  private ReqClient rpcClient;
  private ReqCliProxy proxy;
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
    Serializer<Object> serializer = createSerializer();

    {
      rpcServer = new ReqServer();
      serFacade = new SerHelloWorldFacade();
      HandlerRegistry serverRegistry = new HandlerRegistry();
      serverRegistry.registeHandlers(serverRegistry.findHandler(serFacade));
      rpcServer.handler(new DispatcherHandler(new CrossDispatcher(serverRegistry)));
      rpcServer.codec(serializer);
      rpcServer.start(resource);
      address = rpcServer.ip() + ':' + rpcServer.port();
    }

    {
      rpcClient = new ReqClient();
      cliFacade = new CliHelloWorldFacade();
      HandlerRegistry clientRegistry = new HandlerRegistry();
      clientRegistry.registeHandlers(clientRegistry.findHandler(cliFacade));
      rpcClient.handler(new DispatcherHandler(new CrossDispatcher(clientRegistry)));
      rpcClient.codec(serializer);
      rpcClient.init(resource.getBoss());
      rpcClient.getConnection(address);
    }

    //链接的创建和管理交给client，proxy不要管，直接用就行了
    proxy = new ReqCliProxy(rpcClient.manager());
  }

  private CommonSerializer createSerializer() {
    CommonSerializer serializer = new CommonSerializer();
    serializer.registerSerializer(10, Object.class, new MarkSerializer());
    return serializer;
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
  void doNothingTest() throws Exception {
    HelloWorld world = proxy.getProxy(address, HelloWorld.class);
    for (int i = 0; i < invokeTimes; i++) {
      world.doNothing();
    }

    TimeUnit.MILLISECONDS.sleep(100);
    Assertions.assertEquals(invokeTimes, serFacade.integer.get());
  }

  @Test
  void echoTest() throws Exception {
    HelloWorld world = proxy.getProxy(address, HelloWorld.class);
    for (int i = 0; i < invokeTimes; i++) {
      world.echo("hi");
    }

    TimeUnit.MILLISECONDS.sleep(100);
    Assertions.assertEquals(invokeTimes, serFacade.integer.get());
    Assertions.assertEquals(invokeTimes, cliFacade.integer.get());
  }


  /** 测试 */
  public static final int TEST_REQ = -1;

  /**
   * 世界你好，门面
   *
   * @author ZJP
   * @since 2021年07月22日 21:58:02
   **/
  @Facade
  private static class SerHelloWorldFacade implements HelloWorld {


    public AtomicInteger integer = new AtomicInteger();


    /**
     * 测试
     */
    @ReqMethod(TEST_REQ)
    public String push(String str) {
      integer.incrementAndGet();
      return str;
    }

    @Override
    public Object echo(Object o) {
      integer.incrementAndGet();
      return o;
    }

    @Override
    public void doNothing() {
      integer.incrementAndGet();
    }
  }

  @Facade
  private static class CliHelloWorldFacade {

    public AtomicInteger integer = new AtomicInteger();

    @ReqMethod(-HelloWorld.ECHO)
    public void echoRes(Object o) {
      integer.incrementAndGet();
    }
  }

}
