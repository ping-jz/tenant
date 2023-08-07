package org.example.net;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.example.net.anno.Req;
import org.example.net.anno.Resp;

public class ReqUtil {

  /**
   * 根据{@link Req#value()}的约定来获取协议ID
   *
   * @return 协议ID
   * @since 2022年06月15日 12:12:12
   */
  public static int protoReqId(Method method) {
    Req reqMethod = method.getAnnotation(Req.class);
    if (reqMethod == null) {
      return 0;
    }

    return reqMethod.value() == 0 ? Math.abs(
        method.getDeclaringClass().getName().hashCode() + method.getName().hashCode())
        : reqMethod.value();
  }

  /**
   * 根据{@link Resp#value()}的约定来获取协议ID
   *
   * @return 协议ID
   * @since 2022年06月15日 12:12:12
   */
  public static int protoRespId(Method method) {
    Resp reqMethod = method.getAnnotation(Resp.class);
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
      if (m.getAnnotation(Req.class) != null) {
        int reqId = protoReqId(m);
        if (ids.contains(reqId) || reqId <= 0) {
          throw new IllegalArgumentException(
              String.format("类型:%s.%s,协议号:%s,协议号重复或者小于等于0", m.getDeclaringClass().getName(),
                  m.getName(), reqId));
        }

        m.setAccessible(true);
        ids.add(reqId);
        result.add(Pair.of(reqId, m));
      } else if (m.getAnnotation(Resp.class) != null) {
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

  /**
   * 是否需要注入链接
   *
   * @author zhongjianping
   * @since 2022/12/19 23:10
   */
  public static boolean reqConnection(Method method) {
    Class[] types = method.getParameterTypes();
    if (ArrayUtils.isEmpty(types)) {
      return false;
    }

    if (ArrayUtils.indexOf(types, Collection.class, 1) != ArrayUtils.INDEX_NOT_FOUND) {
      throw new IllegalArgumentException(
          String.format("类型:%s.%s,Connection必须作为第一个参数",
              method.getDeclaringClass().getName(),
              method.getName()));
    }

    return types[0] == Connection.class;
  }
}
