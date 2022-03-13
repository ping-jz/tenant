package org.example.net.proxy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.example.common.ThreadCommonResource;
import org.example.net.CrossDispatcher;
import org.example.net.DispatcherHandler;
import org.example.net.Facade;
import org.example.net.HelloWorld;
import org.example.net.InvokeFuture;
import org.example.net.Message;
import org.example.net.MessageStatus;
import org.example.net.ReqMethod;
import org.example.net.ReqModule;
import org.example.net.ResultInvokeFuture;
import org.example.net.client.DefaultClient;
import org.example.net.handler.HandlerRegistry;
import org.example.net.server.DefaultServer;
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

  /** 服务端消息处理 */
  private SerHelloWorldFacade serFacade;
  /**
   * 客户端消息处理
   */
  private CliHelloWorldFacade cliFacade;
  /**
   * 服务端
   */
  private DefaultServer rpcServer;
  /**
   * 客户端
   */
  private DefaultClient rpcClient;
  /**
   * 请求代理
   */
  private ReqCliProxy proxy;
  /** 服务端地址 */
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
    Serializer<Object> serializer = createSerializer();

    {
      rpcServer = new DefaultServer();
      serFacade = new SerHelloWorldFacade();
      HandlerRegistry serverRegistry = new HandlerRegistry();
      serverRegistry.registeHandlers(serverRegistry.findHandler(serFacade));
      rpcServer.handler(new DispatcherHandler(new CrossDispatcher(serverRegistry)));
      rpcServer.codec(serializer);
      rpcServer.start(resource);
      address = rpcServer.ip() + ':' + rpcServer.port();
    }

    {
      rpcClient = new DefaultClient();
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
    serializer.registerSerializer(11, Message.class);
    return serializer;
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
  public void doNothingTest() throws Exception {
    HelloWorld world = proxy.getProxy(address, HelloWorld.class);
    for (int i = 0; i < invokeTimes; i++) {
      world.doNothing();
    }

    TimeUnit.MILLISECONDS.sleep(100);
    Assertions.assertEquals(invokeTimes, serFacade.integer.get());
  }

  @Test
  public void echoTest() throws Exception {
    HelloWorld world = proxy.getProxy(address, HelloWorld.class);
    for (int i = 0; i < invokeTimes; i++) {
      world.echo("hi");
    }

    TimeUnit.MILLISECONDS.sleep(100);
    Assertions.assertEquals(invokeTimes, serFacade.integer.get());
    Assertions.assertEquals(invokeTimes, cliFacade.integer.get());
  }

  @Test
  public void callBackArgsTest() throws InterruptedException {
    CallBackReq req = proxy.getProxy(address, CallBackReq.class);
    CountDownLatch latch = new CountDownLatch(invokeTimes);
    long answer = 2012;
    for (int i = 0; i < invokeTimes; i++) {
      req.callBackArgs("Hi", i, answer).onSuc(l -> {
        Assertions.assertEquals(answer, l);
        latch.countDown();
      }).invoke();
    }

    Assertions.assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
    Assertions.assertEquals(invokeTimes, serFacade.integer.get());
  }

  @Test
  public void callBackArgsMessageTest() throws InterruptedException {
    CallBackReq req = proxy.getProxy(address, CallBackReq.class);
    CountDownLatch latch = new CountDownLatch(invokeTimes);
    long answer = 2012;
    for (int i = 0; i < invokeTimes; i++) {
      req.callBackArgs("Hi", i, answer).onSuc(msg -> {
        Assertions.assertEquals(answer, msg);
        latch.countDown();
      }).invoke();
    }

    Assertions.assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
    Assertions.assertEquals(invokeTimes, serFacade.integer.get());
  }

  @Test
  public void callBackArrayMessageTest() throws InterruptedException {
    CallBackReq req = proxy.getProxy(address, CallBackReq.class);
    CountDownLatch latch = new CountDownLatch(invokeTimes);
    long[] longs = {1, 2, 3, 4, 5, 6};
    for (int i = 0; i < invokeTimes; i++) {
      req.callBackArray(longs).onSuc(msg -> {
        Assertions.assertArrayEquals(longs, msg);
        latch.countDown();
      }).invoke();
    }

    Assertions.assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
    Assertions.assertEquals(invokeTimes, serFacade.integer.get());
  }

  @Test
  public void calErrMessageTest() throws InterruptedException {
    CallBackReq req = proxy.getProxy(address, CallBackReq.class);
    CountDownLatch latch = new CountDownLatch(invokeTimes);
    for (int i = 0; i < invokeTimes; i++) {
      req.errMsg().onErr(msg -> {
        Assertions.assertTrue(msg.isErr());
        Assertions.assertEquals(MessageStatus.SERVER_EXCEPTION.status(), msg.status());
        latch.countDown();
      }).invoke();
    }

    Assertions.assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
    Assertions.assertEquals(invokeTimes, serFacade.integer.get());
  }

  /** 回调 */
  private static final int CALL_BACK = 200;

  @ReqModule(CALL_BACK)
  interface CallBackReq {

    InvokeFuture<String> callBack(String str);

    InvokeFuture<Long> callBackArgs(String str, Integer i, Long a);

    InvokeFuture<long[]> callBackArray(long[] longs);

    InvokeFuture<Message> errMsg();
  }

  /**
   * 世界你好，门面
   *
   * @author ZJP
   * @since 2021年07月22日 21:58:02
   **/
  @Facade
  private static class SerHelloWorldFacade implements HelloWorld, CallBackReq {

    public AtomicInteger integer = new AtomicInteger();

    @Override
    public Object echo(Object o) {
      integer.incrementAndGet();
      return o;
    }

    @Override
    public void doNothing() {
      integer.incrementAndGet();
    }

    @Override
    public InvokeFuture<String> callBack(String str) {
      integer.incrementAndGet();
      return ResultInvokeFuture.withResult(str);
    }

    @Override
    public InvokeFuture<Long> callBackArgs(String str, Integer i, Long a) {
      integer.incrementAndGet();
      return ResultInvokeFuture.withResult(a);
    }

    @Override
    public InvokeFuture<long[]> callBackArray(long[] longs) {
      integer.incrementAndGet();
      return ResultInvokeFuture.withResult(longs);
    }

    @Override
    public InvokeFuture<Message> errMsg() {
      integer.incrementAndGet();
      Message message = Message.of().status(MessageStatus.SERVER_EXCEPTION);
      return ResultInvokeFuture.withResult(message);
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
