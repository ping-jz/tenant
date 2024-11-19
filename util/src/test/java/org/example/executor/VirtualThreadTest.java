package org.example.executor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;

public class VirtualThreadTest {


  protected static ExecutorService service;

  @BeforeAll
  public static void beforeAll() {
    service = Executors.newVirtualThreadPerTaskExecutor();
  }

  @AfterAll
  public static void afterAll() {
    if (service != null) {
      service.shutdownNow();
    }
  }


  /**
   * 工作窃取
   *
   * @since 2021年10月07日 16:37:58
   */
  @RepeatedTest(ExecutorTest.REPEAT)
  public void test() throws InterruptedException {

  }

}
