package org.example.net;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.TypeName;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import org.example.net.anno.Req;
import org.example.net.handler.ExecutorSupplier;
import org.example.net.handler.FirstArgExecutorSupplier;
import org.example.net.handler.RawExecutorSupplier;
import org.example.net.handler.RpcExecutorSupplier;

public final class Util {

  /** 回调请求ID */
  public static final int CALL_BACK_ID = 1;

  /** 常用类型 */

  public static final TypeName EXECUTOR_SUPPLIER_CLASS_NAME = TypeName.get(ExecutorSupplier.class);
  public static final TypeName FIRST_ARG_EXECUTOR_SUPPLIER_CLASS_NAME = TypeName.get(
      FirstArgExecutorSupplier.class);
  public static final TypeName RAW_EXECUTOR_SUPPLIER_CLASS_NAME = TypeName.get(
      RawExecutorSupplier.class);
  public static final TypeName RPC_EXECUTOR_SUPPLIER_CLASS_NAME = TypeName.get(
      RpcExecutorSupplier.class);
  public static final ClassName CONNECTION_CLASS_NAME = ClassName.get("org.example.net",
      "Connection");
  public static final ClassName CONNECTION_GETTER =
      ClassName.get("org.example.net", "ConnectionManager");
  public static final ClassName BASE_REMOTING = ClassName.get("org.example.net", "BaseRemoting");
  public static final ClassName COMMON_SERIALIZER = ClassName.get("org.example.serde",
      "CommonSerializer");

  public static final String SERIALIZER_VAR_NAME = "serializer";
  public static final FieldSpec COMMON_SERIALIZER_FIELD_SPEC = FieldSpec
      .builder(COMMON_SERIALIZER, SERIALIZER_VAR_NAME)
      .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
      .build();
  public static final ClassName BYTE_BUF = ClassName.get("io.netty.buffer", "ByteBuf");
  public static final ClassName BYTEBUF_UTIL = ClassName.get("org.example.serde",
      "NettyByteBufUtil");
  public static final ClassName POOLED_UTIL = ClassName.get("io.netty.buffer",
      "PooledByteBufAllocator");

  public static final ClassName UNNPOOLED_UTIL = ClassName.get("io.netty.buffer",
      "Unpooled");
  public static final ClassName MESSAGE_CLASS_NAME = ClassName.get("org.example.net", "Message");
  public static final ClassName LOGGER = ClassName.get("org.slf4j", "Logger");
  public static final ClassName LOGGER_FACTOR = ClassName.get("org.slf4j", "LoggerFactory");
  public static final ClassName HANDLER_INTERFACE = ClassName.get("org.example.net.handler",
      "Handler");

  public static final ClassName COMPLETE_ABLE_FUTURE_CLASS_NAME = ClassName.get(
      "java.util.concurrent",
      "CompletableFuture");

  public static final ClassName SUPPLIER_CLASS_NAME = ClassName.get(
      "java.util.function",
      "Supplier");


  public static final String MSG_ID_VAR_NAME = "msgId";

  public static List<ExecutableElement> getReqMethod(ProcessingEnvironment processingEnv,
      TypeElement typeElement) {
    List<ExecutableElement> res = new ArrayList<>();

    for (Element element : typeElement.getEnclosedElements()) {
      if (element.getKind() != ElementKind.METHOD) {
        continue;
      }

      if (element.getAnnotation(Req.class) == null) {
        continue;
      }

      if (element.getModifiers().contains(Modifier.ABSTRACT)) {
        processingEnv.getMessager()
            .printMessage(Kind.ERROR, "@Req can't not applied to abstract method", element);
        continue;
      }

      if (element.getModifiers().contains(Modifier.STATIC)) {
        processingEnv.getMessager()
            .printMessage(Kind.ERROR, "@Req can't not applied to static method", element);
        continue;
      }

      if (!element.getModifiers().contains(Modifier.PUBLIC)) {
        processingEnv.getMessager()
            .printMessage(Kind.ERROR, "@Req  mnust applied to public method", element);
        continue;
      }

      res.add((ExecutableElement) element);
    }

    return res;
  }


  public static int calcProtoId(TypeElement facadeName, ExecutableElement method) {
    Req reqAnno = method.getAnnotation(Req.class);
    int id = reqAnno.value();

    if (id == 0) {
      // 类名哈希+方法名哈希的绝对值作为协议ID,
      id = Math.abs(
          facadeName.getQualifiedName().toString().hashCode()
              + method.getSimpleName().toString().hashCode());
    }

    return id;
  }


  private Util() {
  }
}
