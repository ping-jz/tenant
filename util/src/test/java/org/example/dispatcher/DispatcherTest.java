package org.example.dispatcher;

import org.example.dispatcher.facde.HelloWorldFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 请求分发器测试
 *
 * @author ZJP
 * @since 2021年07月22日 22:47:26
 **/
public class DispatcherTest {

  /** 分发器，测试对象 */
  private HandlerRegistry dispatcher;
  /** 测试门面 */
  private HelloWorldFacade facade;

  @BeforeEach
  void init() {
    dispatcher = new HandlerRegistry();
    facade = new HelloWorldFacade();
    dispatcher.registeHandle(facade);
  }

  @Test
  void registeTest() {

  }

}
