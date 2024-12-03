package org.example.net;

import io.netty.channel.Channel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.example.net.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 消息分发调度者(服务器之间)
 *
 * @author ZJP
 * @since 2021年07月24日 14:41:27
 **/
public class DefaultDispatcher implements Dispatcher {

  /**
   * 日志
   */
  private static Logger logger = LoggerFactory.getLogger(DefaultDispatcher.class);

  /**
   * 协议编号 -> 请求处理者
   */
  private final Map<Integer, Handler> handles;

  public Handler registeHandler(int id, Handler handler) {
    return handles.put(id, handler);
  }


  /**
   * 根据协议编号，获取处理者
   *
   * @param proto 协议编号
   * @since 2021年07月22日 23:35:25
   */
  public Handler getHandler(int proto) {
    return handles.get(proto);
  }


  public DefaultDispatcher() {
    handles = new ConcurrentHashMap<>();
  }


  /**
   * 根据{@link Message#proto()}进行消息分发
   *
   * @param channel 通信channel
   * @param req     请求消息
   * @since 2021年07月24日 15:58:39
   */
  public void doDispatcher(Channel channel, Message req) {
    Handler handler = getHandler(req.proto());
    if (handler == null) {
      logger.error("地址:{}, 协议号:{} 无对应处理器", channel.remoteAddress(),
          req.proto());
      return;
    }

    invokeHandler(channel, req, handler);
  }

  /**
   * 执行处理器
   *
   * @param msg     请求消息
   * @param handler 注册的处理器
   * @since 2021年08月15日 20:22:04
   */
  private void invokeHandler(Channel channel, Message msg, Handler handler) {
    Connection connection = channel.attr(Connection.CONNECTION).get();
    if (connection == null) {
      logger.error("channel:{}, 没有与绑定Connection。无法处理消息:{}", channel.remoteAddress(),
          msg.proto());
      return;
    }

    try {
      handler.invoke(connection, msg);
    } catch (Throwable e) {
      logger.error("from:{}, proto:{}, handler error", channel.remoteAddress(), msg.proto(), e);
    }
  }

  @Override
  public void dispatcher(Channel channel, Message message) {
    doDispatcher(channel, message);
  }
}
