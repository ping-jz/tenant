package org.example.common.net.proxy.invoker;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.example.common.net.ConnectionManager;
import org.example.net.BaseRemoting;
import org.example.net.Connection;
import org.example.net.Message;
import org.example.serde.CommonSerializer;
import org.example.serde.NettyByteBufUtil;
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
  private CommonSerializer serializer;

  public GameFacdeInvoker(ConnectionManager manager, CommonSerializer serializer) {
    this.manager = manager;
    this.serializer = serializer;
    this.remoting = new BaseRemoting();
  }


  public InnerInvoker of(int id) {
    Connection connection = manager.connection(id);
    return of(connection);
  }

  public InnerInvoker of(Connection connection) {
    Objects.requireNonNull(connection);
    if (!connection.isActive()) {
      logger.error("[RPC] GameFacdeInvoker, 因为链接【{}】失效：无法处理", connection.id());
    }

    return new InnerInvoker(Collections.singletonList(connection));
  }

  public class InnerInvoker {

    /** 解释下 */
    private final List<Connection> connections;

    InnerInvoker(List<Connection> cs) {
      connections = cs;
    }

    public void echo(String string) {
      final int id = 200;

      ByteBuf buf = Unpooled.buffer();
      serializer.writeObject(buf, string);
      Message message = Message.of(id).packet(NettyByteBufUtil.readBytes(buf));

      for (Connection connection : connections) {
        remoting.invoke(connection, message);
      }
    }

    public void ok() {
      final int id = 202;

      Message message = Message.of(id);
      for (Connection connection : connections) {
        remoting.invoke(connection, message);
      }
    }
  }


}
