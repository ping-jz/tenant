package org.example.net.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.example.net.facde.cross.CrossHelloWorldFacade;
import org.junit.jupiter.api.Test;

public class SendProxyTest {


  @Test
  void findRpcMethodTest() {
    SendProxy proxy = new SendProxy();
    Map<Integer, RpcMetaMethodInfo> rpcMethods = proxy.findRpcMethods(new CrossHelloWorldFacade());
    assertEquals(2, rpcMethods.size());

    RpcMetaMethodInfo echo = rpcMethods.get(110);
    assertEquals(110, echo.id());
    assertEquals("echo", echo.name());

    RpcMetaMethodInfo doNothong = rpcMethods.get(100);
    assertEquals(100, doNothong.id());
    assertEquals("doNothing", doNothong.name());
  }


}
