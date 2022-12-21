package org.example.proxy.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 中转服配置
 *
 * @author zhongjianping
 * @since 2022/12/13 15:32
 */
@Component
public class ProxyServerConfig {

  /** 服务ID */
  @Value("${proxy.id}")
  private int id;
  /** 监听地址 */
  @Autowired(required = false)
  @Value("${proxy.address:}")
  private String address;
  /** 端口 */
  @Value("${proxy.port}")
  private int port;
  /** 注册密码 */
  @Value("${proxy.registerKey}")
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
