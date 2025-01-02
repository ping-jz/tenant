package org.example.serde;

import io.netty.buffer.ByteBuf;

/**
 * Integer序列化实现(使用varint32和ZigZag32进行编码)
 *
 * 与{@link CommonSerializer} 组合使用,null会被0代理
 *
 * @since 2021年07月17日 16:16:14
 **/
public class LongSerializer implements Serializer<Long> {

  @Override
  public Long readObject(CommonSerializer serializer, ByteBuf buf) {
    return NettyByteBufUtil.readInt64(buf);
  }

  @Override
  public void writeObject(CommonSerializer serializer, ByteBuf buf, Long object) {
    NettyByteBufUtil.writeInt64(buf, object);
  }
}
