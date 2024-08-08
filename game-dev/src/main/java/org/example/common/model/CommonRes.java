package org.example.common.model;

import org.example.serde.Serde;

@Serde
public class CommonRes<T> {

  /** 是否成功 */
  private boolean suc;
  /** 结果 */
  private T res;

  public boolean isSuc() {
    return suc;
  }

  public CommonRes<T> setSuc(boolean suc) {
    this.suc = suc;
    return this;
  }

  public T getRes() {
    return res;
  }

  public CommonRes<T> setRes(T res) {
    this.res = res;
    return this;
  }
}
