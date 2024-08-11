package org.example.net.anno.processor;

import static org.example.net.anno.processor.Util.BYTEBUF_UTIL;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
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
@SupportedAnnotationTypes("org.example.net.anno.RpcModule")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class RpcHandlerProcessor extends AbstractProcessor {

  private static final String FACADE_FIELD_NAME = "facade";
  private static final String SERIALIZER_FIELD_NAME = "serializer";
  private static final String CONNECTION_PARAM_NAME = "c";
  private static final String MESSAGE_PARAM_NAME = "m";

  private static final ParameterSpec CONNECTION_PARAM_SPEC = ParameterSpec.builder(Util.CONNECTION,
      CONNECTION_PARAM_NAME).build();
  private static final ParameterSpec MESSAGE_PARAM_SPEC = ParameterSpec.builder(Util.MESSAGE,
      MESSAGE_PARAM_NAME).build();


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
        TypeName facdeTypeName = TypeName.get(facade.asType());
        try {
          String qualifiedName = facade.getQualifiedName().toString() + "Handler";
          int lastIdx = qualifiedName.lastIndexOf('.');
          String packet = qualifiedName.substring(0, lastIdx);
          String simpleName = qualifiedName.substring(lastIdx + 1);

          TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(simpleName)
              .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
              .addField(FieldSpec
                  .builder(facdeTypeName, FACADE_FIELD_NAME)
                  .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                  .build())
              .addField(FieldSpec
                  .builder(Util.COMMON_SERIALIZER, SERIALIZER_FIELD_NAME)
                  .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                  .build())
              .addMethod(MethodSpec.constructorBuilder()
                  .addModifiers(Modifier.PUBLIC)
                  .addParameter(facdeTypeName, FACADE_FIELD_NAME)
                  .addParameter(Util.COMMON_SERIALIZER, SERIALIZER_FIELD_NAME)
                  .addStatement("this.$L = $L", FACADE_FIELD_NAME, FACADE_FIELD_NAME)
                  .addStatement("this.$L = $L", SERIALIZER_FIELD_NAME, SERIALIZER_FIELD_NAME)
                  .build());

          generateMethod(facade, typeSpecBuilder, Util.getReqMethod(processingEnv, facade));

          JavaFile javaFile = JavaFile.builder(packet, typeSpecBuilder.build())
              .build();

          JavaFileObject file = processingEnv.getFiler().createSourceFile(qualifiedName);
          try (PrintWriter writer = new PrintWriter(file.openWriter())) {
            javaFile.writeTo(writer);
          }
        } catch (Exception e) {
          processingEnv.getMessager()
              .printError(
                  "[%S] %s build invoker error, %s".formatted(getClass(),
                      facade.getQualifiedName(),
                      e.fillInStackTrace()), facade);
        }
      }
    }

    return false;
  }

  public void generateMethod(TypeElement facde, TypeSpec.Builder handler,
      List<Element> methods) {
    handler.addSuperinterface(Util.HANDLER_INTERFACE);

    MethodSpec.Builder invoker = MethodSpec.methodBuilder("invoke")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(byte[].class)
        .addParameter(CONNECTION_PARAM_SPEC)
        .addParameter(MESSAGE_PARAM_SPEC);

    invoker.beginControlFlow("return switch(m.proto())");
    for (Element element : methods) {
      ExecutableElement executableElement = (ExecutableElement) element;
      final int id = Util.calcProtoId(facde, executableElement);
      String methodName = executableElement.getSimpleName().toString();

      String handlerMethodName = methodName + '_' + id;
      MethodSpec.Builder handlerMethod = MethodSpec
          .methodBuilder(handlerMethodName)
          .returns(byte[].class)
          .addParameter(CONNECTION_PARAM_SPEC)
          .addParameter(MESSAGE_PARAM_SPEC);

      invoker.addStatement("case $L -> $L($L, $L)", id, handlerMethodName, CONNECTION_PARAM_NAME,
          MESSAGE_PARAM_NAME);

      List<? extends VariableElement> params = executableElement.getParameters();
      if (!params.isEmpty()) {
        String bufVarName = "buf";
        handlerMethod.addStatement("$T $L = $T.wrappedBuffer($L.packet())",
            Util.BYTE_BUF, bufVarName, Util.UNNPOOLED_UTIL, MESSAGE_PARAM_NAME);
        for (VariableElement p : params) {
          final String pname = p.getSimpleName().toString();
          TypeMirror ptype = p.asType();
          switch (ptype.getKind()) {
            case BOOLEAN ->
                handlerMethod.addStatement("boolean $L = $L.readBoolean()", pname, bufVarName);
            case BYTE -> handlerMethod.addStatement("byte $L = $L.readByte()", pname, bufVarName);
            case SHORT ->
                handlerMethod.addStatement("short $L = $L.readShort()", pname, bufVarName);
            case CHAR -> handlerMethod.addStatement("char $L = $L.readChar()", pname, bufVarName);
            case FLOAT ->
                handlerMethod.addStatement("float $L = $L.readFloat()", pname, bufVarName);
            case DOUBLE ->
                handlerMethod.addStatement("double $L = $L.readDouble()", pname, bufVarName);
            case INT -> handlerMethod.addStatement("int $L = $T.readInt32($L)", pname, BYTEBUF_UTIL,
                bufVarName);
            case LONG ->
                handlerMethod.addStatement("long $L = $T.readInt64($L)", pname, BYTEBUF_UTIL,
                    bufVarName);
            default -> handlerMethod.addStatement("$T $L = $L.read($L)", TypeName.get(ptype), pname,
                SERIALIZER_FIELD_NAME, bufVarName);
          }
        }
        handlerMethod.addCode("\n");
      }

      String paramStr = params.stream().map(p -> p.getSimpleName().toString())
          .collect(Collectors.joining(", "));
      TypeMirror returnType = executableElement.getReturnType();


      if (returnType.getKind() == TypeKind.VOID) {
        handlerMethod.addStatement("$L.$L($L)", FACADE_FIELD_NAME, methodName, paramStr);
        handlerMethod.addStatement("return $T.EMPTY_BYTE_ARRAY", ArrayUtils.class);
      } else {
        String resVar = "res";
        String resBuf = "resBuf";

        switch (returnType.getKind()) {
          case BOOLEAN ->
              handlerMethod.addStatement("boolean $L = $L.$L($L)", resVar, FACADE_FIELD_NAME,
                  methodName, paramStr);
          case BYTE -> handlerMethod.addStatement("byte $L = $L.$L($L)", resVar, FACADE_FIELD_NAME,
              methodName, paramStr);
          case SHORT ->
              handlerMethod.addStatement("short $L = $L.$L($L)", resVar, FACADE_FIELD_NAME,
                  methodName, paramStr);
          case CHAR -> handlerMethod.addStatement("char $L = $L.$L($L)", resVar, FACADE_FIELD_NAME,
              methodName, paramStr);
          case FLOAT ->
              handlerMethod.addStatement("float $L = $L.$L($L)", resVar, FACADE_FIELD_NAME,
                  methodName, paramStr);
          case DOUBLE ->
              handlerMethod.addStatement("double $L = $L.$L($L)", resVar, FACADE_FIELD_NAME,
                  methodName, paramStr);
          case INT -> handlerMethod.addStatement("int $L = $L.$L($L)", resVar, FACADE_FIELD_NAME,
              methodName, paramStr);
          case LONG -> handlerMethod.addStatement("long $L = $L.$L($L)", resVar, FACADE_FIELD_NAME,
              methodName, paramStr);
          default ->
              handlerMethod.addStatement("$T $L = $L.$L($L)", TypeName.get(returnType), resVar,
                  FACADE_FIELD_NAME,
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
          default ->
              handlerMethod.addStatement("$L.writeObject($L, $L)", SERIALIZER_FIELD_NAME, resBuf,
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
    }

    invoker
        .addStatement(
            "default -> throw new UnsupportedOperationException(\"【$L】无法处理消息，原因:【缺少对应方法】，消息ID:【%s】\".formatted($L.proto()))",
            facde.getSimpleName(), MESSAGE_PARAM_NAME)
        .endControlFlow().addCode(";");

    handler.addMethod(invoker.build());
  }
}
