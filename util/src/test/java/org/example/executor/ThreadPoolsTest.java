package org.example.executor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;

public class ThreadPoolsTest {


  protected static ExecutorService[] services;
  protected static int powerOf2;

  @BeforeAll
  public static void beforeAll() {
    powerOf2 = ExecutorTest.nextPowerOfTwo(Runtime.getRuntime().availableProcessors());
    services = new ExecutorService[powerOf2];
    for (int i = 0; i < powerOf2; i++) {
      services[i] = Executors.newSingleThreadExecutor();
    }
  }

  @AfterAll
  public static void afterAll() {
    for (ExecutorService service : services) {
      service.shutdown();
    }
  }


  /**
   * 工作窃取
   *
   * @since 2021年10月07日 16:37:58
   */
  @RepeatedTest(ExecutorTest.REPEAT)
  public void test() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(ExecutorTest.TASKS);
    Runnable task = latch::countDown;
    final int MARK = powerOf2 - 1;
    IntStream.range(0, ExecutorTest.TASKS).parallel().forEach(i -> {
      services[i & MARK].execute(task);
    });

    Assertions.assertTrue(latch.await(5, TimeUnit.MINUTES));
  }


}
