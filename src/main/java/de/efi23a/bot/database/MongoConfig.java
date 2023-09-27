package de.efi23a.bot.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.util.Collection;
import java.util.Collections;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Configuration Class for MongoDB.
 */
@Configuration
@EnableMongoRepositories(basePackages = "de.efi23a.bot.database.repository")
public class MongoConfig extends AbstractMongoClientConfiguration {

  private static final String DATABASE_ENV_VARIABLE = "DATABASE";
  private static final String CONNECTION_STRING_ENV_VARIABLE = "CONNECTION_STRING";

  @Override
  protected String getDatabaseName() {
    return System.getenv(DATABASE_ENV_VARIABLE);
  }

  @Override
  public MongoClient mongoClient() {
    ConnectionString connectionString =
        new ConnectionString(System.getenv(CONNECTION_STRING_ENV_VARIABLE));
    MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
        .applyConnectionString(connectionString)
        .build();

    return MongoClients.create(mongoClientSettings);
  }

  @Override
  protected Collection getMappingBasePackages() {
    return Collections.singleton("de.efi23a.bot");
  }
}
