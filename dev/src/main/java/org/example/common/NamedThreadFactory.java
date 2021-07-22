package org.example.common;

import io.netty.util.concurrent.FastThreadLocalThread;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 可命名线程工厂
 *
 * @author kingston
 */
public class NamedThreadFactory implements ThreadFactory {


  /** 是否为守护线程 */
  private final boolean daemo;
  /** 名字 */
  private final String groupName;
  /** 累计 */
  private final AtomicInteger idGenerator = new AtomicInteger(1);

  public NamedThreadFactory(String group) {
    this(group, false);
  }

  public NamedThreadFactory(String group, boolean daemo) {
    this.groupName = group;
    this.daemo = daemo;
  }

  @Override
  public Thread newThread(Runnable r) {
    String name = getNextThreadName();
    Thread ret = new FastThreadLocalThread(null, r, name, 0);
    ret.setDaemon(daemo);
    return ret;
  }

  private String getNextThreadName() {
    return this.groupName + "-" + this.idGenerator.getAndIncrement();
  }

}
