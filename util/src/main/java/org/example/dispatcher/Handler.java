package org.example.dispatcher;

import java.lang.reflect.Method;

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
  /** 结果协议编号 */
  private int resId;

  public Handler(Object obj, Method method) {
    this.obj = obj;
    this.method = method;
    method.setAccessible(true);
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

  public Object invoke(Object... params) throws Exception {
    return method.invoke(obj, params);
  }
}
