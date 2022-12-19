package org.example.net.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.example.net.HelloWorld;
import org.example.net.ReqUtil;
import org.example.net.anno.ReqMethod;
import org.example.net.anno.RpcModule;
import org.junit.jupiter.api.Test;

public class ReqProxyUtilTest {

  @Test
  public void calcModuleTest() {
    List<Pair<Integer, Method>> methods = ReqUtil.getMethods(HelloWorld.class);
    assertEquals(2, methods.size());
    assertEquals(HelloWorld.ECHO, methods.get(1).getLeft());
  }

  @Test
  public void invalidReqTest() {
    assertThrows(IllegalArgumentException.class, () -> ReqUtil.getMethods(InvalidReq.class));
    assertThrows(IllegalArgumentException.class, () -> ReqUtil.getMethods(DuplicatedReq.class));
  }

  @RpcModule
  private interface InvalidReq {

    @ReqMethod(-99999)
    void invalid();
  }

  @RpcModule
  private interface DuplicatedReq {

    @ReqMethod(1)
    void one();

    @ReqMethod(1)
    void two();
  }
}
