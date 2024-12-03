package org.example.common.persistence;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.example.common.persistence.accessor.Accessor;

/**
 * MongoDb缓存实现
 * <p>
 * 主要负责加载和回写实现
 *
 * <p>1.主动清除的缓存需要先手动写回数据</p>
 * <p>2.数据经过长时间未使用，则会自动写会并清除</p>
 * <p>3.暂时不需要把数据访问抽象出来</p>
 *
 * <p>游戏所需ORM层比较简单，尽量保持代码简单以适配各种修改, 只抽象最简单的功能，并同时暴露你封装的第三方库</p>
 * <p>
 * 不要想做通用的，只要保证能 1.正常入库 2.热数据要适量缓存
 * </p>
 *
 * @author ZJP
 * @since 2021年09月29日 16:34:55
 **/
public class EntityCache<K, T> {

  /**
   * spring mongo template
   */
  private Accessor accessor;
  /**
   * caffeine cache
   */
  private LoadingCache<K, ValueWrapper<T>> cache;
  /**
   * entity类型信息
   */
  private Class<T> entityClass;

  public EntityCache(long expireMill, Class<T> entityClass, Accessor accessor, Executor executor) {
    this(expireMill, Integer.MAX_VALUE, entityClass, accessor, executor);
  }

  public EntityCache(long expireMill, int cacheSize, Class<T> entityClass, Accessor accessor,
      Executor executor) {
    Caffeine<K, ValueWrapper<T>> caffeine = Caffeine.newBuilder().maximumSize(cacheSize)
        .expireAfterAccess(expireMill, TimeUnit.MILLISECONDS)
        .evictionListener((key, value, cause) -> {
          if (value != null && cause != RemovalCause.EXPLICIT) {
            doWriteBack(key, value.getValue());
          }
        });
    if (executor != null) {
      caffeine.executor(executor);
    }

    this.entityClass = entityClass;
    this.accessor = accessor;
    cache = caffeine.build(key -> {
      ValueWrapper<T> res = null;
      T t = accessor.load(entityClass, key);
      if (t != null) {
        res = ValueWrapper.of(t);
      }
      return res;
    });
  }

  public T get(K key) {
    ValueWrapper<T> v = cache.get(key);
    T res = null;
    if (v != null) {
      v.incReads();
      res = v.getValue();
    }
    return res;
  }


  public T getIfPresent(K key) {
    ValueWrapper<T> v = cache.getIfPresent(key);
    return v != null ? v.getValue() : null;
  }


  public T getOrCreate(K key, Function<K, T> provider) {
    return cache.asMap().computeIfAbsent(key, pk -> {
      T t = provider.apply(pk);
      ValueWrapper<T> tWrapper = null;
      if (t != null) {
        tWrapper = ValueWrapper.of(t);
        writeBack(key, tWrapper.getValue());
      }
      return tWrapper;
    }).getValue();
  }


  public T put(K pk, T newObj) {
    Objects.requireNonNull(newObj);
    ValueWrapper<T> newWrapper = ValueWrapper.of(newObj);
    cache.asMap().put(pk, newWrapper);
    return newWrapper.getValue();
  }


  public T putIfAbsent(K key, T newObj) {
    Objects.requireNonNull(newObj);
    return cache.asMap().computeIfAbsent(key, pk -> ValueWrapper.of(newObj)).getValue();
  }


  public long size() {
    return cache.estimatedSize();
  }


  public void flushAll() {
    for (Entry<K, ValueWrapper<T>> entry : cache.asMap().entrySet()) {
      ValueWrapper<T> obj = entry.getValue();
      obj.incWrites();
      writeBack(entry.getKey(), obj.getValue());
    }
  }


  public void flush(K pk) {
    ValueWrapper<T> obj = cache.getIfPresent(pk);
    if (obj != null) {
      obj.incWrites();
      writeBack(pk, obj.getValue());
    }
  }


  public T delete(K pk) {
    ValueWrapper<T> remove = cache.asMap().remove(pk);
    T res = null;
    if (remove != null) {
      res = remove.getValue();
      accessor.delete(entityClass, pk);
    }
    return res;
  }


  public void clean() {
    cache.invalidateAll();
  }


  public void writeBack(K key, T obj) {
    doWriteBack(key, obj);
  }

  private void doWriteBack(K key, T obj) {
    if (obj == null) {
      return;
    }
    accessor.save(obj);
  }
}
