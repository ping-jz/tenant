package org.example.proxy.config;


import java.net.SocketAddress;

/**
 * 中转服配置
 *
 * @author zhongjianping
 * @since 2022/12/13 15:32
 */
public class ProxyServerConfig {

  /** 服务ID */
  private int id;
  private SocketAddress socketAddress;
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

  public SocketAddress getSocketAddress() {
    return socketAddress;
  }

  public void setSocketAddress(SocketAddress socketAddress) {
    this.socketAddress = socketAddress;
  }
}
