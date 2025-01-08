package org.example.serde;

import io.netty.buffer.ByteBuf;

/**
 * Double序列化实现
 *
 * 与{@link Serdes} 组合使用, null会被0代理
 *
 * @since 2021年07月17日 16:16:14
 **/
public class DoubleSerializer implements Serializer<Double> {

  @Override
  public Double readObject(Serdes serializer, ByteBuf buf) {
    return buf.readDouble();
  }

  @Override
  public void writeObject(Serdes serializer, ByteBuf buf, Double object) {
    buf.writeDouble(object);
  }
}
