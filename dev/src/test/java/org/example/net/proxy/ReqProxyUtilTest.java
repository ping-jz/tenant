package org.example.net.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;
import java.util.List;
import org.example.net.HelloWorld;
import org.example.net.ReqMethod;
import org.example.net.ReqModule;
import org.example.util.Pair;
import org.junit.jupiter.api.Test;

public class ReqProxyUtilTest {

  @Test
  void calcProtoIdsTest() {
    List<Pair<Integer, Method>> methods = ReqProxyUtil.calcReqMethods(HelloWorld.class);
    assertEquals(2, methods.size());
    assertEquals(HelloWorld.HELLO_WORLD_MODULE, methods.get(0).first());
    assertEquals(HelloWorld.ECHO, methods.get(1).first());
  }

  @Test
  void invalidReqTest() {
    assertThrows(RuntimeException.class, () -> ReqProxyUtil.calcReqMethods(InvalidReq.class));
    assertThrows(RuntimeException.class, () -> ReqProxyUtil.calcReqMethods(DuplicatedReq.class));
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
