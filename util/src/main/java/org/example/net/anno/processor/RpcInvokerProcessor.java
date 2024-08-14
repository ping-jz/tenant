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
import static org.example.net.anno.processor.Util.isCompleteAbleFuture;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;

/**
 * 负责RPC方法的调用类和代理类
 * <p>
 *
 * @author zhongjianping
 * @since 2024/8/9 11:19
 */
@SupportedAnnotationTypes("org.example.common.net.annotation.RpcModule")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class RpcInvokerProcessor extends AbstractProcessor {

  private static final String INVOKER_PACKAGE = "org.example.common.net.generated.invoker";

  private static final String INNER_SIMPLE_NAME = "Invoker";

  private static final String CONNECTION_FIELD_NAME = "c";
  private static final String MESSAGE_VAR_NAME = "m";
  private static final String BUF_VAR_NAME = "buf";

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
          List<Element> elements = Util.getReqMethod(processingEnv, typeElement);
          generateOuter(typeElement, elements);
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

  public void generateOuter(TypeElement typeElement, List<Element> elements)
      throws Exception {
    TypeSpec inner = generateInner(typeElement, elements);

    String simpleName = typeElement.getSimpleName() + "Invoker";

    TypeSpec.Builder outerBuilder = TypeSpec.classBuilder(simpleName)
        .addJavadoc("{@link $T}\n", typeElement)
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
        .addField(Util.COMMON_SERIALIZER_FIELD_SPEC);

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
        .addStatement("return new $L(c)", INNER_SIMPLE_NAME)
        .build();

    outerBuilder.addMethod(constructor).addMethod(ofId).addMethod(ofConnection);

    TypeSpec outer = outerBuilder.addType(inner).build();
    JavaFile javaFile = JavaFile.builder(INVOKER_PACKAGE, outer).build();

    String qualifiedName = "%s.%s".formatted(INVOKER_PACKAGE, outer.name);
    JavaFileObject file = processingEnv.getFiler().createSourceFile(qualifiedName);
    try (PrintWriter writer = new PrintWriter(file.openWriter())) {
      javaFile.writeTo(writer);
    }
  }

  public TypeSpec generateInner(TypeElement typeElement, List<Element> methods) {

    TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(INNER_SIMPLE_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    typeBuilder.addField(FieldSpec
            .builder(CONNECTION, CONNECTION_FIELD_NAME)
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .build())
        .addMethod(MethodSpec.constructorBuilder()
            .addParameter(CONNECTION, CONNECTION_FIELD_NAME)
            .addStatement("this.$L = $L", CONNECTION_FIELD_NAME, CONNECTION_FIELD_NAME)
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
          .addStatement("$T $L = $T.of(id)", MESSAGE, MESSAGE_VAR_NAME, MESSAGE)
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
            case BOOLEAN -> methodBuilder.addStatement("$L.writeBoolean($L)", BUF_VAR_NAME, name);
            case BYTE -> methodBuilder.addStatement("$L.writeByte($L)", BUF_VAR_NAME, name);
            case SHORT -> methodBuilder.addStatement("$L.writeShort($L)", BUF_VAR_NAME, name);
            case CHAR -> methodBuilder.addStatement("$L.writeChar($L)", BUF_VAR_NAME, name);
            case FLOAT -> methodBuilder.addStatement("$L.writeFloat($L)", BUF_VAR_NAME, name);
            case DOUBLE -> methodBuilder.addStatement("$L.writeDouble($L)", BUF_VAR_NAME, name);
            case INT ->
                methodBuilder.addStatement("$T.writeInt32($L, $L)", BYTEBUF_UTIL, BUF_VAR_NAME,
                    name);
            case LONG ->
                methodBuilder.addStatement("$T.writeInt64($L, $L)", BYTEBUF_UTIL, BUF_VAR_NAME,
                    name);
            default -> methodBuilder.addStatement("serializer.writeObject(buf, $L)", name);
          }
        }

        methodBuilder
            .addStatement("$L.packet($T.readBytes($L))", MESSAGE_VAR_NAME, BYTEBUF_UTIL,
                BUF_VAR_NAME)
            .endControlFlow()
            .beginControlFlow("finally")
            .addStatement("buf.release()")
            .endControlFlow()
            .addCode("\n")
        ;
      }

      TypeMirror typeMirror = method.getReturnType();
      if (isCompleteAbleFuture(typeMirror) != null) {
        //TODO 这里有个BUG，你要生成消息ID啊！！！！！！！
        methodBuilder
            .returns(TypeName.get(typeMirror))
            .addStatement("///TODO 先默认三秒吧，以后看需要改")
            .addStatement("$L.msgId($L.nextCallBackMsgId())", MESSAGE_VAR_NAME,
                CONNECTION_FIELD_NAME)
            .addStatement("return remoting.invoke($L, $L, 3, $T.SECONDS)",
                CONNECTION_FIELD_NAME,
                MESSAGE_VAR_NAME, TimeUnit.class);

      } else {
        methodBuilder.addStatement("remoting.invoke($L, $L)", CONNECTION_FIELD_NAME,
            MESSAGE_VAR_NAME);
      }

      typeBuilder.addMethod(methodBuilder.build());
    }

    return typeBuilder.build();
  }


}
