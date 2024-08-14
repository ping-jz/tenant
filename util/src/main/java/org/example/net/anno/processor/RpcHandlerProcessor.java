package org.example.net.anno.processor;

import static org.example.net.anno.processor.Util.BYTEBUF_UTIL;
import static org.example.net.anno.processor.Util.LOGGER;
import static org.example.net.anno.processor.Util.LOGGER_FACTOR;
import static org.example.net.anno.processor.Util.SERIALIZER_VAR_NAME;
import static org.example.net.anno.processor.Util.isCompleteAbleFuture;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
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
import org.apache.commons.lang3.ArrayUtils;

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
  private static final ParameterSpec CONNECTION_PARAM_SPEC = ParameterSpec.builder(Util.CONNECTION,
      CONNECTION_VAR_NAME).build();
  private static final ParameterSpec MESSAGE_PARAM_SPEC = ParameterSpec.builder(Util.MESSAGE_CLASS_NAME,
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
          generateCallBackHandler(facade, methodElements);
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
        .returns(byte[].class)
        .addParameter(CONNECTION_PARAM_SPEC)
        .addParameter(MESSAGE_PARAM_SPEC)
        .addException(Exception.class);

    invoker.beginControlFlow("return switch(m.proto())");
    IntList intList = new IntArrayList();
    for (Element element : methods) {
      ExecutableElement executableElement = (ExecutableElement) element;
      final int id = Util.calcProtoId(facde, executableElement);
      String methodName = executableElement.getSimpleName().toString();

      MethodSpec.Builder handlerMethod = MethodSpec
          .methodBuilder(methodName)
          .returns(byte[].class)
          .addParameter(CONNECTION_PARAM_SPEC)
          .addParameter(MESSAGE_PARAM_SPEC)
          .addException(Exception.class);

      invoker.addStatement("case $L -> $L($L, $L)", id, methodName, CONNECTION_VAR_NAME,
          MESSAGE_VAR_NAME);

      List<? extends VariableElement> params = executableElement.getParameters();
      if (!params.isEmpty()) {
        handlerMethod.addStatement("$T $L = $T.wrappedBuffer($L.packet())",
            Util.BYTE_BUF, BUF_VAR_NAME, Util.UNNPOOLED_UTIL, MESSAGE_VAR_NAME);
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
            case LONG ->
                handlerMethod.addStatement("long $L = $T.readInt64($L)", pname, BYTEBUF_UTIL,
                    BUF_VAR_NAME);
            default -> handlerMethod.addStatement("$T $L = $L.read($L)", TypeName.get(ptype), pname,
                SERIALIZER_VAR_NAME, BUF_VAR_NAME);
          }
        }
        handlerMethod.addCode("\n");
      }

      String paramStr = params.stream().map(p -> p.getSimpleName().toString())
          .collect(Collectors.joining(", "));
      TypeMirror returnType = executableElement.getReturnType();

      if (returnType.getKind() == TypeKind.VOID) {
        handlerMethod.addStatement("$L.$L($L)", FACADE_VAR_NAME, methodName, paramStr);
        handlerMethod.addStatement("return $T.EMPTY_BYTE_ARRAY", ArrayUtils.class);
      } else {
        String resVar = "res";
        String resBuf = "resBuf";

        switch (returnType.getKind()) {
          case BOOLEAN ->
              handlerMethod.addStatement("boolean $L = $L.$L($L)", resVar, FACADE_VAR_NAME,
                  methodName, paramStr);
          case BYTE -> handlerMethod.addStatement("byte $L = $L.$L($L)", resVar, FACADE_VAR_NAME,
              methodName, paramStr);
          case SHORT -> handlerMethod.addStatement("short $L = $L.$L($L)", resVar, FACADE_VAR_NAME,
              methodName, paramStr);
          case CHAR -> handlerMethod.addStatement("char $L = $L.$L($L)", resVar, FACADE_VAR_NAME,
              methodName, paramStr);
          case FLOAT -> handlerMethod.addStatement("float $L = $L.$L($L)", resVar, FACADE_VAR_NAME,
              methodName, paramStr);
          case DOUBLE ->
              handlerMethod.addStatement("double $L = $L.$L($L)", resVar, FACADE_VAR_NAME,
                  methodName, paramStr);
          case INT -> handlerMethod.addStatement("int $L = $L.$L($L)", resVar, FACADE_VAR_NAME,
              methodName, paramStr);
          case LONG -> handlerMethod.addStatement("long $L = $L.$L($L)", resVar, FACADE_VAR_NAME,
              methodName, paramStr);
          default ->
              handlerMethod.addStatement("$T $L = $L.$L($L)", TypeName.get(returnType), resVar,
                  FACADE_VAR_NAME,
                  methodName, paramStr);
        }

        handlerMethod
            .addCode("\n")
            .addStatement("$T $L = $T.DEFAULT.buffer()", Util.BYTE_BUF, resBuf, Util.POOLED_UTIL)
            .beginControlFlow("try");
        switch (returnType.getKind()) {
          case BOOLEAN -> handlerMethod.addStatement("$L.writeBoolean($L)", resBuf, resVar);
          case BYTE -> handlerMethod.addStatement("$L.writeByte($L)", resBuf, resVar);
          case SHORT -> handlerMethod.addStatement("$L.writeShort($L)", resBuf, resVar);
          case CHAR -> handlerMethod.addStatement("$L.writeChar($L)", resBuf, resVar);
          case FLOAT -> handlerMethod.addStatement("$L.writeFloat($L)", resBuf, resVar);
          case DOUBLE -> handlerMethod.addStatement("$L.writeDouble($L)", resBuf, resVar);
          case INT ->
              handlerMethod.addStatement("$T.writeInt32($L, $L)", BYTEBUF_UTIL, resBuf, resVar);
          case LONG ->
              handlerMethod.addStatement("$T.writeInt64($L, $L)", BYTEBUF_UTIL, resBuf, resVar);
          case DECLARED -> {
            DeclaredType declaredType = (DeclaredType) returnType;
            TypeElement typeElement = (TypeElement) declaredType.asElement();
            if (typeElement.getQualifiedName()
                .contentEquals(Util.COMPLETE_ABLE_FUTURE_TYPE.toString())) {
              handlerMethod.addStatement("$L.writeObject($L, $L.get())", SERIALIZER_VAR_NAME,
                  resBuf,
                  resVar
              );
            } else {
              handlerMethod.addStatement("$L.writeObject($L, $L)", SERIALIZER_VAR_NAME, resBuf,
                  resVar);
            }
          }
          default ->
              handlerMethod.addStatement("$L.writeObject($L, $L)", SERIALIZER_VAR_NAME, resBuf,
                  resVar);
        }
        handlerMethod
            .addStatement("return $T.readBytes($L)", BYTEBUF_UTIL, resBuf)
            .endControlFlow()
            .beginControlFlow("finally")
            .addStatement("$L.release()", resBuf)
            .endControlFlow();
      }

      handler.addMethod(handlerMethod.build());

      intList.add(id);
    }

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
        .returns(byte[].class)
        .addParameter(CONNECTION_PARAM_SPEC)
        .addParameter(MESSAGE_PARAM_SPEC)
        .addException(Exception.class)
        .beginControlFlow("switch($L.proto())", MESSAGE_VAR_NAME);

    String futureVarName = "futureVar";
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
          .addStatement("final int id = $L", id)
          .addStatement("final int msgId = $L.msgId()", MESSAGE_VAR_NAME)
          .addStatement("final $T $L = $L.removeInvokeFuture($L.msgId())", returnType,
              futureVarName, CONNECTION_VAR_NAME, MESSAGE_VAR_NAME)
          .beginControlFlow("if ($L == null)", futureVarName)
          .addStatement(
              "$L.error(\"寻找回调函数失败, 可能原因：【回调函数过期，回调函数不存在】, 协议ID:$L, 消息ID:{}, 链接地址:{} \", $L.msgId(),\n$L.channel().remoteAddress())",
              loggerVarName, id, MESSAGE_VAR_NAME, CONNECTION_VAR_NAME)
          .addStatement("return")
          .endControlFlow()
          .addStatement("$T $L = $T.wrappedBuffer($L.packet())",
              Util.BYTE_BUF, BUF_VAR_NAME, Util.UNNPOOLED_UTIL, MESSAGE_VAR_NAME)
          .addStatement("$L.complete($L.read($L))", futureVarName, SERIALIZER_VAR_NAME,
              BUF_VAR_NAME);

      intList.add(id);
      callBackHandleBuilder.addMethod(methodBuilder.build());
      invokeBuilder.addStatement("case $L -> $L($L, $L)", id, methodName, CONNECTION_VAR_NAME,
          MESSAGE_VAR_NAME);
    }

    if (callBackHandleBuilder.methodSpecs.isEmpty()) {
      return;
    }

    MethodSpec invoker = invokeBuilder
        .addStatement(
            "default -> throw new UnsupportedOperationException(\"【$L】无法回调处理消息，原因:【缺少对应方法】，消息ID:【%s】\".formatted($L.proto()))",
            facde.getSimpleName(), MESSAGE_VAR_NAME)
        .endControlFlow()
        .addCode(";")
        .addStatement("return $T.EMPTY_BYTE_ARRAY", ArrayUtils.class)
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

    String qualifiedName = "%s.%s".formatted(CALL_BACK_HANDLER_PACKAGE, callBackHandler.name);
    JavaFileObject file = processingEnv.getFiler().createSourceFile(qualifiedName);
    try (PrintWriter writer = new PrintWriter(file.openWriter())) {
      javaFile.writeTo(writer);
    }
  }
}
