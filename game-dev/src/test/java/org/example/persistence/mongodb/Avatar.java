package org.example.persistence.mongodb;

import static org.example.persistence.mongodb.AvatarId.avatarId;

import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Document(collection = "avatar")
class Avatar {

  @Id
  @Field(targetType = FieldType.INT64)
  /* 唯一ID */
  private AvatarId id;
  /* 名字 */
  private String name;

  public Avatar() {

  }

  public Avatar(long id) {
    this(avatarId(id));
  }

  public Avatar(AvatarId id) {
    this.id = id;
  }

  public AvatarId id() {
    return id;
  }

  public Avatar id(AvatarId id) {
    this.id = id;
    return this;
  }

  public String name() {
    return name;
  }

  public Avatar name(String name) {
    this.name = name;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Avatar avatar = (Avatar) o;
    return Objects.equals(id, avatar.id) && Objects.equals(name, avatar.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }

  @Override
  public String toString() {
    return "Avatar{" +
        "id=" + id +
        ", name='" + name + '\'' +
        '}';
  }
}
