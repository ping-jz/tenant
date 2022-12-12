package org.example.net;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.example.net.server.DefaultServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExceptionTest {

  @Test
  public void illegalPort() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> new DefaultServer(-1));
  }
}
