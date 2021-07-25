package org.example.net.proxy;

import java.util.Objects;

/**
 * 远程方法基本信息
 *
 * @author ZJP
 * @since 2021年07月25日 15:36:19
 **/
public class RpcMetaMethodInfo {

  /** 唯一调用ID */
  private int id;
  /** 方法名 */
  private String name;

  public int id() {
    return id;
  }

  public RpcMetaMethodInfo id(int id) {
    this.id = id;
    return this;
  }

  public String name() {
    return name;
  }

  public RpcMetaMethodInfo name(String name) {
    this.name = name;
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
    RpcMetaMethodInfo info = (RpcMetaMethodInfo) o;
    return id == info.id && Objects.equals(name, info.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }
}
