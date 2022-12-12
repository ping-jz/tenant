package org.example.net.proxy;

import java.lang.reflect.Method;
import java.util.Objects;
import org.example.util.Identity;

/**
 * 远程方法基本信息
 *
 * @author ZJP
 * @since 2021年07月25日 15:36:19
 **/
public class ReqMetaMethodInfo implements Identity<Integer> {

  /**
   * 唯一调用ID
   */
  private Integer id;
  /**
   * 方法名
   */
  private Method method;

  @Override
  public Integer id() {
    return id;
  }

  public ReqMetaMethodInfo id(Integer id) {
    this.id = id;
    return this;
  }

  public Method method() {
    return method;
  }

  public ReqMetaMethodInfo method(Method method) {
    this.method = method;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReqMetaMethodInfo info = (ReqMetaMethodInfo) o;
    return Objects.equals(id, info.id) && Objects.equals(method, info.method);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, method);
  }
}
