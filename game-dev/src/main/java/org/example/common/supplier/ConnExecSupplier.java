package org.example.common.supplier;

import java.util.concurrent.Executor;
import org.example.exec.VirutalExecutors;
import org.example.net.Connection;
import org.example.net.handler.ArgExecSupplier;

/**
 * 使用Connection来获取Executor
 *
 * @author zhongjianping
 * @since 2024/12/5 18:37
 */
public interface ConnExecSupplier extends ArgExecSupplier<Connection> {

  @Override
  default Executor get(Connection c) {
    return VirutalExecutors.commonPool().getExecutor(c.id());
  }
}
