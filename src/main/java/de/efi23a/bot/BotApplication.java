package de.efi23a.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Dient als Einstiegspunkt für die Bot-Anwendung.
 */

@EnableScheduling
@SpringBootApplication
public class BotApplication {

  /**
   * Starten die Bot-Anwendung.
   * Die Start-Logik wird an das Spring-Framework übergeben.
   *
   * @param args Kommandozeilenargumente
   */
  public static void main(String[] args) {
    SpringApplication.run(BotApplication.class, args);
  }

}
