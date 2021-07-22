package org.example.serde;

import io.netty.buffer.ByteBuf;

/**
 * null默认实现
 *
 * 与{@link CommonSerializer} 组合使用，提供完成功能
 *
 * @since 2021年07月17日 17:02:35
 **/
public final class NullSerializer implements Serializer<Object> {

  public static NullSerializer INSTANCE = new NullSerializer();

  private NullSerializer() {

  }

  @Override
  public Object readObject(ByteBuf buf) {
    return null;
  }

  @Override
  public void writeObject(ByteBuf buf, Object object) {
  }
}
