package org.example.persistence.mongodb;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

class MongoClinetConfiguration extends AbstractMongoClientConfiguration {

  @Override
  protected String getDatabaseName() {
    return "test";
  }

  @Override
  protected void configureClientSettings(MongoClientSettings.Builder builder) {
    builder.credential(MongoCredential.createCredential("game", "admin", "123456".toCharArray()));
  }

  public MongoTemplate mongoTemplate(/*MongoDatabaseFactory databaseFactory, MappingMongoConverter converter*/)
      throws ClassNotFoundException {
    MongoDatabaseFactory databaseFactory = mongoDbFactory();
    MongoCustomConversions customConversions = customConversions();
    MappingMongoConverter mappingMongoConverter = mappingMongoConverter(databaseFactory,
        customConversions, mongoMappingContext(customConversions, mongoManagedTypes()));
    if (mappingMongoConverter.getConversionService() instanceof ConverterRegistry registry) {
      registry.addConverter(new AvatarIdConverter());
      registry.addConverter(new Long2AvatarIdConverter());
    }
    return new MongoTemplate(databaseFactory, mappingMongoConverter);
  }
}
