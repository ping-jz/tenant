package org.example.net.handler;

import java.util.concurrent.Executor;
import org.example.exec.VirtualThreadExecutorService;
import org.example.util.Identity;

/**
 * 根据首个参数，获取执行器。实现了此接口，所有被{@link org.example.net.anno.Req}标记的方法首个参数的类型必须相同
 *
 * @author zhongjianping
 * @since 2024/12/4 15:51
 */
public interface FirstArgExecutorSupplier<T extends Identity> {

  default Executor get(T t) {
    return VirtualThreadExecutorService.commonPool().getExecutor(t);
  }
}
