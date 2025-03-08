package org.example.exec;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import org.example.util.Identity;
import org.jctools.queues.MpscUnboundedArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualExecutor implements Executor {

  private static final Logger logger = LoggerFactory.getLogger(VirtualExecutor.class);

  /**
   * 当前Executor的ID
   */
  private static final ConcurrentHashMap<Thread, Identity> CURRENT = new ConcurrentHashMap<>();

  private final Queue<Runnable> queue;
  private final AtomicBoolean running;
  private final ThreadFactory factory;
  private final Identity identity;

  public VirtualExecutor(Identity identity) {
    this.identity = identity;
    queue = new MpscUnboundedArrayQueue<>(128);
    running = new AtomicBoolean();
    factory = Thread.ofVirtual().name(identity.toString())
        .uncaughtExceptionHandler((t, e) -> {
          logger.error("VirtualExecutor uncaughtException", e);
        })
        .factory();
  }

  public Identity getIdentity() {
    return identity;
  }

  private void run() {
    boolean failed = !running.compareAndSet(false, true);
    if (failed) {
      return;
    }

    Thread thread = Thread.currentThread();
    try {
      CURRENT.put(thread, identity);
      doRun();
    } finally {
      CURRENT.remove(thread);
      running.set(false);

      if (!isEmpty()) {
        trySchedule();
      }
    }
  }

  private void doRun() {
    Runnable runnable = queue.poll();
    if (runnable != null) {
      runnable.run();
    }
  }

  @Override
  public void execute(Runnable command) {
    exec(command);
  }

  public void exec(Runnable command) {
    queue.add(command);
    trySchedule();
  }

  private void trySchedule() {
    if (running.get()) {
      return;
    }

    Thread thread = factory.newThread(this::run);
    thread.start();
  }

  public boolean isEmpty() {
    return queue.isEmpty();
  }

  public static VirtualExecutor current() {
    Identity identity = CURRENT.get(Thread.currentThread());
    if (identity == null) {
      return null;
    }

    return VirutalExecutors.commonPool().getExecutor(identity);
  }

}
