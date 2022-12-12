package org.example.proxy.service;


import java.nio.channels.Channel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 代理服务(主要负责消息中转)
 *
 * @author zhongjianping
 * @since 2022/12/12 23:00
 */
public class ProxyService {

  /** 所有链接， 服务ID -> Netty网络链接 */
  private Map<Integer, Channel> channels;


  public ProxyService() {
    channels = new ConcurrentHashMap<>();
  }
}
