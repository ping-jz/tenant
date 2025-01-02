package org.example.serde;

import org.example.serde.array.BooleanArraySerializer;
import org.example.serde.array.ByteArraySerializer;
import org.example.serde.array.CharArraySerializer;
import org.example.serde.array.DoubleArraySerializer;
import org.example.serde.array.FloatArraySerializer;
import org.example.serde.array.IntArraySerializer;
import org.example.serde.array.LongArraySerializer;
import org.example.serde.array.ShortArraySerializer;
import org.example.serde.array.StringArraySerializer;

public class DefaultSerializersRegister {


  /**
   * 注册常用的类型解析器
   *
   * @since 2021年07月19日 23:00:35
   */
  public void register(CommonSerializer commonSerializer) {
    commonSerializer.registerSerializer(0, NullSerializer.class, new NullSerializer());
    commonSerializer.registerSerializer(1, Byte.class, new ByteSerializer());
    commonSerializer.registerSerializer(2, Boolean.class, new BooleanSerializer());
    commonSerializer.registerSerializer(3, Short.class, new ShortSerializer());
    commonSerializer.registerSerializer(4, Integer.class, new IntegerSerializer());
    commonSerializer.registerSerializer(5, Long.class, new LongSerializer());
    commonSerializer.registerSerializer(6, Float.class, new FloatSerializer());
    commonSerializer.registerSerializer(7, Double.class, new DoubleSerializer());
    commonSerializer.registerSerializer(9, Character.class, new CharacterSerializer());
    commonSerializer.registerSerializer(10, String.class, new StringSerializer());
    commonSerializer.registerSerializer(12, byte[].class, new ByteArraySerializer());
    commonSerializer.registerSerializer(13, boolean[].class, new BooleanArraySerializer());
    commonSerializer.registerSerializer(14, short[].class, new ShortArraySerializer());
    commonSerializer.registerSerializer(15, char[].class, new CharArraySerializer());
    commonSerializer.registerSerializer(16, float[].class, new FloatArraySerializer());
    commonSerializer.registerSerializer(17, double[].class, new DoubleArraySerializer());
    commonSerializer.registerSerializer(18, int[].class, new IntArraySerializer());
    commonSerializer.registerSerializer(19, long[].class, new LongArraySerializer());
    commonSerializer.registerSerializer(20, String[].class, new StringArraySerializer());
  }

}
