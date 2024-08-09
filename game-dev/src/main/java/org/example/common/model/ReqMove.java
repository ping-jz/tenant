package org.example.common.model;

import java.util.Objects;
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

  @Override
  public String toString() {
    return "ReqMove{" +
        "id=" + id +
        ", x=" + x +
        ", y=" + y +
        ", text=" + text +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReqMove reqMove = (ReqMove) o;
    return getId() == reqMove.getId() && Float.compare(getX(), reqMove.getX()) == 0
        && Float.compare(getY(), reqMove.getY()) == 0 && getText() == reqMove.getText();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getX(), getY(), getText());
  }
}
