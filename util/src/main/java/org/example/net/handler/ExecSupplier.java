package org.example.net.handler;

import java.util.concurrent.Executor;

@FunctionalInterface
public interface ExecSupplier {

  /**
   * 根据具体场景，返回对应的执行者 实现者需保证线程安全
   *
   * @since 2024/12/4 10:31
   */
  Executor get();
}
