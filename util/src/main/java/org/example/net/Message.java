package org.example.net;

/**
 * 网络通信协议格式
 *
 * @author ZJP
 * @since 2021年07月24日 14:47:30
 **/
public class Message {

  /** 目标 */
  private int target;
  /** 源 */
  private int source;
  /** 协议编号 (0 < 接收/发送请求,  接收/发送结果 < 0) */
  private int proto;
  /** 序列号(客户端发什么，服务端就返回什么) */
  private int msgId;
  /** 内容 */
  private Object packet;

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

  public Object packet() {
    return packet;
  }

  public Message packet(Object o) {
    this.packet = o;
    return this;
  }

  public Message packet(Object... packet) {
    this.packet = packet;
    return this;
  }

  public Message wrapArray(Object[] o) {
    packet = new Object[]{o};
    return this;
  }

  public int target() {
    return target;
  }

  public Message target(int target) {
    this.target = target;
    return this;
  }

  public Message source(int source) {
    this.source = source;
    return this;
  }

  public int source() {
    return source;
  }

  @Deprecated
  public boolean isSuc() {
    return true;
  }

  @Deprecated
  public boolean isErr() {
    return !isSuc();
  }
}
