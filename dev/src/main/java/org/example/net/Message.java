package org.example.net;

/**
 * 网络通信协议格式
 *
 * @author ZJP
 * @since 2021年07月24日 14:47:30
 **/
public class Message {

  /** 协议编号 (0 < 为收到/发送请求,  收到/发送结果 < 0) */
  private int proto;
  /** 序列号(客户端发什么，服务端就返回什么) */
  private int reqId;
  /** 内容 */
  private Object packet;


  public int proto() {
    return proto;
  }

  public Message proto(int proto) {
    this.proto = proto;
    return this;
  }

  public int optIdx() {
    return reqId;
  }

  public Message optIdx(int optIdx) {
    this.reqId = optIdx;
    return this;
  }

  public Object packet() {
    return packet;
  }

  public Message packet(Object packet) {
    this.packet = packet;
    return this;
  }
}
