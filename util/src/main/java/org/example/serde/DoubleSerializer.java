package org.example.serde;

import io.netty.buffer.ByteBuf;

/**
 * Double序列化实现
 *
 * 与{@link CommonSerializer} 组合使用
 *
 * @since 2021年07月17日 16:16:14
 **/
public class DoubleSerializer implements Serializer<Double> {

  @Override
  public Double readObject(ByteBuf buf) {
    return buf.readDouble();
  }

  @Override
  public void writeObject(ByteBuf buf, Double object) {
    buf.writeDouble(object);
  }
}
