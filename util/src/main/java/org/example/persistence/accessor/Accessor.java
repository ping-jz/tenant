package org.example.persistence.accessor;

/**
 * 将数据共同的操作进行封装
 *
 * @author ZJP
 * @since 2021年11月05日 15:25:59
 **/
public interface Accessor {

  /**
   * 根据主键加载数据
   *
   * @since 2021年11月05日 15:34:08
   **/
  <PK, T> T load(Class<T> entityClass, PK key);


  /**
   * 根据主键删除数据
   *
   * @since 2021年11月05日 15:34:08
   **/
  <PK, T> void delete(Class<T> entityClass, PK key);


  /**
   * 保存数据
   *
   * @param entity 实体类
   * @since 2021年11月05日 16:41:01
   */
  <T> T save(T entity);

}
