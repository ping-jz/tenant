package org.example.net.anno.processor;

import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.tuple.Pair;
import org.example.net.Util;

final class ExecutorSupplierUtil {

  private ExecutorSupplierUtil() {
  }

  public static List<Pair<TypeMirror, TypeName>> executorSupplierInters(
      ProcessingEnvironment processingEnv,
      TypeSpecInfo info) {
    List<Pair<TypeMirror, TypeName>> supplierInterface = new ArrayList<>();

    for (TypeMirror mirror : info.typeElement.getInterfaces()) {
      List<TypeName> supplier = getExecutorSupplierInter(processingEnv, mirror);
      switch (supplier.size()) {
        case 0 -> {
          continue;
        }
        case 1 -> supplierInterface.add(Pair.of(mirror, supplier.getFirst()));
        default -> {
          processingEnv.getMessager().printError(
              "重复ExecutorSuppler接口，请选择实现其中之一保留：%s"
                  .formatted(supplier.stream().map(TypeName::toString)
                      .collect(Collectors.joining(", "))),
              processingEnv.getTypeUtils().asElement(mirror));
          continue;
        }
      }

    }
    return supplierInterface;
  }

  public static List<TypeName> getExecutorSupplierInter(ProcessingEnvironment processingEnv,
      TypeMirror mirror) {
    Queue<TypeMirror> typeMirrors = new ArrayDeque<>();
    typeMirrors.add(mirror);

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
          || rawType.equals(Util.RPC_EXECUTOR_SUPPLIER_CLASS_NAME)
      ) {
        supplierInterface.add(typeName);
      } else {
        TypeElement typeElement = (TypeElement) processingEnv.getTypeUtils().asElement(inter);
        typeMirrors.addAll(typeElement.getInterfaces());
      }
    }

    return supplierInterface;
  }

  public static void firstArgExecutorSupplerCheck(ProcessingEnvironment processingEnv,
      TypeSpecInfo info, TypeName inter) {
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
