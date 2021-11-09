package org.example.net.proxy;

import java.util.ArrayList;
import java.util.List;
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

  /** 服务端消息处理 */
  private SerHelloWorldFacade serFacade;
  /** 客户端消息处理 */
  private CliHelloWorldFacade cliFacade;
  /** 服务端 */
  private ReqServer rpcServer;
  /** 客户端 */
  private ReqClient rpcClient;
  /** 请求代理 */
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
  public void futureTest() throws InterruptedException {
    String hi = "Hi";
    CallBackReq req = proxy.getProxy(address, CallBackReq.class);
    List<InvokeFuture<String>> futures = new ArrayList<>(invokeTimes);

    for (int i = 0; i < invokeTimes; i++) {
      futures.add(req.callBack(hi));
    }

    for (InvokeFuture<String> callBack : futures) {
      Message message = callBack.waitResponse();
      Assertions.assertTrue(message.isSuc());
      Assertions.assertEquals(hi, message.packet());
    }

    Assertions.assertEquals(invokeTimes, serFacade.integer.get());
  }

  @Test
  public void futureArgsTest() throws InterruptedException {
    CallBackReq req = proxy.getProxy(address, CallBackReq.class);
    List<InvokeFuture<Long>> futures = new ArrayList<>(invokeTimes);

    for (int i = 0; i < invokeTimes; i++) {
      futures.add(req.callBackArgs("Hi", i, 2012L));
    }

    for (InvokeFuture<Long> callBack : futures) {
      Message message = callBack.waitResponse();
      Assertions.assertTrue(message.isSuc());
      Assertions.assertEquals(2012L, message.packet());
    }

    Assertions.assertEquals(invokeTimes, serFacade.integer.get());
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
      });
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
      req.callBackArgs("Hi", i, answer).onSucMsg(msg -> {
        Assertions.assertTrue(msg.isSuc());
        Assertions.assertEquals(answer, msg.packet());
        latch.countDown();
      });
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
      req.callBackArray(longs).onSucMsg(msg -> {
        Assertions.assertTrue(msg.isSuc());
        Assertions.assertArrayEquals(longs, (long[]) msg.packet());
        latch.countDown();
      });
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
      });
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
      return InvokeFuture.withResult(str);
    }

    @Override
    public InvokeFuture<Long> callBackArgs(String str, Integer i, Long a) {
      integer.incrementAndGet();
      return InvokeFuture.withResult(a);
    }

    @Override
    public InvokeFuture<long[]> callBackArray(long[] longs) {
      integer.incrementAndGet();
      return InvokeFuture.withResult(longs);
    }

    @Override
    public InvokeFuture<Message> errMsg() {
      integer.incrementAndGet();
      Message message = Message.of().status(MessageStatus.SERVER_EXCEPTION);
      return InvokeFuture.withResult(message);
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
