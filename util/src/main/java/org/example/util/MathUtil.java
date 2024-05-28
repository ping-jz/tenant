package org.example.util;

/**
 * 数据工具
 *
 * @author zhongjianping
 * @since 2023/8/28 21:02
 */
public class MathUtil {

  /**
   * 计算线段的角度
   *
   * @since 2023年08月28日“ 20:15
   */
  public static int dir(float x1, float y1, float x2, float y2) {
    double degree = Math.toDegrees(Math.atan2(x2 - x1, y2 - y1));
    if (degree < 0) {
      degree += 360;
    }

    return (int) degree;
  }
}
