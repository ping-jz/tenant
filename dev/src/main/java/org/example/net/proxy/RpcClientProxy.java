package org.example.net.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端RPC调用代理
 *
 * @author ZJP
 * @since 2021年07月25日 15:36:19
 **/
public class RpcClientProxy {

  /** 每个某块可以注册的方法 */
  private static int METHOD_LIMIT = 100;

  /** 动态代理处理者 */
  private InvocationHandler methodProxy;
  /** 类型 -> [方法名, 方法信息] */
  private Map<Class<?>, Map<String, RpcMetaMethodInfo>> rpcMethodInfos;
  /** 类型 -> Proxy对象 */
  private Map<Class<?>, Object> ivkCaches;

  public RpcClientProxy() {
    ivkCaches = new ConcurrentHashMap<>();
    methodProxy = new SendProxyInvoker(this);
    rpcMethodInfos = new ConcurrentHashMap<>();
  }

  /**
   * 构建远程方法调用信息
   *
   * @param inter rpc接口
   * @since 2021年07月25日 15:49:52
   */
  public Map<String, RpcMetaMethodInfo> registerRpcMethods(Class<?> inter) {
    Set<Integer> ids = new HashSet<>();
    Map<String, RpcMetaMethodInfo> infos = new HashMap<>();

    RpcModule module = inter.getAnnotation(RpcModule.class);
    if (module == null) {
      throw new RuntimeException(String.format("类型:%s, 缺少@RpcModule标记", inter));
    }

    Method[] methods = inter.getDeclaredMethods();
    if (methods.length <= 0) {
      return Collections.emptyMap();
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
      if (ids.contains(info.id())
          || absId < start
          || end <= absId) {
        throw new RuntimeException(
            String.format("类型:%s 方法:%s, 协议号:%s, 协议错误", inter.getName(), m.getName(), info.id()));
      }

      ids.add(info.id());
      infos.put(info.name(), info);
    }

    //没标记的都按方法名，排序分配
    for (int i = 0, mi = 0; mi < methods.length && i < METHOD_LIMIT; i++) {
      if (ids.contains(i)) {
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

      ids.add(info.id());
      infos.put(info.name(), info);
    }

    rpcMethodInfos.put(inter, infos);
    return infos;
  }


  /**
   * 根据类型和方法名，获取远程方法调用信息
   *
   * @param clazz 目标类型
   * @param methodName 方法名
   * @since 2021年07月25日 15:49:52
   */
  public RpcMetaMethodInfo getRpcMetaMethodInfo(Class<?> clazz, String methodName) {
    Map<String, RpcMetaMethodInfo> infos = rpcMethodInfos.get(clazz);
    if (infos == null) {
      infos = registerRpcMethods(clazz);
    }
    return infos.get(methodName);
  }

  @SuppressWarnings("unchecked")
  public <T> T getProxy(Class<T> clz) {
    T proxy = (T) ivkCaches.get(clz);
    if (proxy != null) {
      return proxy;
    } else {
      T insta = (T) Proxy.newProxyInstance(clz.getClassLoader(), new Class[]{clz}, methodProxy);
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

    private RpcClientProxy rpcClientProxy;

    public SendProxyInvoker(RpcClientProxy rpcClientProxy) {
      this.rpcClientProxy = rpcClientProxy;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (method.getDeclaringClass() == Object.class) {
        return method.invoke(proxy, args);
      } else {
        RpcMetaMethodInfo info = rpcClientProxy
            .getRpcMetaMethodInfo(method.getDeclaringClass(), method.getName());
        if (info == null) {
          throw new RuntimeException(
              String.format("类型:%s 方法:%s，不是RPC方法", method.getDeclaringClass(),
                  method.getName()));
        }

        //TODO 如何进行网络发送
        return null;
      }
    }
  }

}
