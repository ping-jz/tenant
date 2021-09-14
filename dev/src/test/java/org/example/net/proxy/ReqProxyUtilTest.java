package org.example.net.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;
import java.util.List;
import org.example.net.Facade;
import org.example.net.HelloWorld;
import org.example.net.ReqMethod;
import org.example.net.ReqModule;
import org.example.util.Pair;
import org.junit.jupiter.api.Test;

public class ReqProxyUtilTest {

  @Test
  public void calcModuleTest() {
    List<Pair<Integer, Method>> methods = ReqUtil.calcModuleMethods(HelloWorldFacade.class.getInterfaces()[0]);
    assertEquals(2, methods.size());
    assertEquals(HelloWorld.HELLO_WORLD_MODULE, methods.get(0).first());
    assertEquals(HelloWorld.ECHO, methods.get(1).first());
  }

  @Test
  public void calcFacadeTest() {
    List<Pair<Integer, Method>> methods = ReqUtil.calcFacadeMethods(HelloWorldFacade.class);
    assertEquals(1, methods.size());
    assertEquals(HelloWorldFacade.TEST_REQ, methods.get(0).first());
  }

  @Test
  public void invalidReqTest() {
    assertThrows(RuntimeException.class, () -> ReqUtil.calcModuleMethods(InvalidReq.class));
    assertThrows(RuntimeException.class, () -> ReqUtil.calcModuleMethods(DuplicatedReq.class));
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

  @ReqModule(0)
  private interface InvalidReq {

    @ReqMethod(99999)
    void invalid();
  }

  @ReqModule(0)
  private interface DuplicatedReq {

    @ReqMethod(1)
    void one();

    @ReqMethod(1)
    void two();
  }
}
