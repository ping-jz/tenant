package org.example.serde;

/**
 * 序列化模块注册
 *
 * @author zhongjianping
 * @since 2025/5/14 11:22
 */
@FunctionalInterface
public interface SerdeRegister {

  /**
   * 使用{@param serdes}注册{@link  Serde}实现
   *
   * @since 2025/5/14 11:23
   */
  void register(Serdes serdes);
}
