package org.example.persistence;

import java.util.function.Function;

/**
 * //TODO 游戏所需ORM层比较简单，尽量保持代码简单以适配各种修改 //只抽象最简单的功能，并同时暴露你封装的第三方库
 *
 *
 * 缓存服务
 *
 * @author ZJP
 * @since 2021年09月29日 17:36:26
 **/
public interface Cache<PK, T> {

  /**
   * 获取
   *
   * @param key 主键
   * @since 2021年09月29日 17:39:50
   */
  T get(PK key);

  /**
   * 尝试获取，不存在则不做任何特殊处理
   *
   * @param key 主键
   * @since 2021年09月29日 17:40:16
   */
  T getIfPresent(PK key);

  /**
   * 尝试获取，不存在则通过provider进行初始化
   *
   * @param key 主键
   * @param provider 初始化构建
   * @since 2021年09月29日 17:40:16
   */
  T getOrCreate(PK key, Function<PK, T> provider);

  /**
   * 添加进缓存
   *
   * @param newObj 新对象
   * @return T 之前关系的对象
   * @since 2021年09月29日 17:44:45
   */
  T put(T newObj);

  /**
   * 如果对应的ID不存在，则添加进缓存
   *
   * @param newObj 新对象
   * @return T 之前关联的对象
   * @since 2021年09月29日 17:44:59
   */
  T putIfAbsent(T newObj);

  /**
   * 返回缓存大小
   *
   * @return 大小
   * @since 2021年09月29日 17:46:04
   */
  long size();

  /**
   * 尝试持久化缓存内容，是否支持看具体实现
   *
   * @since 2021年09月29日 17:47:49
   */
  void flushAll();

  /**
   * 尝试持久化缓存内容，是否支持看具体实现
   *
   * @since 2021年09月29日 17:47:49
   */
  void flush(PK pk);


  /**
   * 删除缓存，如果支持同时从数据删除
   *
   * @param pk 主键
   * @since 2021年11月05日 14:49:57
   */
  T delete(PK pk);

  /**
   * 清空化缓存内容,不会主动持久化内容(如果需要请主动持久化)
   *
   * @since 2021年09月29日 17:47:49
   */
  void clean();

  /**
   * 持久化具体内容
   *
   * @since 2021年09月29日 17:47:49
   */
  void writeBack(T obj);

}
