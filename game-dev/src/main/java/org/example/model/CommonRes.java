package org.example.model;

public class CommonRes<T> {

  /** 是否成功 */
  private boolean suc;
  /** 结果 */
  private T res;

  public boolean isSuc() {
    return suc;
  }

  public CommonRes<T> suc(boolean suc) {
    this.suc = suc;
    return this;
  }

  public T res() {
    return res;
  }

  public CommonRes<T> res(T res) {
    this.res = res;
    return this;
  }
}
