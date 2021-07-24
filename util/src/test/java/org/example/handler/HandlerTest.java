package org.example.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.example.handler.facde.HelloWorldFacade;
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
  private static HandlerRegistry dispatcher;
  /** 测试门面 */
  private static HelloWorldFacade facade;

  @BeforeAll
  static void init() {
    dispatcher = new HandlerRegistry();
    facade = new HelloWorldFacade();
    List<Handler> handlers = dispatcher.findHandler(facade);
    assertEquals(1, handlers.size());

    dispatcher.registeHandlers(handlers);
  }

  @Test
  void registeTest() throws Exception {
    Handler echoHandler = dispatcher.getHandler(HelloWorldFacade.ECHO_REQ);
    assertNotNull(echoHandler);

    String hi = "Hi";
    assertEquals(hi, echoHandler.invoke(hi));
  }

  @Test
  void duplicateRegisteTest() {
    Handler old = dispatcher.getHandler(HelloWorldFacade.ECHO_REQ);

    assertThrows(RuntimeException.class,
        () -> dispatcher.registeHandlers(dispatcher.findHandler(facade)));

    //确保重复注册不会破坏之前的关系
    assertEquals(old, dispatcher.getHandler(HelloWorldFacade.ECHO_REQ));
  }

  @Test
  void noRegisteTest() {
    assertNull(dispatcher.getHandler(HelloWorldFacade.ECHO_REQ + 1));
  }

}
