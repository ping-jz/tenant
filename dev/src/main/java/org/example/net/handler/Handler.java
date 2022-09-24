package org.example.net.handler;

import java.lang.invoke.MethodHandle;
import java.util.Objects;

/**
 * 请求处理数据
 *
 * @author ZJP
 * @since 2021年07月22日 22:25:07
 **/
public class Handler {

  /** 无参数组 */
  private static final Object[] NO_PARAMS = new Object[0];

  private String name;
  /**
   * 请求处理方法
   */
  private MethodHandle method;
  /**
   * 请求协议编号
   */
  private int reqId;

  public Handler() {
  }

  public static Handler of(String name, MethodHandle method, int req) {
    Handler handler = new Handler();
    handler.method = method;
    handler.reqId = req;
    handler.name = name;
    return handler;
  }

  public MethodHandle method() {
    return method;
  }

  public Object invoke() throws Throwable {
    return method.invoke();
  }

  public int reqId() {
    return reqId;
  }

  public Object invoke(Object... params) throws Throwable {
    return method.invokeWithArguments(params);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Handler handler = (Handler) o;
    return reqId == handler.reqId && Objects.equals(method, handler.method);
  }

  @Override
  public int hashCode() {
    return Objects.hash(method, reqId);
  }

  @Override
  public String toString() {
    return name;
  }
}
