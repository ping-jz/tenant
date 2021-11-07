package org.example.persistence;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.example.persistence.accessor.Accessor;
import org.example.persistence.mongo.EntityCache;
import org.example.util.Id;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据和缓存服务入口
 *
 * <p>
 * <p>主要想法，提供定时入库(入库逻辑)。为什么要定时入库，因为手动入库很麻烦，还增加写业务的负担。那如何决定入库条件 主要是靠猜。
 * 因为游戏有一个明显行为模式。频繁读取的数据，大概率会被改变。所以基于这个观察，我定义以下几个规则(检查周期，读取次数，过期时间)</p>
 * <p>1.定时检查，如60秒，每60秒检查一次。 然后取60秒的1/3=20次，定位入库条件。就说每隔一分钟会检查缓存，然后执行对应的操作</p>
 * <p>2.就是说每隔一分钟，检查所有缓存。只要读次数(read)大于等于20次就会自动入库一次，然后重置读次数，需要时间</p>
 * <p>3.还有过期时间expire，通过caffine来实现不需要定期检查。缓存经过expire没有访问后会自动删除然后进行一次保底入库</p>
 * </p>
 * <p>2.特殊需求的，自己手动自动入库</p>
 *
 * @author ZJP
 * @since 2021年11月07日 21:27:53
 **/
public class EntityService {

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  /** 数据访问抽象 */
  private Accessor<?, ?> accessor;

  /** 缓存服务 (类型 -> 缓存) */
  private Map<Class<?>, EntityCache<?, ?>> caches;

  /** 线程池 */
  private ExecutorService executors;

  public EntityService() {
    caches = new ConcurrentHashMap<>();
    executors = Executors.newSingleThreadExecutor();
  }


  public <PK extends Serializable & Comparable<PK>, T extends Id<PK>> void registeEntityClass(
      Class<T> clazz) {
    @SuppressWarnings("all")
    Accessor<PK, T> ac = (Accessor<PK, T>) accessor;
    EntityCache<PK, T> cache = new EntityCache<PK, T>(TimeUnit.SECONDS.toMillis(60), 1024, clazz,
        ac, executors);
    caches.put(clazz, cache);
  }


  public <PK extends Serializable & Comparable<PK>, T extends Id<PK>> EntityCache<PK, T> getEntityCache(
      Class<T> clazz) {
    Objects.requireNonNull(clazz);
    return (EntityCache<PK, T>) caches.get(clazz);
  }

  public void onClose() {
    for (Entry<Class<?>, EntityCache<?, ?>> entry : caches.entrySet()) {
      Class<?> clazz = entry.getKey();
      EntityCache<?, ?> cache = entry.getValue();

      try {
        cache.flushAll();
      } catch (Exception e) {
        logger.error("{} flush error", clazz);
      }
    }
  }

}
