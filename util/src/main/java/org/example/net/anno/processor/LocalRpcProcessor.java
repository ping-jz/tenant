package org.example.net.anno.processor;

import com.google.auto.service.AutoService;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import org.example.net.Util;

/**
 * 负责RPC方法的调用类和代理类
 * <p>
 *
 * @author zhongjianping
 * @since 2024/8/9 11:19
 */
@SupportedAnnotationTypes("org.example.net.anno.LocalRpc")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class LocalRpcProcessor extends AbstractProcessor {


  private static final String INVOKER_SUBFIX = "Invoker";
  private static final String LOCAL_SUBFIX = "Local";
  private static final String FACADE_VAR_NAME = "f";
  private static final String RUNNABLE_VAR_NAME = "r";
  private static final String PARAM_PREFIX = "a_";

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

    for (TypeElement annotation : annotations) {
      Set<? extends Element> annotationElements = roundEnv.getElementsAnnotatedWith(annotation);
      if (annotationElements.isEmpty()) {
        continue;
      }
      for (Element clazz : annotationElements) {
        TypeElement typeElement = (TypeElement) clazz;
        try {
          List<ExecutableElement> elements = Util.getReqMethod(processingEnv, typeElement);

          buildInvoker(typeElement, elements);
        } catch (Exception e) {
          processingEnv.getMessager()
              .printError(
                  "[%s] %s build invoker error, %s".formatted(getClass(),
                      typeElement.getQualifiedName(),
                      Arrays.stream(
                              e.getStackTrace()).map(Objects::toString)
                          .collect(Collectors.joining("\n"))), typeElement);
        }
      }
    }

    return false;
  }

  public void buildInvoker(TypeElement typeElement, List<ExecutableElement> elements)
      throws Exception {
    ClassName typeName = ClassName.get(typeElement);
    TypeSpec invoker = buildInvoker0(typeElement, elements).build();
    ClassName invokerClassName = ClassName.get(typeName.packageName(), invoker.name());

    JavaFile javaFile = JavaFile.builder(invokerClassName.packageName(), invoker).build();
    JavaFileObject file = processingEnv.getFiler()
        .createSourceFile(invokerClassName.canonicalName());
    try (PrintWriter writer = new PrintWriter(file.openWriter())) {
      javaFile.writeTo(writer);
    }
  }

  TypeSpec.Builder buildInvoker0(TypeElement typeElement, List<ExecutableElement> methods) {
    final String simpleName;
    if (typeElement.getSimpleName().toString().endsWith(LOCAL_SUBFIX)) {
      simpleName = typeElement.getSimpleName().toString() + INVOKER_SUBFIX;
    } else {
      simpleName = typeElement.getSimpleName().toString() + LOCAL_SUBFIX + INVOKER_SUBFIX;
    }
    TypeSpec.Builder typeBuilder = TypeSpec
        .classBuilder(simpleName)
        .addAnnotation(Util.COMPONENT_ANNOTATION)
        .addJavadoc("{@link $T}\n", typeElement)
        .addJavadoc("@since $S", LocalDateTime.now())
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    TypeSpecInfo info = new TypeSpecInfo(typeElement, typeBuilder, methods);
    buildExecuotSupplerCode(info);
    buildConstructor(info);

    for (ExecutableElement method : methods) {
      Name methodName = method.getSimpleName();
      TypeMirror returnTypeMirror = method.getReturnType();
      TypeName returnTypeName = TypeName.get(returnTypeMirror);
      boolean hasReutrn = returnTypeMirror.getKind() != TypeKind.VOID;

      MethodSpec.Builder methodBuilder = MethodSpec
          .methodBuilder(methodName.toString())

          .addModifiers(Modifier.PUBLIC)
          .addJavadoc("{@link $T#$L}", typeElement, methodName);

      int idx = 0;
      for (VariableElement variableElement : method.getParameters()) {
        String pname = PARAM_PREFIX + idx;
        TypeName paramTypeName = TypeName.get(variableElement.asType());
        methodBuilder
            .addParameter(paramTypeName, variableElement.getSimpleName().toString())
            .addStatement("$T $L = $L", paramTypeName, pname, variableElement.getSimpleName())
        ;
        idx += 1;
      }

      if (info.executor == null) {
        if (hasReutrn) {
          methodBuilder.addCode("return ");
        }
        methodBuilder
            .returns(returnTypeName)
            .addStatement(
                buildInvokeCodeBlock(method)
                    .build()
            );
      } else {
        if (hasReutrn) {
          ParameterizedTypeName returnType = ParameterizedTypeName
              .get(
                  Util.COMPLETE_ABLE_FUTURE_CLASS_NAME,
                  returnTypeName.box()
              );

          ParameterizedTypeName supplierType = ParameterizedTypeName
              .get(
                  Util.SUPPLIER_CLASS_NAME,
                  returnTypeName.box()
              );

          methodBuilder
              .returns(returnType)
              .addCode("$T $L = () ->", supplierType, RUNNABLE_VAR_NAME)
              .beginControlFlow("")
              .addStatement("return $L", buildInvokeCodeBlock(method).build())
              .endControlFlow("")
              .addStatement("return $T.supplyAsync($L, $L)", Util.COMPLETE_ABLE_FUTURE_CLASS_NAME,
                  RUNNABLE_VAR_NAME, info.executor)
          ;

        } else {
          methodBuilder
              .addCode("$T $L = () ->", Runnable.class, RUNNABLE_VAR_NAME)
              .beginControlFlow("")
              .addStatement(buildInvokeCodeBlock(method).build())
              .endControlFlow("")
              .addStatement("$L.execute($L)", info.executor, RUNNABLE_VAR_NAME);
        }
      }

      typeBuilder.addMethod(methodBuilder.build());
    }

    return typeBuilder;
  }

  void buildConstructor(TypeSpecInfo info) {
    TypeName typeName = TypeName.get(info.typeElement.asType());

    info.builder.addField(
        FieldSpec
            .builder(typeName, FACADE_VAR_NAME)
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .build()
    );

    info.builder.addMethod(
        MethodSpec.constructorBuilder()
            .addParameter(typeName, FACADE_VAR_NAME)
            .addStatement("this.$L = $L", FACADE_VAR_NAME, FACADE_VAR_NAME)
            .build()
    );
  }

  /**
   * 调用指定的方法
   *
   * @since 2024/12/4 11:01
   */
  private static CodeBlock.Builder buildInvokeCodeBlock(ExecutableElement method) {
    String paramStr = IntStream.range(0, method.getParameters().size())
        .mapToObj(idx -> PARAM_PREFIX + idx)
        .collect(Collectors.joining(", "));

    CodeBlock.Builder codeBlock = CodeBlock.builder();
    codeBlock.add("this.$L.$L($L)", FACADE_VAR_NAME, method.getSimpleName().toString(), paramStr);

    return codeBlock;
  }

  private void buildExecuotSupplerCode(TypeSpecInfo info) {
    List<TypeName> supplierInterfaces = executorSupplierInters(info);
    switch (supplierInterfaces.size()) {
      case 1 -> {
        TypeName typeName = supplierInterfaces.getFirst();
        TypeName rawType = typeName;
        if (rawType instanceof ParameterizedTypeName t) {
          rawType = t.rawType();
        }
        if (rawType.equals(Util.EXECUTOR_SUPPLIER_CLASS_NAME)) {
          info.executor = CodeBlock.builder()
              .add("$L.get()",
                  FACADE_VAR_NAME)
              .build();
        } else if (rawType.equals(Util.FIRST_ARG_EXECUTOR_SUPPLIER_CLASS_NAME)) {
          firstArgExecutorSupplerCheck(info, typeName);
          info.executor = CodeBlock.builder()
              .add("$L.get($L)",
                  FACADE_VAR_NAME,
                  PARAM_PREFIX + 0)
              .build();
        } else if (typeName.equals(Util.RAW_EXECUTOR_SUPPLIER_CLASS_NAME)) {
          info.executor = null;
        } else {
          processingEnv.getMessager()
              .printError("不支持的ExecutorSuppler接口：%s, 请联系作者".formatted(typeName),
                  info.typeElement);
        }
      }
      case 0 -> processingEnv.getMessager().printError(
          "缺少ExecutorSuppler接口，请选择实现其中之一：%s, %s"
              .formatted(Util.EXECUTOR_SUPPLIER_CLASS_NAME, Util.RPC_EXECUTOR_SUPPLIER_CLASS_NAME),
          info.typeElement);
      default -> processingEnv.getMessager().printError(
          "重复ExecutorSuppler接口，请选择实现其中之一保留：%s"
              .formatted(supplierInterfaces.stream().map(TypeName::toString)
                  .collect(Collectors.joining(", "))),
          info.typeElement);
    }
  }

  List<TypeName> executorSupplierInters(TypeSpecInfo info) {
    List<TypeName> supplierInterface = new ArrayList<>();

    Queue<TypeMirror> typeMirrors = new ArrayDeque<>(info.typeElement.getInterfaces());

    while (!typeMirrors.isEmpty()) {
      TypeMirror inter = typeMirrors.poll();
      TypeName typeName = TypeName.get(inter);
      TypeName rawType = typeName;
      if (typeName instanceof ParameterizedTypeName t) {
        rawType = t.rawType();
      }
      if (rawType.equals(Util.EXECUTOR_SUPPLIER_CLASS_NAME)
          || rawType.equals(Util.FIRST_ARG_EXECUTOR_SUPPLIER_CLASS_NAME)
          || rawType.equals(Util.RAW_EXECUTOR_SUPPLIER_CLASS_NAME)
      ) {
        supplierInterface.add(typeName);
      } else {
        TypeElement typeElement = (TypeElement) processingEnv.getTypeUtils().asElement(inter);
        typeMirrors.addAll(typeElement.getInterfaces());
      }
    }

    return supplierInterface;
  }

  void firstArgExecutorSupplerCheck(TypeSpecInfo info, TypeName inter) {
    ParameterizedTypeName parameterizedTypeName = null;
    if (inter instanceof ParameterizedTypeName p) {
      parameterizedTypeName = p;
    } else {
      return;
    }

    TypeName name = parameterizedTypeName.typeArguments().getFirst();
    for (ExecutableElement element : info.methods) {
      List<? extends VariableElement> parameters = element.getParameters();
      if (parameters.isEmpty()) {
        processingEnv.getMessager()
            .printError("方法参数不能为空，并且类型为：%s, 详情定义：%s".formatted(name, inter),
                element);
        continue;
      }

      VariableElement p0 = parameters.getFirst();
      TypeName paramTypeName = TypeName.get(p0.asType());
      if (!paramTypeName.equals(name)) {
        processingEnv.getMessager()
            .printError(
                "参数：%s，提供的类型：%s,需要的类型：%s, 详情定义：%s".formatted(p0.getSimpleName(),
                    paramTypeName, name, inter
                ),
                p0);
      }
    }
  }

}
