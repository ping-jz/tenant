package org.example.serde;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

public class RecordSerializerTest {

  @Test
  public void registerEmptyTest() {
    new RecordSerializer(Empty.class, new CommonSerializer());
  }

  @Test
  public void registerTest() {
    new RecordSerializer(Compose.class, new CommonSerializer());
  }


  @Test
  public void emptyTest() {
    CommonSerializer commonSerializer = new CommonSerializer();
    commonSerializer.registerRecord(11, Empty.class);

    Empty empty = new Empty();
    ByteBuf buf = Unpooled.buffer();
    commonSerializer.writeObject(buf, empty);

    Empty res = commonSerializer.read(buf);
    assertEquals(empty, res);
  }

  @Test
  public void primityTest() {
    CommonSerializer commonSerializer = new CommonSerializer();
    commonSerializer.registerRecord(12, Empty.class);
    commonSerializer.registerRecord(11, Compose.class);

    Compose abc = new Compose(1, 2L, 3F, "EEEEEE", new Empty());
    ByteBuf buf = Unpooled.buffer();
    commonSerializer.writeObject(buf, abc);

    Compose res = commonSerializer.read(buf);
    assertEquals(abc, res);
  }


  private record Empty() {

  }

  private record Compose(int a, long b, float c, String d, Empty e) {

  }

}
