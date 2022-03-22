package org.example.serde;

import io.netty.buffer.ByteBuf;

/**
 * Integer序列化实现(使用varint32和ZigZag32进行编码)
 *
 * 与{@link CommonSerializer} 组合使用, null会被0代理
 *
 * @since 2021年07月17日 16:16:14
 **/
public class IntegerSerializer implements Serializer<Integer> {

  @Override
  public Integer readObject(ByteBuf buf) {
    return NettyByteBufUtil.readInt32(buf);
  }

  @Override
  public void writeObject(ByteBuf buf, Integer object) {
    NettyByteBufUtil.writeInt32(buf, object == null ? 0 : object);
  }
}
