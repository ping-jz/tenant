package org.example.net.anno.processor;

import static org.example.net.Util.FACADE_VAR_NAME;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.tuple.Pair;
import org.example.net.Util;

final class ExecutorSupplierUtil {

  private ExecutorSupplierUtil() {
  }

  public static void buildExecuotSupplerCode(ProcessingEnvironment processingEnv,
      TypeSpecInfo info) {
    List<Pair<TypeMirror, TypeName>> supplierInterfaces = executorSupplierInters(
        processingEnv, info);
    switch (supplierInterfaces.size()) {
      case 1 -> {
        Pair<TypeMirror, TypeName> pair = supplierInterfaces.getFirst();
        TypeName typeName = pair.getRight();
        TypeName rawType = typeName;
        if (rawType instanceof ParameterizedTypeName t) {
          rawType = t.rawType();
        }

        if (rawType.equals(Util.EXECUTOR_SUPPLIER_CLASS_NAME)) {
          info.executor = ignore -> CodeBlock.builder()
              .add("$L.get()", FACADE_VAR_NAME)
              .build();
        } else if (rawType.equals(Util.FIRST_ARG_EXECUTOR_SUPPLIER_CLASS_NAME)) {
          argExecutorSupplerCheck(processingEnv, info, pair);
          info.executor = method -> {
            return CodeBlock.builder()
                .add("$L.get($L)",
                    FACADE_VAR_NAME,
                    method.getParameters().getFirst().toString())
                .build();
          };
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
              .formatted(Util.EXECUTOR_SUPPLIER_CLASS_NAME,
                  Util.FIRST_ARG_EXECUTOR_SUPPLIER_CLASS_NAME),
          info.typeElement);
      default -> processingEnv.getMessager().printError(
          "重复ExecutorSuppler接口，请选择实现其中之一保留：%s"
              .formatted(supplierInterfaces
                  .stream()
                  .map(Pair::getLeft)
                  .map(TypeMirror::toString)
                  .collect(Collectors.joining(", "))),
          info.typeElement);
    }
  }

  private static List<Pair<TypeMirror, TypeName>> executorSupplierInters(
      ProcessingEnvironment processingEnv,
      TypeSpecInfo info) {
    List<Pair<TypeMirror, TypeName>> supplierInterface = new ArrayList<>();

    for (TypeMirror inter : info.typeElement.getInterfaces()) {
      List<TypeName> supplier = getExecutorSupplierInter(processingEnv, inter);
      switch (supplier.size()) {
        case 0 -> {
          continue;
        }
        case 1 -> supplierInterface.add(Pair.of(inter, supplier.getFirst()));
        default -> {
          processingEnv.getMessager().printError(
              "重复ExecutorSuppler接口，请选择实现其中之一保留：%s"
                  .formatted(supplier.stream().map(TypeName::toString)
                      .collect(Collectors.joining(", "))),
              processingEnv.getTypeUtils().asElement(inter));
          continue;
        }
      }

    }
    return supplierInterface;
  }

  private static List<TypeName> getExecutorSupplierInter(ProcessingEnvironment processingEnv,
      TypeMirror orginInter) {
    Queue<TypeMirror> typeMirrors = new ArrayDeque<>();
    typeMirrors.add(orginInter);

    List<TypeName> supplierInterface = new ArrayList<>();
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
        typeMirrors.addAll(processingEnv.getTypeUtils().directSupertypes(inter));
      }
    }

    return supplierInterface;
  }

  private static void argExecutorSupplerCheck(ProcessingEnvironment processingEnv,
      TypeSpecInfo info, Pair<TypeMirror, TypeName> interAndrootInter) {

    TypeMirror inter = interAndrootInter.getLeft();
    TypeName rootInter = interAndrootInter.getRight();
    TypeName name = Util.OBJECT;
    if (rootInter instanceof ParameterizedTypeName p) {
      name = p.typeArguments().getFirst();
    }

    for (ExecutableElement element : info.methods) {
      List<? extends VariableElement> parameters = element.getParameters();
      if (parameters.isEmpty()) {
        processingEnv.getMessager()
            .printError(
                "方法参数不能为空，并且类型为：%s, 详细定义请查询：%s".formatted(name, inter),
                element);
        continue;
      }

      VariableElement p0 = parameters.getFirst();
      TypeName paramTypeName = TypeName.get(p0.asType());
      if (!paramTypeName.equals(name)) {
        processingEnv.getMessager()
            .printError(
                "参数：%s，提供的类型：%s,需要的类型：%s, 详细定义请查询：%s".formatted(
                    p0.getSimpleName(),
                    paramTypeName, name, inter
                ),
                p0);
      }
    }
  }

}
