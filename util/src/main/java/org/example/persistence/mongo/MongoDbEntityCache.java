package org.example.persistence.mongo;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.example.persistence.Cache;
import org.example.util.Id;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * MongoDb缓存实现
 *
 * @author ZJP
 * @since 2021年09月29日 16:34:55
 **/
public class MongoDbEntityCache<PK extends Serializable & Comparable<PK>, T extends Id<PK>> implements
    Cache<PK, T> {

  /** spring mongo template */
  private MongoTemplate template;
  /** caffeine cache */
  private LoadingCache<PK, T> cache;
  /** entity类型信息 */
  private Class<T> entityClass;

  public MongoDbEntityCache(long expireMill, int cacheSize, MongoTemplate template,
      Executor executor) {
    Caffeine<PK, T> caffeine = Caffeine.newBuilder()
        .maximumSize(cacheSize)
        .expireAfterAccess(expireMill, TimeUnit.MILLISECONDS)
        .removalListener((key, value, cause) -> {
          if (value != null) {
            doWriteBack(value);
          }
        });
    if (executor != null) {
      caffeine.executor(executor);
    }

    this.cache = caffeine.build(key -> template.findById(key, entityClass));
  }

  @Override
  public T get(PK key) {
    return cache.get(key);
  }

  @Override
  public T getIfPresent(PK key) {
    return cache.getIfPresent(key);
  }

  @Override
  public T getOrCreate(PK key, Function<PK, T> provider) {
    return cache.asMap().computeIfAbsent(key, provider);
  }

  @Override
  public T put(T newObj) {
    Objects.requireNonNull(newObj);
    return cache.asMap().put(newObj.id(), newObj);
  }

  @Override
  public T putIfAbsent(T newObj) {
    Objects.requireNonNull(newObj);
    return cache.asMap().putIfAbsent(newObj.id(), newObj);
  }

  @Override
  public long size() {
    return cache.estimatedSize();
  }

  @Override
  public void flushAll() {
    for (T obj : cache.asMap().values()) {
      writeBack(obj);
    }
  }

  @Override
  public void flush(PK pk) {
    T obj = cache.getIfPresent(pk);
    if (obj != null) {
      writeBack(obj);
    }
  }

  @Override
  public void clean() {
    cache.invalidateAll();
  }

  @Override
  public void writeBack(T obj) {

  }

  @Override
  public void writeBack(Iterable<T> objs) {

  }

  private void doWriteBack(T obj) {
    try {
      template.save(obj);
    } catch (Exception e) {
      //持久化失败，放入缓存等待下次
      if (cache.get(obj.id()) == null) {
        cache.put(obj.id(), obj);
      }
    }
  }
}
