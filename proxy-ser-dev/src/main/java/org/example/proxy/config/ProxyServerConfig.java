package org.example.proxy.config;


/**
 * 中转服配置
 *
 * @author zhongjianping
 * @since 2022/12/13 15:32
 */
public class ProxyServerConfig {

  /** 服务ID */
  private int id;
  /** 监听地址 */
  private String address;
  /** 端口 */
  private int port;
  /** 注册密码 */
  private String registerKey = "none";

  public String getRegisterKey() {
    return registerKey;
  }

  public void setRegisterKey(String registerKey) {
    this.registerKey = registerKey;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }
}
