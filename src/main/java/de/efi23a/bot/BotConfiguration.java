package de.efi23a.bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BotConfiguration {

  private static final String TOKEN_ENV_VARIABLE = "BOT_TOKEN";

  @Bean
  JDA jda() throws InterruptedException {
    String token = System.getenv(TOKEN_ENV_VARIABLE);

    return JDABuilder.createDefault(token)
        .build()
        .awaitReady();
  }

}
