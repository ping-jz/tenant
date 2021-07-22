package org.example.dispatcher;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 根据协议号int,提供获取和注册处理者。 至于如何调用。应该交由业务来进行决定
 *
 * @author ZJP
 * @since 2021年07月22日 22:06:22
 **/
public class HandlerRegistry {

  /** 协议编号 -> 请求处理者 */
  private final Map<Integer, Handler> handles;

  public HandlerRegistry() {
    handles = new ConcurrentHashMap<>();
  }

  public void registeHandle(Object object) {
    Class<?> clazz = object.getClass();
    while (clazz != Object.class) {
      Method[] methods = clazz.getMethods();
      for (Method m : methods) {
        if (m.isAnnotationPresent(Packet.class)) {

        }
      }
      clazz = clazz.getSuperclass();
    }
  }


  /**
   * 根据协议编号，获取处理者
   *
   * @param proto 协议编号
   * @since 2021年07月22日 23:35:25
   */
  public Handler getHandler(int proto) {
    return handles.get(proto);
  }
}
