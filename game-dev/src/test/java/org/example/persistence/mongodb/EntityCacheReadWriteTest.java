package org.example.persistence.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import org.example.common.persistence.EntityCache;
import org.example.common.persistence.accessor.Accessor;
import org.example.common.persistence.mongodb.MongoDbAccessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.example.persistence.mongodb.AvatarId.avatarId;

/**
 * 手动配置环境，用Ideal测试吧
 */
@EnabledIfSystemProperty(named = "mongoDbTest", matches = "true")
public class EntityCacheReadWriteTest {

  /* The mongo client */
  private static long expireTime = 1;
  private static MongoClient client;
  private static EntityCache<AvatarId, Avatar> avatarCache;

  @BeforeAll
  static void beforeAll() throws Exception {

    client = MongoClients.create(
        "mongodb://game:123456@localhost:27017/test?authSource=admin&connecttimeoutms=30000&sockettimeoutms=300000&appName=appName&maxPoolSize=20&minPoolSize=1");
//
    MongoClinetConfiguration configuration = new MongoClinetConfiguration();
    MongoTemplate template = configuration.mongoTemplate();

    Accessor accessor = new MongoDbAccessor(template);
    avatarCache = new EntityCache<>(expireTime, Avatar.class, accessor, ForkJoinPool.commonPool());
  }

  @AfterAll
  static void afterAll() {
    if (client != null) {
      // 清空测试数据
      MongoDatabase database = client.getDatabase("test");
      database.getCollection("avatar").drop();
      client.close();
    }
  }

  @Test
  void readWriteTest() throws Exception {
    AvatarId avatarId = avatarId(2);
    String name = "Test";
    avatarCache.getOrCreate(avatarId, id -> {
      Avatar a = new Avatar(id);
      a.name(name);
      return a;
    });

    TimeUnit.MILLISECONDS.sleep(expireTime * 10);
    Assertions.assertNull(avatarCache.getIfPresent(avatarId));

    Avatar expected = new Avatar(avatarId);
    expected.name(name);
    Assertions.assertEquals(expected, avatarCache.get(avatarId));
  }

  @Test
  void multiReadWriteTest() throws Exception {
    String namePrefix = "Test";
    for (long i = 10; i < 20; i++) {
      avatarCache.getOrCreate(avatarId(i), id -> {
        Avatar a = new Avatar(id);
        a.name(namePrefix + id);
        return a;
      });
    }

    TimeUnit.MILLISECONDS.sleep(expireTime * 10);
    for (long i = 10; i < 20; i++) {
      Assertions.assertNull(avatarCache.getIfPresent(avatarId(i)));
    }

    for (long i = 10; i < 20; i++) {
      AvatarId id = avatarId(i);
      Avatar expected = new Avatar(id);
      expected.name(namePrefix + id);
      Assertions.assertEquals(expected, avatarCache.get(avatarId(i)));
    }
  }

}
