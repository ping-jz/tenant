package org.example.net;

import io.netty.channel.Channel;

/**
 * 消息分发
 *
 * @author ZJP
 * @since 2021年08月19日 12:08:39
 **/
@FunctionalInterface
public interface Dispatcher {

  /**
   * 根据具体场景，把消息分发到对应的处理器
   *
   * @param channel 链接
   * @param message 请求消息
   * @since 2021年08月19日 12:10:07
   */
  void dispatcher(Channel channel, Message message);
}
