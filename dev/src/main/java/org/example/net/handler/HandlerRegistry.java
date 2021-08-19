package org.example.net.handler;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.example.net.ReqMethod;

/**
 * 根据协议号int,提供获取和注册处理者的方法。
 *
 * 获取之后如何验证Handler是否合法，交由业务决定。此类只提供基础的基础和获取。调用方式则交给业务决定
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

  /**
   * 寻找被{@link ReqMethod}标记的非静态公共方法
   *
   * @param object 需要被注册的对象
   * @since 2021年07月24日 10:04:05
   */
  public List<Handler> findHandler(Object object) {
    List<Handler> res = null;
    Class<?> clazz = object.getClass();

    Method[] methods = clazz.getMethods();
    for (Method m : methods) {
      int modifier = m.getModifiers();
      if (Modifier.isStatic(modifier)) {
        continue;
      }

      ReqMethod packet = m.getAnnotation(ReqMethod.class);
      if (packet == null) {
        continue;
      }
      m.setAccessible(true);

      if (res == null) {
        res = new ArrayList<>();
      }

      Handler handler = Handler.of(object, m, packet.value());
      res.add(handler);
    }

    return res == null ? Collections.emptyList() : res;
  }

  /**
   * 根据{@link Handler#reqId()}获取协议ID然后进行注册，协议ID不允许重复
   *
   * @param hs 需要注册处理者
   * @since 2021年07月24日 10:25:05
   */
  public void registeHandlers(List<Handler> hs) {
    for (Handler h : hs) {
      final Handler old = handles.get(h.reqId());
      if (old != null) {
        throw new RuntimeException(String
            .format("协议ID:【%s】,%s和%s发生重读", h.reqId(), h.method().getName(), h.method().getName()));
      }

      handles.put(h.reqId(), h);
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
