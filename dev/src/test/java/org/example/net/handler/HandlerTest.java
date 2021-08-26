package org.example.net.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.example.net.Facade;
import org.example.net.HelloWorld;
import org.example.net.ReqMethod;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 请求分发器测试
 *
 * @author ZJP
 * @since 2021年07月22日 22:47:26
 **/
public class HandlerTest {

  /** 分发器，测试对象 */
  private static HandlerRegistry registry;
  /** 测试门面 */
  private static HelloWorldFacade facade;

  @BeforeAll
  static void init() {
    registry = new HandlerRegistry();
    facade = new HelloWorldFacade();
    List<Handler> handlers = registry.findHandler(facade);
    assertEquals(3, handlers.size());

    registry.registeHandlers(handlers);
  }

  @Test
  void facadeTest() throws Exception {
    Handler echoHandler = registry.getHandler(HelloWorldFacade.TEST_REQ);
    assertNotNull(echoHandler);

    String hi = "Hi";
    assertEquals(hi, echoHandler.invoke(hi));
  }

  @Test
  void moduleTest() throws Exception {
    Handler echoHandler = registry.getHandler(HelloWorld.ECHO);
    Handler doNothing = registry.getHandler(HelloWorld.DO_NOTHING);
    assertNotNull(echoHandler);
    assertNotNull(doNothing);

    String hi = "Hi";
    assertEquals(hi, echoHandler.invoke(hi));

    doNothing.invoke();
  }

  @Test
  void duplicateRegistryTest() {
    Handler old = registry.getHandler(HelloWorldFacade.TEST_REQ);

    assertThrows(RuntimeException.class,
        () -> registry.registeHandlers(registry.findHandler(facade)));

    //确保重复注册不会破坏之前的关系
    assertEquals(old, registry.getHandler(HelloWorldFacade.TEST_REQ));
  }

  @Test
  void noRegistryTest() {
    assertNull(registry.getHandler(HelloWorldFacade.TEST_REQ + 1));
  }

  /**
   * 世界你好，门面
   *
   * @author ZJP
   * @since 2021年07月22日 21:58:02
   **/
  @Facade
  private static class HelloWorldFacade implements HelloWorld {

    /** 测试 */
    public static final int TEST_REQ = 1;


    /**
     * 测试
     *
     * @param str 内容
     * @since 2021年07月22日 21:58:45
     */
    @ReqMethod(TEST_REQ)
    public String test(String str) {
      return str;
    }

    @Override
    public Object echo(Object o) {
      return o;
    }

    @Override
    public void doNothing() {

    }
  }
}
