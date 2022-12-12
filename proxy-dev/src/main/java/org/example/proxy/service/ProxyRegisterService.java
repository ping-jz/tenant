package org.example.proxy.service;

import io.netty.channel.Channel;
import org.example.proxy.model.ServerRegister;

/**
 * 服务注册
 *
 * @author zhongjianping
 * @since 2022/12/12 22:55
 */
public class ProxyRegisterService {

  /**
   * 服务链接中转服的首条首条协议必须是注册协议，否则当作恶意链接关闭
   *
   * @param channel 网络链接
   * @param register 服务信息和链接验证
   * @since 2022/12/12 22:58
   */
  public void register(Channel channel, ServerRegister register, ProxyService proxyService) {



  }
}
