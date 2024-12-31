package org.example.exec;


import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.ReentrantLock;
import org.example.util.Identity;
import org.jctools.queues.MpscUnboundedArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualExecutor implements Executor {

  private static final Logger logger = LoggerFactory.getLogger(VirtualExecutor.class);

  /**
   * 当前Executor的ID
   */
  private static final ThreadLocal<Identity> CURRENT = new ThreadLocal<>();

  private final Queue<Runnable> queue;
  private final ReentrantLock lock;
  private final ThreadFactory factory;
  private final Identity identity;

  public VirtualExecutor(Identity identity) {
    this.identity = identity;
    queue = new MpscUnboundedArrayQueue<>(128);
    lock = new ReentrantLock();
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
    lock.lock();
    try {
      CURRENT.set(identity);
      doRun();
    } finally {
      CURRENT.remove();
      lock.unlock();
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

  public Thread exec(Runnable command) {
    queue.add(command);
    Thread thread = factory.newThread(this::run);
    thread.start();
    return thread;
  }

  public boolean isEmpty() {
    return queue.isEmpty();
  }

  public static VirtualExecutor current() {
    Identity identity = CURRENT.get();
    if (identity == null) {
      return null;
    }

    return VirutalExecutors.commonPool().getExecutor(identity);
  }

}
