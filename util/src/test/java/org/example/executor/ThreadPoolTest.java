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

public class ThreadPoolTest {


  protected static ExecutorService service;
  protected static int powerOf2;


  @BeforeAll
  public static void beforeAll() {
    powerOf2 = ExecutorTest.nextPowerOfTwo(Runtime.getRuntime().availableProcessors());
    service = Executors.newFixedThreadPool(powerOf2);
  }

  @AfterAll
  public static void afterAll() {
    if (service != null) {
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
    IntStream.range(0, ExecutorTest.TASKS).parallel().forEach(i -> service.execute(task));

    Assertions.assertTrue(latch.await(5, TimeUnit.MINUTES));
  }


}
