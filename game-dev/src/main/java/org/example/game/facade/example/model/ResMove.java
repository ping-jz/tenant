package org.example.game.facade.example.model;

import org.example.serde.Serde;

/**
 * 移动结果
 *
 * @author ZJP
 * @since 2021年09月27日 15:26:14
 **/
@Serde
public class ResMove {

  /** 对象ID */
  private long id;
  /** x轴 */
  private float x;
  /** y轴 */
  private float y;
  /** 方向 */
  private int dir;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public float getX() {
    return x;
  }

  public void setX(float x) {
    this.x = x;
  }

  public float getY() {
    return y;
  }

  public void setY(float y) {
    this.y = y;
  }

  public int getDir() {
    return dir;
  }

  public void setDir(int dir) {
    this.dir = dir;
  }
}
