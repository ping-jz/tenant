package org.example.persistence.mongodb;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
class Long2AvatarIdConverter implements Converter<Long, AvatarId> {

  @Override
  public AvatarId convert(Long source) {
    return AvatarId.avatarId(source);
  }
}
