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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
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
        }
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

        try {
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

        } catch (IOException e) {
          processingEnv.getMessager()
              .printMessage(Kind.ERROR, "@Serde build error, %s".formatted(e.toString()), clazz);
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
    final String serializer = "serializer";
    MethodSpec constructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(CommonSerializer.class, serializer)
        .addStatement("this.$N = $N", serializer, serializer)
        .build();

    FieldSpec fieldSpec = FieldSpec
        .builder(CommonSerializer.class, serializer)
        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
        .build();

    return builder.addMethod(constructor).addField(fieldSpec);
  }

  private static MethodSpec deSerializerCode(TypeName typeName, List<Element> fieldElements) {
    MethodSpec.Builder builder = MethodSpec.methodBuilder("readObject")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(ByteBuf.class, "buf")
        .addStatement("$T object = new $T()", typeName, typeName).returns(typeName);

    fieldElements.forEach(e -> {
      String fieldName = StringUtils.capitalize(e.getSimpleName().toString());
      switch (e.asType().getKind()) {
        case BOOLEAN: {
          builder.addStatement("object.set$L($L)", fieldName, "buf.readBoolean()");
          break;
        }
        case BYTE: {
          builder.addStatement("object.set$L($L)", fieldName, "buf.readByte()");
          break;
        }
        case SHORT: {
          builder.addStatement("object.set$L($L)", fieldName, "buf.readShort()");
          break;
        }
        case INT: {
          builder.addStatement("object.set$L($T.$L)", fieldName, NettyByteBufUtil.class,
              "readInt32(buf)");
          break;
        }
        case LONG: {
          builder.addStatement("object.set$L($T.$L)", fieldName, NettyByteBufUtil.class,
              "readInt64(buf)");
          break;
        }
        case CHAR: {
          builder.addStatement("object.set$L($L)", fieldName, "buf.readChar()");
          break;
        }
        case FLOAT: {
          builder.addStatement("object.set$L($L)", fieldName, "buf.readFloat()");
          break;
        }
        case DOUBLE: {
          builder.addStatement("object.set$L($L)", fieldName, "buf.readDouble()");
          break;
        }
        default: {
          builder.addStatement("object.set$L($L)", fieldName, "serializer.read(buf)");
          break;
        }
      }
    });

    builder.addStatement("return object");
    return builder.build();
  }

  private static MethodSpec serializerCode(TypeName typeName, List<Element> fieldElements) {
    MethodSpec.Builder builder = MethodSpec.methodBuilder("writeObject")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(ByteBuf.class, "buf").addParameter(typeName, "object")
        .returns(TypeName.VOID);

    fieldElements.forEach(e -> {
      String fieldName = StringUtils.capitalize(e.getSimpleName().toString());
      switch (e.asType().getKind()) {
        case BOOLEAN: {
          builder.addStatement("buf.writeBoolean(object.is$L())", fieldName);
          break;
        }
        case BYTE: {
          builder.addStatement("buf.writeByte(object.get$L())", fieldName);
          break;
        }

        case SHORT: {
          builder.addStatement("buf.writeShort(object.get$L())", fieldName);
          break;
        }

        case INT: {
          builder.addStatement("$T.writeInt32(buf, object.get$L())", NettyByteBufUtil.class,
              fieldName);
          break;
        }
        case LONG: {
          builder.addStatement("$T.writeInt64(buf, object.get$L())", NettyByteBufUtil.class,
              fieldName);
          break;
        }
        case CHAR: {
          builder.addStatement("buf.writeChar(object.get$L())", fieldName);
          break;
        }
        case FLOAT: {
          builder.addStatement("buf.writeFloat(object.get$L())", fieldName);
          break;
        }
        case DOUBLE: {
          builder.addStatement("buf.writeDouble(object.get$L())", fieldName);
          break;
        }
        default: {
          builder.addStatement("serializer.writeObject(buf, object.get$L())", fieldName);
          break;
        }
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