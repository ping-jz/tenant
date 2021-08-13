package org.example.net;

import org.example.net.server.RpcServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExceptionTest {

  @Test
  void illegalPort() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> new RpcServer(-1));
  }

}
