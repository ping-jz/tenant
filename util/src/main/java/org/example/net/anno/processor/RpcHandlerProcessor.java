package org.example.net.anno.processor;

import static org.example.net.Util.BYTEBUF_UTIL;
import static org.example.net.Util.BYTE_BUF;
import static org.example.net.Util.CONNECTION_CLASS_NAME;
import static org.example.net.Util.LOGGER;
import static org.example.net.Util.LOGGER_FACTOR;
import static org.example.net.Util.MESSAGE_CLASS_NAME;
import static org.example.net.Util.MSG_ID_VAR_NAME;
import static org.example.net.Util.SERIALIZER_VAR_NAME;
import static org.example.net.Util.isCompleteAbleFuture;

import com.google.auto.service.AutoService;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeSpec.Builder;
import io.netty.util.ReferenceCountUtil;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;
import org.example.net.Util;

/**
 * 负责RPC方法的调用类和代理类
 * <p>
 * //TODO 处理下会话参数Connection, Message等，这些不用生成。用调用者自己传入
 *
 * @author zhongjianping
 * @since 2024/8/9 11:19
 */
@SupportedAnnotationTypes("org.example.common.net.annotation.RpcModule")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class RpcHandlerProcessor extends AbstractProcessor {

  private static final String FACADE_VAR_NAME = "facade";

  private static final String CONNECTION_VAR_NAME = "c";
  private static final String MESSAGE_VAR_NAME = "m";
  private static final String BUF_VAR_NAME = "buf";
  private static final String PROTOS_VAR_NAME = "protos";
  private static final ParameterSpec CONNECTION_PARAM_SPEC = ParameterSpec.builder(
      CONNECTION_CLASS_NAME,
      CONNECTION_VAR_NAME).build();
  private static final ParameterSpec MESSAGE_PARAM_SPEC = ParameterSpec.builder(
      Util.MESSAGE_CLASS_NAME,
      MESSAGE_VAR_NAME).build();

  private static final String CALL_BACK_HANDLER_PACKAGE = "org.example.common.net.generated.callback";

  private static final String CALL_BACK_SIMPLE_NAME = "CallBack";


  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (TypeElement annotation : annotations) {
      Set<? extends Element> annotationElements = roundEnv.getElementsAnnotatedWith(annotation);
      if (annotationElements.isEmpty()) {
        continue;
      }
      for (Element clazz : annotationElements) {
        if (clazz.getKind() != ElementKind.CLASS) {
          processingEnv.getMessager()
              .printMessage(Kind.ERROR, "@RpcModule must be applied to a Class", clazz);
          return false;
        }

        if (clazz.getModifiers().contains(Modifier.ABSTRACT)) {
          processingEnv.getMessager()
              .printMessage(Kind.ERROR, "@RpcModule can't not applied to abstract class", clazz);
          return false;
        }
        TypeElement facade = (TypeElement) clazz;

        List<Element> methodElements = Util.getReqMethod(processingEnv, facade);
        if (methodElements.isEmpty()) {
          continue;
        }

        try {
          generateHandler(facade, methodElements);
          //generateCallBackHandler(facade, methodElements);
        } catch (Exception e) {
          processingEnv.getMessager()
              .printError(
                  "[%s] %s build Handler error, cause:%s, \n%s".formatted(getClass(),
                      facade.getQualifiedName(),
                      e,
                      Arrays.stream(
                              e.getStackTrace()).map(Objects::toString)
                          .collect(Collectors.joining("\n"))
                  ),
                  facade);
        }
      }
    }

    return false;
  }

  private void generateHandler(TypeElement facade, List<Element> elements) throws IOException {
    TypeName facdeTypeName = TypeName.get(facade.asType());
    String qualifiedName = facade.getQualifiedName().toString() + "Handler";
    int lastIdx = qualifiedName.lastIndexOf('.');
    String packet = qualifiedName.substring(0, lastIdx);
    String simpleName = qualifiedName.substring(lastIdx + 1);

    TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(simpleName)
        .addAnnotation(Util.HANDLER_ANNOTATION)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addSuperinterface(Util.HANDLER_INTERFACE)
        .addField(FieldSpec
            .builder(facdeTypeName, FACADE_VAR_NAME)
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .build())
        .addField(Util.COMMON_SERIALIZER_FIELD_SPEC)
        .addMethod(MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(facdeTypeName, FACADE_VAR_NAME)
            .addParameter(Util.COMMON_SERIALIZER, SERIALIZER_VAR_NAME)
            .addStatement("this.$L = $L", FACADE_VAR_NAME, FACADE_VAR_NAME)
            .addStatement("this.$L = $L", SERIALIZER_VAR_NAME, SERIALIZER_VAR_NAME)
            .build());

    generateMethod(facade, typeSpecBuilder, elements);

    JavaFile javaFile = JavaFile.builder(packet, typeSpecBuilder.build())
        .build();

    JavaFileObject file = processingEnv.getFiler().createSourceFile(qualifiedName);
    try (PrintWriter writer = new PrintWriter(file.openWriter())) {
      javaFile.writeTo(writer);
    }
  }

  public void generateMethod(TypeElement facde, TypeSpec.Builder handler,
      List<Element> methods) {
    MethodSpec.Builder invoker = MethodSpec.methodBuilder("invoke")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(CONNECTION_PARAM_SPEC)
        .addParameter(MESSAGE_PARAM_SPEC)
        .addException(Exception.class);

    invoker.beginControlFlow("switch(m.proto())");
    IntList intList = generateMethod0(facde, handler, methods, invoker);

    invoker
        .addStatement(
            "default -> throw new UnsupportedOperationException(\"【$L】无法处理消息，原因:【缺少对应方法】，消息ID:【%s】\".formatted($L.proto()))",
            facde.getSimpleName(), MESSAGE_VAR_NAME)
        .endControlFlow().addCode(";");

    handler
        .addField(FieldSpec.builder(int[].class, PROTOS_VAR_NAME)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .initializer("new int[]{$L}",
                intList.intStream().mapToObj(String::valueOf).collect(Collectors.joining(",")))
            .build())
        .addMethod(invoker.build());
  }

  private static IntList generateMethod0(TypeElement facde, Builder handler, List<Element> methods,
      MethodSpec.Builder invoker) {
    IntList intList = new IntArrayList();
    for (Element element : methods) {
      ExecutableElement executableElement = (ExecutableElement) element;
      final int id = Util.calcProtoId(facde, executableElement);
      String methodName = executableElement.getSimpleName().toString();

      MethodSpec.Builder handlerMethod = MethodSpec
          .methodBuilder(methodName)
          .addParameter(CONNECTION_PARAM_SPEC)
          .addParameter(MESSAGE_PARAM_SPEC)
          .addModifiers(Modifier.PRIVATE);

      invoker.addStatement("case $L -> $L($L, $L)", id, methodName, CONNECTION_VAR_NAME,
          MESSAGE_VAR_NAME);

      handlerMethod.addStatement("$T $L = $L.packet()", BYTE_BUF, BUF_VAR_NAME, MESSAGE_VAR_NAME);

      TypeMirror returnTypeMirror = executableElement.getReturnType();
      boolean callback = returnTypeMirror.getKind() != TypeKind.VOID;
      if (callback) {
        handlerMethod.addStatement("int $L = $T.readInt32($L)", MSG_ID_VAR_NAME, BYTEBUF_UTIL,
            BUF_VAR_NAME);
      }

      List<? extends VariableElement> params = executableElement.getParameters();
      for (VariableElement p : params) {
        final String pname = p.getSimpleName().toString();
        TypeMirror ptype = p.asType();
        switch (ptype.getKind()) {
          case BOOLEAN ->
              handlerMethod.addStatement("boolean $L = $L.readBoolean()", pname, BUF_VAR_NAME);
          case BYTE -> handlerMethod.addStatement("byte $L = $L.readByte()", pname, BUF_VAR_NAME);
          case SHORT ->
              handlerMethod.addStatement("short $L = $L.readShort()", pname, BUF_VAR_NAME);
          case CHAR -> handlerMethod.addStatement("char $L = $L.readChar()", pname, BUF_VAR_NAME);
          case FLOAT ->
              handlerMethod.addStatement("float $L = $L.readFloat()", pname, BUF_VAR_NAME);
          case DOUBLE ->
              handlerMethod.addStatement("double $L = $L.readDouble()", pname, BUF_VAR_NAME);
          case INT -> handlerMethod.addStatement("int $L = $T.readInt32($L)", pname, BYTEBUF_UTIL,
              BUF_VAR_NAME);
          case LONG -> handlerMethod.addStatement("long $L = $T.readInt64($L)", pname, BYTEBUF_UTIL,
              BUF_VAR_NAME);
          default -> {
            TypeMirror paramType = p.asType();
            TypeName paramTypeName = TypeName.get(paramType);

            if (paramTypeName.equals(CONNECTION_CLASS_NAME)) {
              handlerMethod.addStatement("$T $L = $L", CONNECTION_CLASS_NAME, pname,
                  CONNECTION_VAR_NAME);
            } else if (paramTypeName.equals(MESSAGE_CLASS_NAME)) {
              handlerMethod.addStatement("$T $L = $L", MESSAGE_CLASS_NAME, pname,
                  MESSAGE_VAR_NAME);
            } else {
              handlerMethod.addStatement("$T $L = $L.read($L)", TypeName.get(ptype), pname,
                  SERIALIZER_VAR_NAME, BUF_VAR_NAME);
            }
          }
        }
      }
      if (!params.isEmpty()) {
        handlerMethod.addCode("\n");
      }

      String paramStr = params.stream().map(p -> p.getSimpleName().toString())
          .collect(Collectors.joining(", "));

      if (callback) {
        String resVarName = "res";
        String resBuf = "resBuf";

        switch (returnTypeMirror.getKind()) {
          case BOOLEAN ->
              handlerMethod.addStatement("boolean $L = $L.$L($L)", resVarName, FACADE_VAR_NAME,
                  methodName, paramStr);
          case BYTE ->
              handlerMethod.addStatement("byte $L = $L.$L($L)", resVarName, FACADE_VAR_NAME,
                  methodName, paramStr);
          case SHORT ->
              handlerMethod.addStatement("short $L = $L.$L($L)", resVarName, FACADE_VAR_NAME,
                  methodName, paramStr);
          case CHAR ->
              handlerMethod.addStatement("char $L = $L.$L($L)", resVarName, FACADE_VAR_NAME,
                  methodName, paramStr);
          case FLOAT ->
              handlerMethod.addStatement("float $L = $L.$L($L)", resVarName, FACADE_VAR_NAME,
                  methodName, paramStr);
          case DOUBLE ->
              handlerMethod.addStatement("double $L = $L.$L($L)", resVarName, FACADE_VAR_NAME,
                  methodName, paramStr);
          case INT -> handlerMethod.addStatement("int $L = $L.$L($L)", resVarName, FACADE_VAR_NAME,
              methodName, paramStr);
          case LONG ->
              handlerMethod.addStatement("long $L = $L.$L($L)", resVarName, FACADE_VAR_NAME,
                  methodName, paramStr);
          default -> handlerMethod.addStatement("$T $L = $L.$L($L)", TypeName.get(returnTypeMirror),
              resVarName,
              FACADE_VAR_NAME,
              methodName, paramStr);
        }

        handlerMethod
            .addCode("\n")
            .addStatement("$T $L = $T.DEFAULT.buffer()", BYTE_BUF, resBuf, Util.POOLED_UTIL)
            .beginControlFlow("try")
            .addStatement("$T.writeInt32($L, $L)", BYTEBUF_UTIL, resBuf, MSG_ID_VAR_NAME)
            .addStatement("$L.writeObject($L, $L)", SERIALIZER_VAR_NAME, resBuf, resVarName)
            .addStatement("$L.channel().writeAndFlush($T.of($L, $L))", CONNECTION_VAR_NAME,
                MESSAGE_CLASS_NAME, Util.CALL_BACK_ID, resBuf)
            .endControlFlow()
            .beginControlFlow("catch (Throwable t)")
            .addStatement("$T.release($L)", ReferenceCountUtil.class, resBuf)
            .addStatement("throw t")
            .endControlFlow();

      } else {
        handlerMethod.addStatement("$L.$L($L)", FACADE_VAR_NAME, methodName, paramStr);
      }

      handler.addMethod(handlerMethod.build());

      intList.add(id);
    }
    return intList;
  }


  public void generateCallBackHandler(TypeElement facde, List<Element> elements)
      throws Exception {
    final String loggerVarName = "logger";
    TypeSpec.Builder callBackHandleBuilder = TypeSpec
        .classBuilder(facde.getSimpleName() + CALL_BACK_SIMPLE_NAME)
        .addAnnotation(Util.HANDLER_ANNOTATION)
        .addField(FieldSpec
            .builder(LOGGER, loggerVarName)
            .addModifiers(Modifier.FINAL, Modifier.STATIC)
            .initializer("$T.getLogger($S)", LOGGER_FACTOR, "CallBackLogger")
            .build())
        .addField(Util.COMMON_SERIALIZER_FIELD_SPEC)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addSuperinterface(Util.HANDLER_INTERFACE);

    MethodSpec.Builder invokeBuilder = MethodSpec.methodBuilder("invoke")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(BYTE_BUF)
        .addParameter(CONNECTION_PARAM_SPEC)
        .addParameter(MESSAGE_PARAM_SPEC)
        .addException(Exception.class)
        .beginControlFlow("switch($L.proto())", MESSAGE_VAR_NAME);

    final String futureVarName = "futureVar";
    final String msgIdVarName = "msgId";
    IntList intList = new IntArrayList();
    for (Element e : elements) {
      ExecutableElement method = (ExecutableElement) e;
      TypeMirror returnTypeMirror = method.getReturnType();
      DeclaredType returnType = isCompleteAbleFuture(returnTypeMirror);
      if (returnType == null) {
        continue;
      }
      //Handle ID
      int id = Math.negateExact(Util.calcProtoId(facde, method));

      String methodName = method.getSimpleName().toString();
      MethodSpec.Builder methodBuilder = MethodSpec
          .methodBuilder(methodName)
          .addParameter(CONNECTION_PARAM_SPEC)
          .addParameter(MESSAGE_PARAM_SPEC)
          .addException(Exception.class)
          .addStatement("final int $L = $T.readInt32($L.packet())", msgIdVarName, BYTEBUF_UTIL,
              MESSAGE_VAR_NAME)
          .addStatement("final $T $L = $L.removeInvokeFuture($L)", returnType,
              futureVarName, CONNECTION_VAR_NAME, msgIdVarName)
          .beginControlFlow("if ($L == null)", futureVarName)
          .addStatement(
              "$L.error(\"寻找回调函数失败, 可能原因：【回调函数过期，回调函数不存在】, 协议ID:$L, 消息ID:{}, 链接地址:{} \", $L,\n$L.channel().remoteAddress())",
              loggerVarName, id, msgIdVarName, CONNECTION_VAR_NAME)
          .addStatement("return")
          .endControlFlow()
          .addStatement("$T $L = $T.wrappedBuffer($L.packet())",
              BYTE_BUF, BUF_VAR_NAME, Util.UNNPOOLED_UTIL, MESSAGE_VAR_NAME)
          .addStatement("$L.complete($L.read($L))", futureVarName, SERIALIZER_VAR_NAME,
              BUF_VAR_NAME);

      intList.add(id);
      callBackHandleBuilder.addMethod(methodBuilder.build());
      invokeBuilder.addStatement("case $L -> $L($L, $L)", id, methodName, CONNECTION_VAR_NAME,
          MESSAGE_VAR_NAME);
    }

    if (intList.isEmpty()) {
      return;
    }

    MethodSpec invoker = invokeBuilder
        .addStatement(
            "default -> throw new UnsupportedOperationException(\"【$L】无法回调处理消息，原因:【缺少对应方法】，消息ID:【%s】\".formatted($L.proto()))",
            facde.getSimpleName(), MESSAGE_VAR_NAME)
        .endControlFlow()
        .addStatement("return $T.EMPTY_BUFFER", Util.UNNPOOLED_UTIL)
        .build();

    TypeSpec callBackHandler = callBackHandleBuilder
        .addField(FieldSpec.builder(int[].class, PROTOS_VAR_NAME)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .initializer("new int[]{$L}",
                intList.intStream().mapToObj(String::valueOf).collect(Collectors.joining(",")))
            .build())
        .addMethod(invoker)
        .addMethod(MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(Util.COMMON_SERIALIZER, SERIALIZER_VAR_NAME)
            .addStatement("this.$L = $L", SERIALIZER_VAR_NAME, SERIALIZER_VAR_NAME)
            .build())
        .build();
    JavaFile javaFile = JavaFile.builder(CALL_BACK_HANDLER_PACKAGE, callBackHandleBuilder.build())
        .build();

    String qualifiedName = "%s.%s".formatted(CALL_BACK_HANDLER_PACKAGE, callBackHandler.name());
    JavaFileObject file = processingEnv.getFiler().createSourceFile(qualifiedName);
    try (PrintWriter writer = new PrintWriter(file.openWriter())) {
      javaFile.writeTo(writer);
    }
  }
}
