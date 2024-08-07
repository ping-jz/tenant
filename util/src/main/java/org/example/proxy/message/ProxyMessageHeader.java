package org.example.proxy.message;

/**
 * 消息中转头部信息，此类不会实际使用。仅作为数据结构展示，方便理解
 * <p>
 * 长度(4byte) + 中转信息头 + 信息 = 一个完整的中转信息
 * <p>
 * 长度=头部长度+信息长度
 *
 * @author zhongjianping
 * @since 17:46
 */
public class ProxyMessageHeader {

  /** 源 */
  private int source;
  /** 目标 */
  private int target;

  public int getSource() {
    return source;
  }

  public void setSource(int source) {
    this.source = source;
  }

  public int getTarget() {
    return target;
  }

  public void setTarget(int target) {
    this.target = target;
  }
}
