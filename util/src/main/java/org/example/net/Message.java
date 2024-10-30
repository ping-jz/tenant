package org.example.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCounted;
import java.util.Objects;

/**
 * 网络通信协议格式
 *
 * @author ZJP
 * @since 2021年07月24日 14:47:30
 **/
public class Message implements ReferenceCounted {

  /** 协议编号 (0 < [接收/发送]请求,  [接收/发送]结果 < 0) */
  private int proto;
  /** 序列号(客户端发什么，服务端就返回什么) */
  private int msgId;
  /** 内容 */
  private ByteBuf packet = Unpooled.EMPTY_BUFFER;

  public Message() {
  }

  public static Message of(int proto, int msgId, byte[] packet) {
    Message message = new Message();
    message.proto = proto;
    message.msgId = msgId;
    message.packet = Unpooled.wrappedBuffer(packet);
    return message;
  }

  public static Message of(int proto, int msgId, ByteBuf packet) {
    Message message = new Message();
    message.proto = proto;
    message.msgId = msgId;
    message.packet = packet;
    return message;
  }

  public int proto() {
    return proto;
  }


  public int msgId() {
    return msgId;
  }

  public ByteBuf packet() {
    return packet;
  }


  @Deprecated
  public boolean isSuc() {
    return true;
  }

  @Deprecated
  public boolean isErr() {
    return !isSuc();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Message message = (Message) o;
    return proto == message.proto && msgId == message.msgId && Objects.deepEquals(packet,
        message.packet);
  }

  @Override
  public int hashCode() {
    return Objects.hash(proto, msgId, packet);
  }

  @Override
  public int refCnt() {
    return packet.refCnt();
  }

  @Override
  public ReferenceCounted retain() {
     packet.retain();
     return this;
  }

  @Override
  public ReferenceCounted retain(int increment) {
    packet.retain(increment);
    return this;
  }

  @Override
  public ReferenceCounted touch() {
    packet.touch();
    return this;
  }

  @Override
  public ReferenceCounted touch(Object hint) {
    packet.touch();
    return this;
  }

  @Override
  public boolean release() {
    msgId = 0;
    proto = 0;
    return packet.release();
  }

  @Override
  public boolean release(int decrement) {
    return packet.release(decrement);
  }
}
