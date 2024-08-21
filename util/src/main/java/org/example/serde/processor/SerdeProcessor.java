package org.example.serde.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import io.netty.buffer.ByteBuf;
import java.io.PrintWriter;
import java.util.ArrayList;
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
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;
import org.apache.commons.lang3.StringUtils;
import org.example.serde.CommonSerializer;
import org.example.serde.NettyByteBufUtil;
import org.example.serde.Serializer;

@SupportedAnnotationTypes("org.example.serde.Serde")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class SerdeProcessor extends AbstractProcessor {

  private static final String BUF_VAR_NAME = "buf";
  private static final String SERIALIZER_VAR_NAME = "serializer";
  private static final String OBJECT_VAR_NAME = "object";

  public SerdeProcessor() {
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
              .printMessage(Kind.ERROR, "@Serde must be applied to a Class", clazz);
          return false;
        }

        try {
          String className = clazz.toString();
          String packageName = null;
          int lastDot = className.lastIndexOf('.');
          if (lastDot > 0) {
            packageName = className.substring(0, lastDot);
          }

          String simpleClassName = className.substring(lastDot + 1);
          TypeName typename = ClassName.get(packageName, simpleClassName);
          String builderClassName = className + "Serde";
          String builderSimpleClassName = builderClassName.substring(lastDot + 1);

          TypeName genericInterface = ParameterizedTypeName.get(ClassName.get(Serializer.class),
              typename);
          Builder builder = TypeSpec.classBuilder(builderSimpleClassName)
              .addSuperinterface(genericInterface)
              .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

          List<Element> fieldElements = getAllFieldElements((TypeElement) clazz);

          MethodSpec deSer = deSerializerCode(typename, fieldElements);
          MethodSpec serde = serializerCode(typename, fieldElements);
          TypeSpec typeSpec = consturctorAndFields(builder)
              .addMethod(deSer)
              .addMethod(serde)
              .build();

          JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(builderClassName);
          try (PrintWriter writer = new PrintWriter(builderFile.openWriter())) {
            JavaFile.builder(packageName, typeSpec).build().writeTo(writer);
          }

        } catch (Throwable e) {
          processingEnv.getMessager()
              .printError(
                  "[%s] %s build Serde error, %s".formatted(getClass(),
                      clazz,
                      Arrays.stream(
                              e.getStackTrace()).map(Objects::toString)
                          .collect(Collectors.joining("\n"))), clazz);
          throw new RuntimeException(e);
        }
      }
    }
    return false;
  }

  public List<Element> getAllFieldElements(TypeElement element) {
    List<TypeElement> clazzs = new ArrayList<>();
    clazzs.add(element);

    TypeElement parent = element;
    while (true) {
      parent = getSuperclass(parent);
      if (parent == null) {
        break;
      }

      clazzs.addFirst(parent);
    }

    List<Element> fields = new ArrayList<>();
    for (TypeElement typeElement : clazzs) {
      List<Element> fieldElements = typeElement.getEnclosedElements().stream()
          .filter(e -> e.getKind() == ElementKind.FIELD).filter(
              e -> !(e.getModifiers().contains(Modifier.FINAL) || e.getModifiers()
                  .contains(Modifier.STATIC) || e.getModifiers().contains(Modifier.TRANSIENT)))
          .collect(Collectors.toUnmodifiableList());

      fields.addAll(fieldElements);
    }

    return fields;
  }

  private static TypeSpec.Builder consturctorAndFields(TypeSpec.Builder builder) {

    MethodSpec constructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(CommonSerializer.class, SERIALIZER_VAR_NAME)
        .addStatement("this.$N = $N", SERIALIZER_VAR_NAME, SERIALIZER_VAR_NAME)
        .build();

    FieldSpec fieldSpec = FieldSpec
        .builder(CommonSerializer.class, SERIALIZER_VAR_NAME)
        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
        .build();

    return builder.addMethod(constructor).addField(fieldSpec);
  }

  private static MethodSpec deSerializerCode(TypeName typeName, List<Element> fieldElements) {
    MethodSpec.Builder builder = MethodSpec.methodBuilder("readObject")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(ByteBuf.class, BUF_VAR_NAME)
        .addStatement("$T $L = new $T()", typeName, OBJECT_VAR_NAME, typeName).returns(typeName);

    fieldElements.forEach(e -> {
      String fieldName = StringUtils.capitalize(e.getSimpleName().toString());
      switch (e.asType().getKind()) {
        case BOOLEAN -> builder.addStatement("$L.set$L($L.readBoolean())",
            OBJECT_VAR_NAME,
            fieldName,
            BUF_VAR_NAME);
        case BYTE -> builder.addStatement("$L.set$L($L.readByte())",
            OBJECT_VAR_NAME,
            fieldName,
            BUF_VAR_NAME);
        case SHORT -> builder.addStatement("$L.set$L($L.readShort())",
            OBJECT_VAR_NAME,
            fieldName,
            BUF_VAR_NAME);
        case CHAR -> builder.addStatement("$L.set$L($L.readChar())",
            OBJECT_VAR_NAME,
            fieldName,
            BUF_VAR_NAME);
        case FLOAT -> builder.addStatement("$L.set$L($L.readFloat())",
            OBJECT_VAR_NAME,
            fieldName,
            BUF_VAR_NAME);
        case DOUBLE -> builder.addStatement("$L.set$L($L.readDouble())",
            OBJECT_VAR_NAME,
            fieldName,
            BUF_VAR_NAME);
        case INT -> builder.addStatement("$L.set$L($T.readInt32($L))",
            OBJECT_VAR_NAME,
            fieldName,
            NettyByteBufUtil.class,
            BUF_VAR_NAME);
        case LONG -> builder.addStatement("$L.set$L($T.readInt64($L))",
            OBJECT_VAR_NAME,
            fieldName,
            NettyByteBufUtil.class,
            BUF_VAR_NAME);
        default -> builder.addStatement("$L.set$L($L.read(buf))",
            OBJECT_VAR_NAME,
            fieldName,
            SERIALIZER_VAR_NAME);
      }
    });

    builder.addStatement("return object");
    return builder.build();
  }

  private static MethodSpec serializerCode(TypeName typeName, List<Element> fieldElements) {
    MethodSpec.Builder builder = MethodSpec.methodBuilder("writeObject")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(ByteBuf.class, BUF_VAR_NAME).addParameter(typeName, OBJECT_VAR_NAME)
        .returns(TypeName.VOID);

    fieldElements.forEach(e -> {
      String fieldName = StringUtils.capitalize(e.getSimpleName().toString());
      switch (e.asType().getKind()) {
        case BOOLEAN -> builder.addStatement("$L.writeBoolean($L.is$L())",
            BUF_VAR_NAME,
            OBJECT_VAR_NAME,
            fieldName);
        case BYTE -> builder.addStatement("$L.writeByte($L.get$L())",
            BUF_VAR_NAME,
            OBJECT_VAR_NAME,
            fieldName);
        case SHORT -> builder.addStatement("$L.writeShort($L.get$L())",
            BUF_VAR_NAME,
            OBJECT_VAR_NAME,
            fieldName);
        case CHAR -> builder.addStatement("$L.writeChar($L.get$L())",
            BUF_VAR_NAME,
            OBJECT_VAR_NAME,
            fieldName);
        case FLOAT -> builder.addStatement("$L.writeFloat($L.get$L())",
            BUF_VAR_NAME,
            OBJECT_VAR_NAME,
            fieldName);
        case DOUBLE -> builder.addStatement("$L.writeDouble($L.get$L())",
            BUF_VAR_NAME,
            OBJECT_VAR_NAME,
            fieldName);
        case INT -> builder.addStatement("$T.writeInt32($L, $L.get$L())",
            NettyByteBufUtil.class,
            BUF_VAR_NAME,
            OBJECT_VAR_NAME,
            fieldName);
        case LONG -> builder.addStatement("$T.writeInt64($L, $L.get$L())",
            NettyByteBufUtil.class,
            BUF_VAR_NAME,
            OBJECT_VAR_NAME,
            fieldName);
        default -> builder.addStatement("$L.writeObject($L, $L.get$L())",
            SERIALIZER_VAR_NAME,
            BUF_VAR_NAME,
            OBJECT_VAR_NAME,
            fieldName);
      }
    });

    return builder.build();
  }

  private TypeElement getSuperclass(TypeElement type) {
    if (type.getSuperclass().getKind() == TypeKind.DECLARED) {
      TypeElement superclass = (TypeElement) processingEnv.getTypeUtils()
          .asElement(type.getSuperclass());
      String name = superclass.getQualifiedName().toString();
      if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("android.")) {
        // Skip system classes, this just degrades performance
        return null;
      } else {
        return superclass;
      }
    } else {
      return null;
    }
  }


}