package org.example.net;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

@FunctionalInterface
public interface ExecutorSupplier extends Supplier<Executor> {

  /**
   * 根据具体场景，返回对应的执行者 实现者需保证线程安全
   *
   * @since 2024/12/4 10:31
   */
  Executor get();
}
