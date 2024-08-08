package org.example.common.persistence;

/**
 * 记录缓存内容的各项监控指标
 *
 * @author ZJP
 * @since 2021年11月04日 20:20:45
 **/
public class ValueWrapper<V> {

  /** 读统计 */
  private long reads;
  /** 写统计 */
  private long writes;
  /** 最后读取时间 */
  private long lastRead;
  /** 最后写时间 */
  private long lastWrite;
  /** 具体内容 */
  private V value;

  public static <V> ValueWrapper<V> of(V v) {
    ValueWrapper<V> res = new ValueWrapper<>();
    res.setValue(v);
    return res;
  }


  public long getReads() {
    return reads;
  }

  public void setReads(long reads) {
    this.reads = reads;
  }

  public long getWrites() {
    return writes;
  }

  public void setWrites(long writes) {
    this.writes = writes;
  }

  public long getLastRead() {
    return lastRead;
  }

  public void setLastRead(long lastRead) {
    this.lastRead = lastRead;
  }

  public long getLastWrite() {
    return lastWrite;
  }

  public void setLastWrite(long lastWrite) {
    this.lastWrite = lastWrite;
  }

  public V getValue() {
    return value;
  }

  public void setValue(V value) {
    this.value = value;
  }

  public long incReads() {
    reads += 1;
    lastRead = System.currentTimeMillis();

    return reads;
  }

  public long incWrites() {
    writes += 1;
    lastWrite = System.currentTimeMillis();

    return writes;
  }
}
