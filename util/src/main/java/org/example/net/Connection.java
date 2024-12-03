package org.example.net;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

/**
 * An abstract Over netty channel
 *
 * @author ZJP
 * @since 2021年08月13日 17:44:32
 **/
public record Connection(Integer id, Channel channel) {

  public static final AttributeKey<Connection> CONNECTION = AttributeKey.valueOf("connection");

  public static Connection connection(Integer id, Channel channel) {
    Connection connection = new Connection(id, channel);
    connection.channel().attr(CONNECTION).set(connection);
    return connection;
  }

  public boolean isActive() {
    Channel channel = channel();
    return channel != null && channel.isActive();
  }

  public void close() {
    if (isActive()) {
      channel().close();
    }
  }
}
