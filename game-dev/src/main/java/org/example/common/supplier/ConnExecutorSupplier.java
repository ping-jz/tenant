package org.example.common.supplier;

import java.util.concurrent.Executor;
import org.example.exec.VirutalExecutors;
import org.example.net.Connection;
import org.example.net.Message;
import org.example.net.handler.RpcExecutorSupplier;

/**
 * 使用Connection来获取Executor
 *
 * @author zhongjianping
 * @since 2024/12/5 18:37
 */
public interface ConnExecutorSupplier extends RpcExecutorSupplier {

  @Override
  default Executor get(Connection c, Message m) {
    return VirutalExecutors.commonPool().getExecutor(c.id());
  }
}
