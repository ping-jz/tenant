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
  private final LoadingCache<Identity, VirtualExecutor> executors;

  public VirutalExecutors() {
    temporalExecutor = Caffeine
        .newBuilder()
        .expireAfterAccess(Duration.ofMinutes(1))
        .build(VirtualExecutor::new);

    executors = Caffeine
        .newBuilder()
        .build(VirtualExecutor::new);
  }

  /**
   * 使用默认的{@link DefaultIdentity#DEFAULT}所属的{@link VirtualExecutor} 来执行任务
   *
   * @since 2024/12/8 18:31
   */
  public Thread executeOndefault(Runnable command) {
    return executeOnId(DefaultIdentity.DEFAULT, command);
  }

  /**
   * 使用与此{@code id}绑定{@link VirtualExecutor}来执行任务
   *
   * @param id 指定执行器所属ID
   * @since 2024/12/8 18:31
   */
  public Thread executeOnId(Identity id, Runnable command) {
    return temporalExecutor.get(id).exec(command);
  }

  /**
   * 使用默认的{@link DefaultIdentity#DEFAULT}所属的{@link VirtualExecutor} 来执行定时任务
   *
   * @since 2024/12/8 18:31
   */
  public ScheduledFuture<?> scheduleOndefault(Runnable command, long delay, TimeUnit unit) {
    return scheduledExecutorService.schedule(() -> {
      executeOnId(DefaultIdentity.DEFAULT, command);
    }, delay, unit);
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
      executeOnId(id, command);
    }, delay, unit);
  }

  public VirtualExecutor getExecutor(Identity id) {
    return temporalExecutor.get(id);
  }

  public static VirutalExecutors commonPool() {
    return common;
  }
}

