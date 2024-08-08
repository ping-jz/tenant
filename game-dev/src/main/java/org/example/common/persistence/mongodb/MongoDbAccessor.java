package org.example.common.persistence.mongodb;

import org.example.common.persistence.accessor.Accessor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * 对MongoTemplate进行简单的封装
 *
 * @author ZJP
 * @since 2021年11月05日 15:23:56
 **/
public class MongoDbAccessor implements Accessor {

  public static final String ID = "_id";

  /**
   * spring的封装的mongodb操作
   */
  private MongoTemplate template;

  public MongoDbAccessor(MongoTemplate template) {
    this.template = template;
  }

  @Override
  public <PK, T> T load(Class<T> entityClass, PK key) {
    return template.findById(key, entityClass);
  }

  @Override
  public <PK, T> void delete(Class<T> entityClass, PK key) {
    template.remove(Query.query(Criteria.where(ID).is(key)), entityClass);
  }

  @Override
  public <T> T save(T entity) {
    return template.save(entity);
  }
}
