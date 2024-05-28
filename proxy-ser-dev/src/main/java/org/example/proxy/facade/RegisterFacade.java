package org.example.proxy.facade;

import org.example.net.Connection;
import org.example.net.anno.Req;
import org.example.net.anno.RpcModule;
import org.example.proxy.message.ProxyProtoId;
import org.example.proxy.model.ServerRegister;
import org.example.proxy.service.ProxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 服务器注册
 *
 * @author zhongjianping
 * @since 2022/12/19 18:24
 */
@RpcModule
@Component
public class RegisterFacade {

  @Autowired
  private ProxyService proxyService;

  /**
   * 服务器注册
   *
   * @since 2022/12/19 18:24
   */
  @Req(ProxyProtoId.REGISTER)
  public void register(Connection connection, ServerRegister serverRegister) {
    proxyService.register(connection, serverRegister);
  }

}
