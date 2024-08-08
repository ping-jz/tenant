package org.example.net.proxy.invoker;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Collections;
import java.util.List;
import org.example.net.BaseRemoting;
import org.example.net.Connection;
import org.example.net.ConnectionManager;
import org.example.net.Message;
import org.example.serde.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GameFacdeInvoker {

  private Logger logger = LoggerFactory.getLogger(getClass());

  /** 链接管理 (直接拿就行了，创建和管理链接，proxy不要管) */
  private ConnectionManager manager;
  /** 调用逻辑 */
  private BaseRemoting remoting;
  /** 编码 */
  private Serializer<Object> serializer;

  public InnerInvoker of(int id) {
    Connection connection = manager.connection(id);
    if (connection == null || connection.isActive()) {
      logger.error("{} 未链接", id);
    }

    return new InnerInvoker(Collections.singletonList(connection));
  }

  public class InnerInvoker {

    /** 解释下 */
    private final List<Connection> connections;

    public InnerInvoker(List<Connection> connections) {
      this.connections = connections;
    }

    public void getServerIds() {
      final int id = 200;

      for (Connection connection : connections) {
        Message message = Message.of(id).target(connection.id());
        remoting.invoke(connection, message);
      }
    }

    public void cnsumerItem(List<Long> ids) {
      final int id = 201;

      ByteBuf buf = Unpooled.buffer();
      serializer.writeObject(buf, ids);
      for (Connection connection : connections) {
        Message message = Message.of(id).target(connection.id()).packet(buf.array());
        remoting.invoke(connection, message);
      }
    }

    public void req() {
      final int id = 202;

      for (Connection connection : connections) {
        Message message = Message.of(id).target(connection.id());
        remoting.invoke(connection, message);
      }
    }
  }


}
