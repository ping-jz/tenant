package org.example.net.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.example.net.facde.cross.HelloWorld;
import org.junit.jupiter.api.Test;

public class RpcClientProxyTest {


  /**
   * 验证能否正确注册rpc调用方法信息
   */
  @Test
  void findRpcMethodTest() {
    RpcClientProxy proxy = new RpcClientProxy();
    Map<String, RpcMetaMethodInfo> rpcMethods = proxy.registerRpcMethods(HelloWorld.class);
    assertEquals(2, rpcMethods.size());

    RpcMetaMethodInfo echo = rpcMethods.get("echo");
    RpcMetaMethodInfo echoTwo = proxy.getRpcMetaMethodInfo(HelloWorld.class, "echo");
    assertEquals(echoTwo, echo);

    RpcMetaMethodInfo doNothing = rpcMethods.get("doNothing");
    RpcMetaMethodInfo doNothingTwo = proxy.getRpcMetaMethodInfo(HelloWorld.class, "doNothing");
    assertEquals(doNothingTwo, doNothing);
  }

  @Test
  void registerMethodTest() {

  }


}
