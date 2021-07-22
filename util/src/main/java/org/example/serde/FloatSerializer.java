package org.example.serde;

import io.netty.buffer.ByteBuf;

/**
 * Float序列化实现
 *
 * 与{@link CommonSerializer} 组合使用
 *
 * @since 2021年07月17日 16:16:14
 **/
public class FloatSerializer implements Serializer<Float> {

  @Override
  public Float readObject(ByteBuf buf) {
    return buf.readFloat();
  }

  @Override
  public void writeObject(ByteBuf buf, Float object) {
    buf.writeFloat(object);
  }
}
