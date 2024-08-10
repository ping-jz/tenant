package org.example.net.anno.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

class RpcProcessorConstant {

  public static final String INVOKER_PACKAGE = "org.example.common.net.proxy.invoker";
  public static final String INNER_SIMPLE_NAME = "Invoker";

  /** 常用类型 */
  public static final ClassName CONNECTION = ClassName.get("org.example.net", "Connection");
  public static final ParameterizedTypeName CONNECTION_GETTER = ParameterizedTypeName.get(
      ClassName.get("java.util.function", "Function"),
      TypeName.INT.box(), CONNECTION
  );
  public static final ClassName BASE_REMOTING = ClassName.get("org.example.net", "BaseRemoting");
  public static final ClassName COMMON_SERIALIZER = ClassName.get("org.example.serde",
      "CommonSerializer");
  public static final ClassName BYTE_BUF = ClassName.get("io.netty.buffer", "ByteBuf");
  public static final ClassName BYTEBUF_UTIL = ClassName.get("org.example.serde",
      "NettyByteBufUtil");
  public static final ClassName POOLED_UTIL = ClassName.get("io.netty.buffer",
      "PooledByteBufAllocator");
  public static final ClassName MESSAGE = ClassName.get("org.example.net", "Message");
  public static final ClassName LOGGER = ClassName.get("org.slf4j", "Logger");
  public static final ClassName LOGGER_FACTOR = ClassName.get("org.slf4j", "LoggerFactory");

}
