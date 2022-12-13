package org.example.proxy.service;

import java.net.InetSocketAddress;
import org.example.proxy.config.ProxyServerConfig;
import org.junit.jupiter.api.Test;

public class ProxyServiceTest {

  @Test
  public void start() {
    ProxyServerConfig serverConfig = new ProxyServerConfig();
    serverConfig.setId(1);
    serverConfig.setSocketAddress(new InetSocketAddress(0));


    ProxyService proxyService = new ProxyService();
    proxyService.setProxyServerConfig(serverConfig);

    proxyService.start(null, 0);

    proxyService.close();
  }

}
