package org.example.net;

import org.example.common.net.server.DefaultServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExceptionTest {

  @Test
  public void illegalPort() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> new DefaultServer(-1));
  }
}
