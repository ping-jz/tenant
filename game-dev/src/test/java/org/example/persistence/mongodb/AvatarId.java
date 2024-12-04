package org.example.persistence.mongodb;

import java.io.Serializable;
import org.example.util.Identity;

/**
 * 目前看来当作内存对象使用更好，不然就是造孽
 *
 * @author zhongjianping
 * @since 2024/11/18 22:58
 */

record AvatarId(long id) implements Identity, Comparable<AvatarId>, Serializable {

  /**
   * 推荐静态引用
   *
   * @since 2024/11/18 21:43
   */
  public static AvatarId avatarId(long id) {
    return new AvatarId(id);
  }

  @Override
  public int compareTo(AvatarId o) {
    return Long.compare(id, o.id);
  }
}