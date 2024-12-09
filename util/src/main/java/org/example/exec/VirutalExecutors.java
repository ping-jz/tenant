package org.example.exec;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.example.util.Identity;

public class VirutalExecutors {

  private static final VirutalExecutors common = new VirutalExecutors();

  private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
  private final LoadingCache<Identity, VirtualExecutor> temporalExecutor;

  public VirutalExecutors() {
    temporalExecutor = Caffeine
        .newBuilder()
        .expireAfterAccess(Duration.ofMinutes(1))
        .build(VirtualExecutor::new);
  }

  /**
   * 使用与此{@code id}绑定{@link VirtualExecutor}来执行任务
   *
   * @param id 指定执行器所属ID
   * @since 2024/12/8 18:31
   */
  public Thread executeWith(Identity id, Runnable command) {
    return temporalExecutor.get(id).exec(command);
  }

  /**
   * 使用当前{@link VirtualExecutor#current()}来执行定时任务，如果不在{@link VirtualExecutor}环境中则报错
   *
   * @since 2024/12/8 18:31
   */
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    VirtualExecutor executor = VirtualExecutor.current();
    Identity id = Objects.requireNonNull(executor,
        "当前环境缺少VirtualExecutor。请求修复或使用scheduleOndefault来执行").getIdentity();
    return scheduledExecutorService.schedule(() -> {
      executeWith(id, command);
    }, delay, unit);
  }

  /**
   * 使用当前{@link VirtualExecutor#current()}来执行定时任务，如果不在{@link VirtualExecutor}环境中则报错
   *
   * @since 2024/12/8 18:31
   */
  public ScheduledFuture<?> schedule(Identity identity, Runnable command, long delay,
      TimeUnit unit) {
    Identity id = Objects.requireNonNull(identity, "identity不能为空");
    return scheduledExecutorService.schedule(() -> {
      executeWith(id, command);
    }, delay, unit);
  }


  public VirtualExecutor getExecutor(Identity id) {
    return temporalExecutor.get(id);
  }

  public static VirutalExecutors commonPool() {
    return common;
  }
}

