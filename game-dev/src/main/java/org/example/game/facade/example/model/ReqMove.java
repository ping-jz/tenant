package org.example.game.facade.example.model;

import org.example.serde.Serde;

/**
 * 移动请求
 *
 * @author ZJP
 * @since 2021年09月27日 15:18:34
 **/
@Serde
public class ReqMove {

  /** 对象ID */
  private long id;
  /** X 轴 */
  private float x;
  /** Y 轴 */
  private float y;

  private int text;

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

  public int getText() {
    return text;
  }

  public void setText(int text) {
    this.text = text;
  }
}
