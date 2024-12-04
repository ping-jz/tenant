package org.example.net;

import java.util.concurrent.Executor;

@FunctionalInterface
public interface RpcExecutorSupplier {

  /**
   * 根据链接信息或者请求信息，返回对应的执行者
   *
   * @param c 网络链接
   * @param m 请求体
   * @since 2024/12/4 10:31
   */
  Executor get(Connection c, Message m);
}
