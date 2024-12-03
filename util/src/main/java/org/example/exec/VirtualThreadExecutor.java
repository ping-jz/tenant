package org.example.exec;


import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;
import org.jctools.queues.MpscChunkedArrayQueue;

public class VirtualThreadExecutor implements Executor {

  private final Queue<Runnable> queue;

  private final ReentrantLock lock;

  public VirtualThreadExecutor() {
    queue = new MpscChunkedArrayQueue<>(1024);
    lock = new ReentrantLock();
  }


  private void run() {
    lock.lock();
    try {
      doRun();
    } finally {
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
    queue.add(command);
    Thread.ofVirtual().start(this::run);
  }


}
