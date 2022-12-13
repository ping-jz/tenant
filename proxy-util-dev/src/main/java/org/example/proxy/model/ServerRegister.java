package org.example.proxy.model;

/**
 * 服务器注册信息
 *
 * @author zhongjianping
 * @since 2022/12/13 14:13
 */
public class ServerRegister {
  /** 服务器ID */
  private int id;
  /** 服务器子ID */
  private int[] subIds;
  /** 验证码 */
  private String password;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int[] getSubIds() {
    return subIds;
  }

  public void setSubIds(int[] subIds) {
    this.subIds = subIds;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
