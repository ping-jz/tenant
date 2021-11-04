package org.example.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

public class WorkQueueTest {

  @RepeatedTest(10)
  public void countTest() {
    WorkQueue<Integer> queue = new WorkQueue<>();

    int loop = Short.MAX_VALUE * 10;
    long expect = 0;
    for (int i = 0; i < loop; i++) {
      int rnd = ThreadLocalRandom.current().nextInt();
      queue.push(rnd);
      expect += rnd;
    }

    Assertions.assertEquals(loop, queue.queueSize());

    long result = 0;
    for (int i = 0; i < loop; i++) {
      result += queue.poll();
    }

    Assertions.assertEquals(expect, result);
    Assertions.assertEquals(WorkQueue.INITIAL_QUEUE_CAPACITY, queue.capacity());
    Assertions.assertTrue(queue.isEmpty());
  }


  @RepeatedTest(10)
  public void threadTest() throws Exception {
    ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime()
        .availableProcessors());
    WorkQueue<Integer> queue = new WorkQueue<>();

    int loop = Short.MAX_VALUE * 10;
    final AtomicLong result = new AtomicLong();
    long expect = 0;
    for (int i = 0; i < loop; i++) {
      int rnd = ThreadLocalRandom.current().nextInt();
      expect += rnd;
      service.execute(() -> {
        queue.push(rnd);
      });
      service.execute(() -> {
        for (; ; ) {
          Integer t = queue.poll();
          if (t != null) {
            result.addAndGet(t);
            break;
          }
        }
      });
    }

    service.shutdown();
    Assertions.assertTrue(service.awaitTermination(1, TimeUnit.SECONDS));

    Assertions.assertEquals(expect, result.get());
    Assertions.assertEquals(WorkQueue.INITIAL_QUEUE_CAPACITY, queue.capacity());
    Assertions.assertTrue(queue.isEmpty());
  }

}
