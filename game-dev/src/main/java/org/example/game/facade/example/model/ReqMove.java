package org.example.game.facade.example.model;

/**
 * 移动请求
 *
 * @author ZJP
 * @since 2021年09月27日 15:18:34
 **/
public class ReqMove {

  /** 对象ID */
  private long id;
  /** X 轴 */
  private float x;
  /** Y 轴 */
  private float y;

  public long id() {
    return id;
  }

  public ReqMove id(long id) {
    this.id = id;
    return this;
  }

  public float x() {
    return x;
  }

  public ReqMove x(float x) {
    this.x = x;
    return this;
  }

  public float y() {
    return y;
  }

  public ReqMove y(float y) {
    this.y = y;
    return this;
  }
}
