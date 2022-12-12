package org.example.game.facade.example.model;

/**
 * 移动结果
 *
 * @author ZJP
 * @since 2021年09月27日 15:26:14
 **/
public class ResMove {

  /** 对象ID */
  private long id;
  /** x轴 */
  private float x;
  /** y轴 */
  private float y;
  /** 方向 */
  private int dir;

  public long id() {
    return id;
  }

  public ResMove id(long id) {
    this.id = id;
    return this;
  }

  public float x() {
    return x;
  }

  public ResMove x(float x) {
    this.x = x;
    return this;
  }

  public float y() {
    return y;
  }

  public ResMove y(float y) {
    this.y = y;
    return this;
  }

  public int dir() {
    return dir;
  }

  public ResMove dir(int dir) {
    this.dir = dir;
    return this;
  }
}
