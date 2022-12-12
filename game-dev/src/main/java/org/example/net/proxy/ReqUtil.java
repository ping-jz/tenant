package org.example.net.proxy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.example.net.ReqMethod;
import org.example.net.RespMethod;

public class ReqUtil {

  public static int METHOD_LIMIT_PER_MODULE = 100;

  /**
   * 根据{@link ReqMethod#value()}的约定来获取协议ID
   *
   * @return 协议ID
   * @since 2022年06月15日 12:12:12
   */
  public static int protoReqId(Method method) {
    ReqMethod reqMethod = method.getAnnotation(ReqMethod.class);
    if (reqMethod == null) {
      return 0;
    }

    return reqMethod.value() == 0 ? Math.abs(
        method.getDeclaringClass().getName().hashCode() + method.getName().hashCode())
        : reqMethod.value();
  }

  /**
   * 根据{@link RespMethod#value()}的约定来获取协议ID
   *
   * @return 协议ID
   * @since 2022年06月15日 12:12:12
   */
  public static int protoRespId(Method method) {
    RespMethod reqMethod = method.getAnnotation(RespMethod.class);
    if (reqMethod == null) {
      return 0;
    }

    return Math.negateExact(reqMethod.value());
  }


  /**
   * 获取网络请求返回方法
   *
   * @param clz rpc模块
   * @since 2022年06月15日 12:16:17
   */
  public static List<Pair<Integer, Method>> getMethods(Class<?> clz) {
    Set<Integer> ids = new HashSet<>();
    List<Pair<Integer, Method>> result = new ArrayList<>();

    for (Method m : clz.getDeclaredMethods()) {
      if (m.getAnnotation(ReqMethod.class) != null) {
        int reqId = protoReqId(m);
        if (ids.contains(reqId) || reqId <= 0) {
          throw new IllegalArgumentException(
              String.format("类型:%s.%s,协议号:%s,协议号重复或者小于等于0", m.getDeclaringClass().getName(),
                  m.getName(), reqId));
        }

        m.setAccessible(true);
        ids.add(reqId);
        result.add(Pair.of(reqId, m));
      } else if (m.getAnnotation(RespMethod.class) != null) {
        int respId = protoRespId(m);
        if (ids.contains(respId) || 0 <= respId) {
          throw new IllegalArgumentException(
              String.format("类型:%s.%s,协议号:%s,协议号重复或者大于等于0", m.getDeclaringClass().getName(),
                  m.getName(), respId));
        }

        m.setAccessible(true);
        ids.add(respId);
        result.add(Pair.of(respId, m));
      }
    }

    result.sort(Comparator.comparingInt(Pair::getLeft));
    return result;
  }
}
