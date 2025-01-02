package org.example.net.anno.processor;

import static org.example.net.Util.BYTEBUF_UTIL;
import static org.example.net.Util.BYTE_BUF;
import static org.example.net.Util.CONNECTION_CLASS_NAME;
import static org.example.net.Util.FACADE_VAR_NAME;
import static org.example.net.Util.MESSAGE_CLASS_NAME;
import static org.example.net.Util.MSG_ID_VAR_NAME;
import static org.example.net.Util.SERIALIZER_VAR_NAME;

import com.google.auto.service.AutoService;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.netty.util.ReferenceCountUtil;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import org.example.net.Util;

/**
 * 负责RPC方法的调用类和代理类
 * <p>
 *
 * @author zhongjianping
 * @since 2024/8/9 11:19
 */
@SupportedAnnotationTypes("org.example.net.anno.Rpc")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class RpcHandlerProcessor extends AbstractProcessor {

  private static final String CONNECTION_VAR_NAME = "c";
  private static final String MESSAGE_VAR_NAME = "m";
  private static final String BUF_VAR_NAME = "b";
  private static final String PROTOS_VAR_NAME = "protos";
  private static final String RUNNABLE_VAR_NAME = "r";

  private static final ParameterSpec CONNECTION_PARAM_SPEC = ParameterSpec.builder(
      CONNECTION_CLASS_NAME,
      CONNECTION_VAR_NAME).build();
  private static final ParameterSpec MESSAGE_PARAM_SPEC = ParameterSpec.builder(
      MESSAGE_CLASS_NAME,
      MESSAGE_VAR_NAME).build();

  private Set<String> set = Collections.emptySet();

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    set = new ConcurrentSkipListSet<>();
  }


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

        List<ExecutableElement> methodElements = Util.getReqMethod(processingEnv, facade);
        if (methodElements.isEmpty()) {
          continue;
        }

        set.add(facade.toString());

        try {
          buildHandler(facade, methodElements);
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

    if (roundEnv.processingOver()) {
      try {
        FileObject fileObject = processingEnv.getFiler()
            .createResource(StandardLocation.CLASS_OUTPUT, "",
                "META-INF/services/testssss");

        try (PrintWriter writer = new PrintWriter(fileObject.openWriter())) {
          for (String s : set) {
            writer.println(s);
          }
          writer.println(Instant.now().toString());
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    return false;
  }

  private void buildHandler(TypeElement facade, List<ExecutableElement> elements)
      throws IOException {
    TypeName facdeTypeName = TypeName.get(facade.asType());
    String qualifiedName = facade.getQualifiedName().toString() + "Handler";
    int lastIdx = qualifiedName.lastIndexOf('.');
    String packet = qualifiedName.substring(0, lastIdx);
    String simpleName = qualifiedName.substring(lastIdx + 1);

    TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(simpleName)
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

    TypeSpecInfo info = buildTypeSpecInfo(facade, typeSpecBuilder, elements);

    buildtMethod(info);

    JavaFile javaFile = JavaFile.builder(packet, typeSpecBuilder.build())
        .build();

    JavaFileObject file = processingEnv.getFiler().createSourceFile(qualifiedName);
    try (PrintWriter writer = new PrintWriter(file.openWriter())) {
      javaFile.writeTo(writer);
    }
  }

  TypeSpecInfo buildTypeSpecInfo(TypeElement typeElement, TypeSpec.Builder builder,
      List<ExecutableElement> elements) {
    TypeSpecInfo info = new TypeSpecInfo(typeElement, builder, elements);

    ExecutorSupplierUtil.buildExecuotSupplerCode(processingEnv, info);

    return info;
  }

  void buildtMethod(TypeSpecInfo info) {
    MethodSpec.Builder invoker = MethodSpec.methodBuilder("invoke")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(CONNECTION_PARAM_SPEC)
        .addParameter(MESSAGE_PARAM_SPEC)
        .addException(Exception.class);

    invoker.beginControlFlow("switch(m.proto())");
    IntList intList = buildMethod0(info, invoker);

    invoker
        .addStatement(
            "default -> throw new UnsupportedOperationException(\"【$L】无法处理消息，原因:【缺少对应方法】，消息ID:【%s】\".formatted($L.proto()))",
            info.typeElement.getSimpleName(), MESSAGE_VAR_NAME)
        .endControlFlow().addCode(";");

    info.builder
        .addField(FieldSpec.builder(int[].class, PROTOS_VAR_NAME)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .initializer("new int[]{$L}",
                intList.intStream().mapToObj(String::valueOf).collect(Collectors.joining(",")))
            .build())
        .addMethod(invoker.build());
  }

  private static IntList buildMethod0(TypeSpecInfo info,
      MethodSpec.Builder handlerMethod) {
    IntList intList = new IntArrayList();
    for (ExecutableElement element : info.methods) {
      final int id = Util.calcProtoId(info.typeElement, element);
      Name methodName = element.getSimpleName();

      MethodSpec.Builder methodBuilder = MethodSpec
          .methodBuilder(methodName.toString())
          .addParameter(CONNECTION_PARAM_SPEC)
          .addParameter(MESSAGE_PARAM_SPEC)
          .addModifiers(Modifier.PRIVATE);

      handlerMethod.addStatement("case $L -> $L($L, $L)", id, methodName, CONNECTION_VAR_NAME,
          MESSAGE_VAR_NAME);

      methodBuilder.addStatement("$T $L = $L.packet()", BYTE_BUF, BUF_VAR_NAME, MESSAGE_VAR_NAME);

      if (hasReturnValue(element)) {
        methodBuilder.addStatement("int $L = $T.readInt32($L)", MSG_ID_VAR_NAME, BYTEBUF_UTIL,
            BUF_VAR_NAME);
      }

      List<? extends VariableElement> params = element.getParameters();
      for (VariableElement p : params) {
        final String pname = p.getSimpleName().toString();
        TypeMirror ptype = p.asType();
        switch (ptype.getKind()) {
          case BOOLEAN ->
              methodBuilder.addStatement("boolean $L = $L.readBoolean()", pname, BUF_VAR_NAME);
          case BYTE -> methodBuilder.addStatement("byte $L = $L.readByte()", pname, BUF_VAR_NAME);
          case SHORT ->
              methodBuilder.addStatement("short $L = $L.readShort()", pname, BUF_VAR_NAME);
          case CHAR -> methodBuilder.addStatement("char $L = $L.readChar()", pname, BUF_VAR_NAME);
          case FLOAT ->
              methodBuilder.addStatement("float $L = $L.readFloat()", pname, BUF_VAR_NAME);
          case DOUBLE ->
              methodBuilder.addStatement("double $L = $L.readDouble()", pname, BUF_VAR_NAME);
          case INT -> methodBuilder.addStatement("int $L = $T.readInt32($L)", pname, BYTEBUF_UTIL,
              BUF_VAR_NAME);
          case LONG -> methodBuilder.addStatement("long $L = $T.readInt64($L)", pname, BYTEBUF_UTIL,
              BUF_VAR_NAME);
          default -> {
            TypeMirror paramType = p.asType();
            TypeName paramTypeName = TypeName.get(paramType);

            if (paramTypeName.equals(CONNECTION_CLASS_NAME)) {
              methodBuilder.addStatement("$T $L = $L", CONNECTION_CLASS_NAME, pname,
                  CONNECTION_VAR_NAME);
            } else if (paramTypeName.equals(MESSAGE_CLASS_NAME)) {
              methodBuilder.addStatement("$T $L = $L", MESSAGE_CLASS_NAME, pname,
                  MESSAGE_VAR_NAME);
            } else {
              methodBuilder.addStatement("$T $L = $L.readObject($L)", TypeName.get(ptype), pname,
                  SERIALIZER_VAR_NAME, BUF_VAR_NAME);
            }
          }
        }
      }
      if (!params.isEmpty()) {
        methodBuilder.addCode("\n");
      }

      CodeBlock.Builder invokeCodeBlock = buildInvokeCodeBlock(element);
      if (info.executor != null) {
        methodBuilder
            .addCode("$T $L = () ->", Runnable.class, RUNNABLE_VAR_NAME)
            .beginControlFlow("")
            .addCode(invokeCodeBlock.build())
            .endControlFlow("")
            .addStatement("$L.execute($L)", info.executor.apply(element), RUNNABLE_VAR_NAME);
      } else {
        methodBuilder.addCode(invokeCodeBlock.build());
      }

      info.builder.addMethod(methodBuilder.build());

      intList.add(id);
    }
    return intList;
  }

  /**
   * 调用指定的方法
   *
   * @since 2024/12/4 11:01
   */
  private static CodeBlock.Builder buildInvokeCodeBlock(ExecutableElement executableElement) {
    String paramStr = executableElement.getParameters().stream()
        .map(p -> p.getSimpleName().toString())
        .collect(Collectors.joining(", "));
    Name methodName = executableElement.getSimpleName();

    CodeBlock.Builder codeBlock = CodeBlock.builder();
    if (hasReturnValue(executableElement)) {
      String resVarName = "res";
      String resBuf = "resBuf";
      TypeMirror returnTypeMirror = executableElement.getReturnType();
      switch (returnTypeMirror.getKind()) {
        case BOOLEAN ->
            codeBlock.addStatement("boolean $L = $L.$L($L)", resVarName, FACADE_VAR_NAME,
                methodName, paramStr);
        case BYTE -> codeBlock.addStatement("byte $L = $L.$L($L)", resVarName, FACADE_VAR_NAME,
            methodName, paramStr);
        case SHORT -> codeBlock.addStatement("short $L = $L.$L($L)", resVarName, FACADE_VAR_NAME,
            methodName, paramStr);
        case CHAR -> codeBlock.addStatement("char $L = $L.$L($L)", resVarName, FACADE_VAR_NAME,
            methodName, paramStr);
        case FLOAT -> codeBlock.addStatement("float $L = $L.$L($L)", resVarName, FACADE_VAR_NAME,
            methodName, paramStr);
        case DOUBLE -> codeBlock.addStatement("double $L = $L.$L($L)", resVarName, FACADE_VAR_NAME,
            methodName, paramStr);
        case INT -> codeBlock.addStatement("int $L = $L.$L($L)", resVarName, FACADE_VAR_NAME,
            methodName, paramStr);
        case LONG -> codeBlock.addStatement("long $L = $L.$L($L)", resVarName, FACADE_VAR_NAME,
            methodName, paramStr);
        default -> codeBlock.addStatement("$T $L = $L.$L($L)", TypeName.get(returnTypeMirror),
            resVarName,
            FACADE_VAR_NAME,
            methodName, paramStr);
      }

      codeBlock
          .add("\n")
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
      codeBlock.addStatement("$L.$L($L)", FACADE_VAR_NAME, methodName, paramStr);
    }

    return codeBlock;
  }

  private static boolean hasReturnValue(ExecutableElement executableElement) {
    return executableElement.getReturnType().getKind() != TypeKind.VOID;
  }

}
