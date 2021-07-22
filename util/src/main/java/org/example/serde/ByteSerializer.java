package org.example.serde;

import io.netty.buffer.ByteBuf;

/**
 * Byte序列化实现
 *
 * 与{@link CommonSerializer} 组合使用
 *
 * @since 2021年07月17日 16:16:14
 **/
public class ByteSerializer implements Serializer<Byte> {

  @Override
  public Byte readObject(ByteBuf buf) {
    return buf.readByte();
  }

  @Override
  public void writeObject(ByteBuf buf, Byte object) {
    buf.writeByte(object);
  }
}
