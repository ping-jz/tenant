package org.example.serde.array;

import io.netty.buffer.ByteBuf;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import org.example.serde.Serdes;
import org.example.serde.Serializer;

public class StringArraySerializer implements Serializer<String[]> {

  public StringArraySerializer() {

  }


  @Override
  public String[] readObject(Serdes serializer, ByteBuf buf) {
    int length = serializer.readVarInt32(buf);
    if (length < 0) {
      return null;
    }

    String[] array = new String[length];
    for (int i = 0; i < length; ++i) {
      int strLength = serializer.readVarInt32(buf);
      if (0 <= strLength) {
        array[i] = buf.readCharSequence(strLength, StandardCharsets.UTF_8).toString();
      }
    }

    return array;
  }


  @Override
  public void writeObject(Serdes serializer, ByteBuf buf, String[] object) {
    if (object == null) {
      serializer.writeVarInt32(buf, Integer.MIN_VALUE);
      return;
    }

    final int length = Array.getLength(object);
    serializer.writeVarInt32(buf, length);

    for (String s : object) {
      if (s == null) {
        serializer.writeVarInt32(buf, Integer.MIN_VALUE);
      } else {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        serializer.writeVarInt32(buf, bytes.length);
        buf.writeBytes(bytes);
      }
    }
  }
}
