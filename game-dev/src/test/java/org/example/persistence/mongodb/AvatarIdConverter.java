package org.example.persistence.mongodb;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
class AvatarIdConverter implements Converter<AvatarId, Long> {

  @Override
  public Long convert(AvatarId source) {
    return source.id();
  }
}
