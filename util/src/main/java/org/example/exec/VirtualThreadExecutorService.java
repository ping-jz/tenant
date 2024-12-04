package org.example.exec;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.util.concurrent.TimeUnit;
import org.example.util.Identity;

public class VirtualThreadExecutorService {

  private final LoadingCache<Identity, VirtualThreadExecutor> executors;

  private static final VirtualThreadExecutorService common = new VirtualThreadExecutorService();

  public VirtualThreadExecutorService() {
    executors = Caffeine
        .newBuilder()
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build(id -> new VirtualThreadExecutor());
  }

  public void execute(Identity id, Runnable command) {
    executors.get(id).execute(command);
  }

  public VirtualThreadExecutor removeExecutor(Identity id) {
    return executors.asMap().remove(id);
  }

  public VirtualThreadExecutor getExecutor(Identity id) {
    return executors.get(id);
  }

  public static VirtualThreadExecutorService commonPool() {
    return common;
  }
}
