package org.example.persistence.mongo;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.example.persistence.Cache;
import org.example.persistence.ValueWrapper;
import org.example.persistence.accessor.Accessor;
import org.example.util.Id;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * MongoDb缓存实现
 *
 * 主要负责加载和回写实现
 *
 * <p>1.主动清除的缓存需要先手动写回数据</p>
 * <p>2.数据经过长时间未使用，则会自动写会并清除</p>
 * <p>3.暂时不需要把数据访问抽象出来</p>
 *
 * @author ZJP
 * @since 2021年09月29日 16:34:55
 **/
public class EntityCache<PK extends Serializable & Comparable<PK>, T extends Id<PK>> implements
    Cache<PK, T> {

  private static final String ID = "_id";

  /** spring mongo template */
  private Accessor<PK, T> accessor;
  /** caffeine cache */
  private LoadingCache<PK, ValueWrapper<T>> cache;
  /** entity类型信息 */
  private Class<T> entityClass;

  public EntityCache(long expireMill, int cacheSize, Accessor<PK, T> accessor,
      Executor executor) {
    Caffeine<PK, ValueWrapper<T>> caffeine = Caffeine.newBuilder()
        .maximumSize(cacheSize)
        .expireAfterAccess(expireMill, TimeUnit.MILLISECONDS)
        .removalListener((key, value, cause) -> {
          if (value != null && cause != RemovalCause.EXPLICIT) {
            doWriteBack(key, value.getValue());
          }
        });
    if (executor != null) {
      caffeine.executor(executor);
    }

    this.accessor = accessor;
    this.cache = caffeine.build(key -> ValueWrapper.of(accessor.load(entityClass, key)));
  }

  public EntityCache(long expireMill, int cacheSize, MongoTemplate accessor,
      RemovalListener<PK, T> removalListener, CacheLoader<PK, T> loader, Executor executor) {
    Caffeine<PK, ValueWrapper<T>> caffeine = Caffeine.newBuilder()
        .maximumSize(cacheSize)
        .expireAfterAccess(expireMill, TimeUnit.MILLISECONDS)
        .removalListener((key, value, cause) -> {
          if (value != null) {
            removalListener.onRemoval(key, value.getValue(), cause);
          }
        });
    if (executor != null) {
      caffeine.executor(executor);
    }

    this.cache = caffeine.build(key -> ValueWrapper.of(loader.load(key)));
  }

  @Override
  public T get(PK key) {
    ValueWrapper<T> v = cache.get(key);
    T res = null;
    if (v != null) {
      v.incReads();
      res = v.getValue();
    }
    return res;
  }

  @Override
  public T getIfPresent(PK key) {
    ValueWrapper<T> v = cache.getIfPresent(key);
    return v != null ? v.getValue() : null;
  }

  @Override
  public T getOrCreate(PK key, Function<PK, T> provider) {
    return cache.asMap().computeIfAbsent(key, pk ->
        ValueWrapper.of(provider.apply(pk))
    ).getValue();
  }

  @Override
  public T put(T newObj) {
    Objects.requireNonNull(newObj);
    ValueWrapper<T> newWrapper = ValueWrapper.of(newObj);
    cache.asMap().put(newObj.id(), newWrapper);
    return newWrapper.getValue();
  }

  @Override
  public T putIfAbsent(T newObj) {
    Objects.requireNonNull(newObj);
    return cache.asMap().computeIfAbsent(newObj.id(), pk -> ValueWrapper.of(newObj)).getValue();
  }

  @Override
  public long size() {
    return cache.estimatedSize();
  }

  @Override
  public void flushAll() {
    for (ValueWrapper<T> obj : cache.asMap().values()) {
      obj.incWrites();
      writeBack(obj.getValue());
    }
  }

  @Override
  public void flush(PK pk) {
    ValueWrapper<T> obj = cache.getIfPresent(pk);
    if (obj != null) {
      obj.incWrites();
      writeBack(obj.getValue());
    }
  }

  @Override
  public T delete(PK pk) {
    ValueWrapper<T> remove = cache.getIfPresent(pk);
    if (remove != null) {
      accessor.delete(entityClass, pk);
    }
    return remove != null ? remove.getValue() : null;
  }

  @Override
  public void clean() {
    cache.invalidateAll();
  }

  @Override
  public void writeBack(T obj) {
    doWriteBack(obj.id(), obj);
  }

  private void doWriteBack(PK key, T obj) {
    if (obj == null) {
      return;
    }

    try {
      accessor.save(obj);
    } catch (Exception e) {
      //持久化失败，放入缓存等待下次
      cache.asMap().computeIfAbsent(obj.id(), k -> ValueWrapper.of(obj));
    }
  }

  public Accessor<PK, T> template() {
    return accessor;
  }
}
