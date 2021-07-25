package org.example.net.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.example.handler.Handler;

public class SendProxy {

  /** 每个某块可以注册的方法 */
  private static int METHOD_LIMIT = 100;

  /** 动态代理处理者 */
  private InvocationHandler handler;
  /** 类型 -> [方法名, 方法信息] */
  private Map<Class<?>, Map<String, RpcMetaMethodInfo>> rpcMethodInfos;
  /** 类型 -> Proxy对象 */
  private Map<Class<?>, Object> ivkCaches;
  /** 协议ID -> 对应处理方法 */
  private Map<Integer, Handler> handlers;

  public SendProxy() {
    ivkCaches = new ConcurrentHashMap<>();
    handler = new SendProxyInvoker();
  }

  /**
   * 构建远程方法调用信息
   *
   * @param object 注册的远程Facade
   * @since 2021年07月25日 15:49:52
   */
  public Map<Integer, RpcMetaMethodInfo> findRpcMethods(Object object) {
    Class<?>[] interfaces = object.getClass().getInterfaces();
    Map<Integer, RpcMetaMethodInfo> infos = new HashMap<>();
    for (Class<?> inter : interfaces) {
      RpcModule module = inter.getAnnotation(RpcModule.class);
      if (module == null) {
        continue;
      }

      Method[] methods = inter.getDeclaredMethods();
      if (methods.length <= 0) {
        continue;
      }

      int start = module.value();
      int end = start + METHOD_LIMIT;
      Arrays.sort(methods, Comparator.comparing(Method::getName));
      for (int i = 0; i < methods.length; i++) {
        Method m = methods[i];
        RpcMethod method = m.getAnnotation(RpcMethod.class);
        if (method == null) {
          continue;
        }
        methods[i] = null;

        RpcMetaMethodInfo info = new RpcMetaMethodInfo()
            .id(method.value()).name(m.getName());
        int absId = Math.abs(info.id());
        if (infos.containsKey(info.id())
            || absId < start
            || end <= absId) {
          throw new RuntimeException(
              String.format("类型:%s 方法:%s, 协议号:%s, 协议错误", inter.getName(), m.getName(), info.id()));
        }

        infos.put(info.id(), info);
      }

      //没标记的都按方法名，排序分配
      for (int i = 0, mi = 0; mi < methods.length && i < METHOD_LIMIT; i++) {
        if (infos.containsKey(i)) {
          continue;
        }

        Method m = methods[mi++];
        if (m == null) {
          continue;
        }

        RpcMetaMethodInfo info = new RpcMetaMethodInfo().id(module.value() + i)
            .name(m.getName());
        int absId = Math.abs(info.id());
        if (absId < start || end <= absId) {
          throw new RuntimeException(
              String.format("类型:%s 方法:%s, 协议号:%s, 协议错误", inter.getName(), m.getName(), info.id()));
        }

        infos.put(info.id(), info);
      }
    }

    return infos;
  }

  @SuppressWarnings("unchecked")
  public <T> T getProxy(Class<T> clz) {
    T proxy = (T) ivkCaches.get(clz);
    if (proxy != null) {
      return proxy;
    } else {
      T insta = (T) Proxy.newProxyInstance(clz.getClassLoader(), new Class[]{clz}, handler);
      ivkCaches.putIfAbsent(clz, insta);
      return insta;
    }
  }


  /**
   * 这个最后在测试吧
   *
   * @author ZJP
   * @since 2021年07月25日 14:23:21
   **/
  private static class SendProxyInvoker implements InvocationHandler {

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (method.getDeclaringClass() == Object.class) {
        return method.invoke(proxy, args);
      } else {
        return null;
      }
    }
  }

}
