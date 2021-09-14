package org.example.net;

import org.example.net.server.ReqServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExceptionTest {

  @Test
  public void illegalPort() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> new ReqServer(-1));
  }

}
