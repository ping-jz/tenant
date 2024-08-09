package org.example.net;

import java.util.Arrays;
import java.util.Objects;
import org.apache.commons.lang3.ArrayUtils;

/**
 * 网络通信协议格式
 *
 * @author ZJP
 * @since 2021年07月24日 14:47:30
 **/
public class Message {

  /** 协议编号 (0 < [接收/发送]请求,  [接收/发送]结果 < 0) */
  private int proto;
  /** 序列号(客户端发什么，服务端就返回什么) */
  private int msgId;
  /** 内容 */
  private byte[] packet = ArrayUtils.EMPTY_BYTE_ARRAY;

  public Message() {
  }

  public static Message of() {
    return new Message();
  }

  public static Message of(int proto) {
    return new Message().proto(proto);
  }

  public int proto() {
    return proto;
  }

  public Message proto(int proto) {
    this.proto = proto;
    return this;
  }

  public int msgId() {
    return msgId;
  }

  public Message msgId(int optIdx) {
    msgId = optIdx;
    return this;
  }

  public byte[] packet() {
    return packet;
  }


  public Message packet(byte[] packet) {
    this.packet = packet;
    return this;
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
    return Objects.hash(proto, msgId, Arrays.hashCode(packet));
  }
}
