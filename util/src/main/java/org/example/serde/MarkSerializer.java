package org.example.serde;

import io.netty.buffer.ByteBuf;

/**
 * 用来做类型提示的，不能实列化
 * @author zhongjianping
 * @since 2024/8/9 9:27
 */
public class MarkSerializer implements Serializer<Object> {

  @Override
  public Object readObject(ByteBuf buf) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeObject(ByteBuf buf, Object object) {
  }
}
