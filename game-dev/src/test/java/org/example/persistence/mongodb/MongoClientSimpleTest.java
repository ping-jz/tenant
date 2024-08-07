package org.example.persistence.mongodb;

import static com.mongodb.client.model.Filters.eq;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "mongoDbTest", matches = "true")
public class MongoClientSimpleTest {

  private static MongoClient client;

  @BeforeAll
  static void before() {
    client = MongoClients.create(
        "mongodb://game:123456@localhost:27017/sample_mflix?authSource=admin&connecttimeoutms=30000&sockettimeoutms=300000&appName=appName&maxPoolSize=20&minPoolSize=1");
  }

  @AfterAll
  static void after() {
    if (client != null) {
      client.close();
    }
  }

  @Test
  public void read() {
    MongoDatabase database = client.getDatabase("sample_mflix");
    MongoCollection<Document> collection = database.getCollection("movies");
    collection.insertOne(new Document("title", "Back to the Future"));
    Document doc = collection.find(Filters.eq("title", "Back to the Future")).first();

    Assertions.assertEquals("Back to the Future", doc.getString("title"));
    Assertions.assertEquals(1L, collection.countDocuments());
    collection.drop();
  }

}
