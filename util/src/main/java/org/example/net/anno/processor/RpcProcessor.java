package org.example.net.anno.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;
import org.example.net.anno.Req;

/**
 * 负责RPC方法的调用类和代理类
 *
 * @author zhongjianping
 * @since 2024/8/9 11:19
 */
@SupportedAnnotationTypes("org.example.net.anno.RpcModule")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class RpcProcessor extends AbstractProcessor {

  private static final String INVOKER_PACKAGE = "org.example.common.net.proxy.invoker";

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

        TypeElement typeElement = (TypeElement) clazz;
        List<Element> elements = getReqMethod(typeElement);

        TypeSpec invoker = generateInvoker(typeElement, elements);

        String simpleName = clazz.getSimpleName() + "Invoker";
        String qualifiedName = "%s.%s".formatted(INVOKER_PACKAGE, simpleName);
        TypeSpec.Builder typeSpecBuilder = TypeSpec
            .classBuilder(simpleName)
            .addJavadoc("@since $S", LocalDateTime.now())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addType(invoker);

        JavaFile javaFile = JavaFile.builder(INVOKER_PACKAGE, typeSpecBuilder.build()).build();
        try {
          JavaFileObject file = processingEnv.getFiler().createSourceFile(qualifiedName);
          try (PrintWriter writer = new PrintWriter(file.openWriter())) {
            javaFile.writeTo(writer);
          }
        } catch (IOException e) {
          processingEnv.getMessager()
              .printError(
                  "[rpc] %s build invoker error, %s".formatted(typeElement.getQualifiedName(),
                      e.toString()), typeElement);
        }
      }

    }

    return false;
  }

  public TypeSpec generateInvoker(TypeElement typeElement, List<Element> methods) {
    String simpleName = "Invoker";

    TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(simpleName)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    for (Element e : methods) {
      String methodName = e.getSimpleName().toString();
      ExecutableElement method = (ExecutableElement) e;
      MethodSpec.Builder methodBuilder = MethodSpec
          .methodBuilder(e.getSimpleName().toString())
          .addModifiers(Modifier.PUBLIC);

      //Handle ID
      Req reqAnno = method.getAnnotation(Req.class);
      int id = reqAnno.value();
      if (id == 0) {
        id = Math.abs((typeElement.getQualifiedName() + methodName).hashCode());
      }
      methodBuilder.addStatement("final int id = $L", id);

      //Handle param
      for (VariableElement variableElement : method.getParameters()) {
        methodBuilder.addParameter(TypeName.get(variableElement.asType()),
            variableElement.getSimpleName().toString());
      }

      typeBuilder.addMethod(methodBuilder.build());
    }

    return typeBuilder.build();
  }

  public List<Element> getReqMethod(TypeElement typeElement) {
    List<Element> res = new ArrayList<>();

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

      res.add(element);
    }

    return res;
  }

}
