package org.example.net.handler;

import java.lang.reflect.Method;
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

  /** 请求处理者 */
  private Object obj;
  /** 请求处理方法 */
  private Method method;
  /** 请求协议编号 */
  private int reqId;

  public Handler() {
  }

  public static Handler of(Object obj, Method method, int req) {
    Handler handler = new Handler();
    handler.obj = obj;
    handler.method = method;
    handler.reqId = req;
    return handler;
  }

  public Object obj() {
    return obj;
  }

  public Method method() {
    return method;
  }

  public Object invoke() throws Exception {
    return invoke(NO_PARAMS);
  }

  public int reqId() {
    return reqId;
  }

  public Object invoke(Object... params) throws Exception {
    return method.invoke(obj, params);
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
    return method.getDeclaringClass() + "." + method.getName();
  }
}
