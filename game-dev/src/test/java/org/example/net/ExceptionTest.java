package org.example.net;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import org.example.common.net.server.DefaultServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExceptionTest {

  @Test
  public void illegalPort() {
    Kryo kryo = new Kryo();
    kryo.register(SomeClass.class);
    kryo.register(int[][].class);

    SomeClass someClass = new SomeClass();
    kryo.writeObject(new ByteBufferOutput(), someClass);
  }

  public static class SomeClass {

    private int[][] arrays = new int[][]{
        {1}, {0}
    };
  }
}
