package org.example.net.anno.processor;

import static org.example.net.anno.processor.Util.BASE_REMOTING;
import static org.example.net.anno.processor.Util.BYTEBUF_UTIL;
import static org.example.net.anno.processor.Util.BYTE_BUF;
import static org.example.net.anno.processor.Util.COMMON_SERIALIZER;
import static org.example.net.anno.processor.Util.CONNECTION;
import static org.example.net.anno.processor.Util.CONNECTION_GETTER;
import static org.example.net.anno.processor.Util.LOGGER;
import static org.example.net.anno.processor.Util.LOGGER_FACTOR;
import static org.example.net.anno.processor.Util.MESSAGE;
import static org.example.net.anno.processor.Util.POOLED_UTIL;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;
import org.example.net.anno.Req;

/**
 * 负责RPC方法的调用类和代理类
 * <p>
 *
 * @author zhongjianping
 * @since 2024/8/9 11:19
 */
@SupportedAnnotationTypes("org.example.net.anno.RpcModule")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class RpcInvokerProcessor extends AbstractProcessor {

  private static final String INVOKER_PACKAGE = "org.example.common.net.proxy.invoker";
  private static final String INNER_SIMPLE_NAME = "Invoker";

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
        try {

          List<Element> elements = Util.getReqMethod(processingEnv, typeElement);

          TypeSpec inner = generateInner(typeElement, elements);

          TypeSpec outer = generateOuter(typeElement)
              .addType(inner).build();

          JavaFile javaFile = JavaFile.builder(INVOKER_PACKAGE, outer).build();

          String qualifiedName = "%s.%s".formatted(INVOKER_PACKAGE, outer.name);
          JavaFileObject file = processingEnv.getFiler().createSourceFile(qualifiedName);
          try (PrintWriter writer = new PrintWriter(file.openWriter())) {
            javaFile.writeTo(writer);
          }
        } catch (Exception e) {
          processingEnv.getMessager()
              .printError(
                  "[%S] %s build invoker error, %s".formatted(getClass(),
                      typeElement.getQualifiedName(),
                      e.toString()), typeElement);
        }
      }
    }

    return false;
  }

  public TypeSpec.Builder generateOuter(TypeElement clazz) {

    String simpleName = clazz.getSimpleName() + "Invoker";

    TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(simpleName)
        .addJavadoc("{@link $T}\n", clazz)
        .addJavadoc("@since $S", LocalDateTime.now())
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addField(FieldSpec
            .builder(LOGGER, "logger")
            .addModifiers(Modifier.FINAL, Modifier.STATIC)
            .initializer("$T.getLogger($L.class)", LOGGER_FACTOR, simpleName)
            .build())
        .addField(FieldSpec
            .builder(CONNECTION_GETTER, "manager")
            .addModifiers(Modifier.PRIVATE)
            .build())
        .addField(FieldSpec
            .builder(BASE_REMOTING, "remoting")
            .addModifiers(Modifier.PRIVATE)
            .build())
        .addField(FieldSpec
            .builder(COMMON_SERIALIZER, "serializer")
            .addModifiers(Modifier.PRIVATE).build());

    MethodSpec constructor = MethodSpec
        .constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(CONNECTION_GETTER, "manager")
        .addParameter(COMMON_SERIALIZER, "serializer")
        .addStatement("this.manager = manager")
        .addStatement("this.serializer = serializer")
        .addStatement("remoting = new $T()", BASE_REMOTING)
        .build();

    ClassName inner_type = ClassName.get(String.format("%s.%s", INVOKER_PACKAGE, simpleName),
        INNER_SIMPLE_NAME);

    MethodSpec ofId = MethodSpec.methodBuilder("of")
        .addModifiers(Modifier.PUBLIC)
        .returns(inner_type)
        .addParameter(TypeName.INT, "id")
        .addStatement("$T connection = manager.apply(id)", CONNECTION)
        .addStatement("return of(connection)").build();

    MethodSpec ofConnection = MethodSpec.methodBuilder("of")
        .addModifiers(Modifier.PUBLIC)
        .returns(inner_type)
        .addParameter(CONNECTION, "c")
        .addStatement("$T.requireNonNull(c)", Objects.class)
        .beginControlFlow("if (!c.isActive())")
        .addStatement("logger.error(\"[RPC] $L, 因为链接【{}】失效：无法处理\", c.id());", simpleName)
        .endControlFlow()
        .addStatement("return new $L($T.singletonList(c))", INNER_SIMPLE_NAME, Collections.class)
        .build();

    typeSpecBuilder.addMethod(constructor).addMethod(ofId).addMethod(ofConnection);

    return typeSpecBuilder;
  }

  public TypeSpec generateInner(TypeElement typeElement, List<Element> methods) {

    TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(INNER_SIMPLE_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    TypeName connectionsType = ParameterizedTypeName.get(ClassName.get(List.class),
        CONNECTION);
    typeBuilder.addField(FieldSpec
            .builder(connectionsType, "connections")
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .build())
        .addMethod(MethodSpec.constructorBuilder()
            .addParameter(connectionsType, "cs")
            .addStatement("connections = cs")
            .build());

    for (Element e : methods) {
      String methodName = e.getSimpleName().toString();
      ExecutableElement method = (ExecutableElement) e;
      MethodSpec.Builder methodBuilder = MethodSpec
          .methodBuilder(e.getSimpleName().toString())
          .addModifiers(Modifier.PUBLIC);

      //Handle ID
      int id = Util.calcProtoId(typeElement, method);
      methodBuilder
          .addJavadoc("{@link $T#$L}", typeElement, methodName)
          .addStatement("final int id = $L", id)
          .addStatement("$T message = $T.of(id)", MESSAGE, MESSAGE)
      ;

      List<? extends VariableElement> pparameters = method.getParameters();
      if (!pparameters.isEmpty()) {
        methodBuilder
            .addCode("\n")
            .addStatement("$T buf = $T.DEFAULT.buffer()", BYTE_BUF, POOLED_UTIL)
            .beginControlFlow("try")
        ;

        //Handle param
        for (VariableElement variableElement : method.getParameters()) {
          Name name = variableElement.getSimpleName();
          TypeMirror paramType = variableElement.asType();
          TypeName paramTypeName = TypeName.get(paramType);

          // Connection和Message不用生成
          if (paramTypeName.equals(CONNECTION) || paramTypeName.equals(MESSAGE)) {
            continue;
          }

          methodBuilder.addParameter(paramTypeName, name.toString());
          switch (paramType.getKind()) {
            case BOOLEAN -> methodBuilder.addStatement("buf.writeBoolean($L)", name);
            case BYTE -> methodBuilder.addStatement("buf.writeByte($L)", name);
            case SHORT -> methodBuilder.addStatement("buf.writeShort($L)", name);
            case CHAR -> methodBuilder.addStatement("buf.writeChar($L)", name);
            case FLOAT -> methodBuilder.addStatement("buf.writeFloat($L)", name);
            case DOUBLE -> methodBuilder.addStatement("buf.writeDouble($L)", name);
            case INT -> methodBuilder.addStatement("$T.writeInt32(buf, $L)", BYTEBUF_UTIL, name);
            case LONG -> methodBuilder.addStatement("$T.writeInt64(buf, $L)", BYTEBUF_UTIL, name);
            default -> methodBuilder.addStatement("serializer.writeObject(buf, $L)", name);
          }
        }

        methodBuilder
            .addStatement("message.packet($T.readBytes(buf))", BYTEBUF_UTIL)
            .endControlFlow()
            .beginControlFlow("finally")
            .addStatement("buf.release()")
            .endControlFlow()
            .addCode("\n")
        ;

      }

      methodBuilder
          .beginControlFlow("for ($T c : connections)", CONNECTION)
          .addStatement("remoting.invoke(c, message)")
          .endControlFlow();
      typeBuilder.addMethod(methodBuilder.build());
    }

    return typeBuilder.build();
  }



}
