package org.example.net.proxy;


import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class RpcCliProxyTest {


  @Test
  void exceptionTest() {
    assertThrows(IllegalArgumentException.class, () -> RpcCliProxy.registerRpcMethods(
        List.class));

    assertThrows(IllegalArgumentException.class, () -> RpcCliProxy.registerRpcMethods(
        ArrayList.class));
  }


}
