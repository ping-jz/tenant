package org.example.proxy.service;


import io.netty.channel.Channel;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.example.net.Connection;
import org.example.proxy.config.ProxyServerConfig;
import org.example.proxy.model.ServerRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 代理服务(主要负责消息中转)
 *
 * @author zhongjianping
 * @since 2022/12/12 23:00
 */
@Component
public class ProxyService {

  /** 日志 */
  private Logger logger = LoggerFactory.getLogger(this.getClass());
  /** 服务器配置 */
  @Autowired
  private ProxyServerConfig proxyServerConfig;
  /** 所有链接， 服务ID -> Netty网络链接 */
  private Map<Integer, Channel> channels;


  public ProxyService() {
    channels = new ConcurrentHashMap<>();
  }

  /**
   * 服务链接中转服的首条首条协议必须是注册协议，否则当作恶意链接关闭
   *
   * @param connection 网络链接
   * @since 2022/12/12 22:58
   */
  public boolean register(Connection connection, ServerRegister serverRegister) {
    boolean isSuc = doRegister(connection, serverRegister);
    if (isSuc) {
      logger.error("[proxy]服务器ID:{}, 子服务器:{}, 注册成功", serverRegister.getId(),
          Arrays.toString(serverRegister.getSubIds()));
    } else {
      channels.remove(serverRegister.getId());
      if (ArrayUtils.isNotEmpty(serverRegister.getSubIds())) {
        for (int id : serverRegister.getSubIds()) {
          channels.remove(id);
        }
      }
    }
    return isSuc;
  }

  private boolean doRegister(Connection connection, ServerRegister serverRegister) {
    Channel channel = connection.channel();
    Channel oldChannel = channels.putIfAbsent(serverRegister.getId(), channel);
    if (oldChannel != null) {
      logger.error("[proxy]服务器ID:{}, 新:{}, 旧:{}, 注册目标冲突", serverRegister.getId(),
          channel.remoteAddress(),
          oldChannel.remoteAddress());
      return false;
    }

    int[] subIds = ObjectUtils.defaultIfNull(serverRegister.getSubIds(),
        ArrayUtils.EMPTY_INT_ARRAY);
    for (Integer subId : subIds) {
      Channel oldSubChannel = channels.putIfAbsent(subId, channel);
      if (oldSubChannel != null) {
        logger.error("[proxy]服务器ID:{}, 新:{}, 旧:{}, 注册子目标:{}, 冲突",
            serverRegister.getId(), channel.remoteAddress(), oldSubChannel.remoteAddress(), subId);
        return false;
      }
    }

    return true;
  }

  public ProxyServerConfig getProxyServerConfig() {
    return proxyServerConfig;
  }

  public void setProxyServerConfig(ProxyServerConfig proxyServerConfig) {
    this.proxyServerConfig = proxyServerConfig;
  }

  public Map<Integer, Channel> getChannels() {
    return channels;
  }

  public void setChannels(Map<Integer, Channel> channels) {
    this.channels = channels;
  }

}
