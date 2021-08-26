package org.example.serde;

import io.netty.buffer.ByteBuf;

/**
 * 标记序列化(配合CommonSerializer,单纯填充标记)，用来解决Array的ComponentType
 *
 * @author ZJP
 * @since 2021年08月26日 15:33:04
 **/
public class MarkSerializer implements Serializer<ObjectSerializer> {

  @Override
  public ObjectSerializer readObject(ByteBuf buf) {
    return null;
  }

  @Override
  public void writeObject(ByteBuf buf, ObjectSerializer object) {
  }
}
