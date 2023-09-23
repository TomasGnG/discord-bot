package de.efi23a.bot;

import de.efi23a.bot.features.AlertFeature;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Dient als Einstiegspunkt für die Bot-Anwendung.
 */
@SpringBootApplication
public class BotApplication {

  /**
   * Starten die Bot-Anwendung.
   * Die Start-Logik wird an das Spring-Framework übergeben.
   *
   * @param args Kommandozeilenargumente
   */
  public static void main(String[] args) throws InterruptedException {
    SpringApplication.run(BotApplication.class, args);

    new AlertFeature(new BotConfiguration().jda());
  }

}
