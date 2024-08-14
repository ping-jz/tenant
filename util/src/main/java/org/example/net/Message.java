package org.example.net;

import java.util.Arrays;
import java.util.Objects;
import org.apache.commons.lang3.ArrayUtils;

/**
 * 网络通信协议格式 //TODO packet可以使用Pooled吗? //TODO 借助Netty的环境，实现自动释放
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

  public static Message of(int proto, int msgId, byte[] packet) {
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

  public byte[] packet() {
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
    return Objects.hash(proto, msgId, Arrays.hashCode(packet));
  }
}
