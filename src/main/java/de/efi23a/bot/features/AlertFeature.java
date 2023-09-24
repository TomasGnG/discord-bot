package de.efi23a.bot.features;

import jakarta.annotation.PostConstruct;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.springframework.stereotype.Component;

/**
 * Alert Feature von Tomas Keder.
 */
@Component
@RequiredArgsConstructor
public class AlertFeature {

  private final JDA jda;

  @PostConstruct
  void postConstruct() {
    registerAlertCommand();
  }

  private void registerAlertCommand() {
    var cmd = Commands.slash("alert", "Verwalte die Erinnerungen.")
        .addSubcommands(
            new SubcommandData("list", "Listet alle Erinnerungen auf."),
            new SubcommandData("add", "Erstelle eine neue Erinnerung.")
                .addOption(OptionType.STRING, "name", "Benenne die Erinnerung.", true)
                .addOption(OptionType.STRING, "date", "Datum für die Erinnerung. Bsp: "
                        + new SimpleDateFormat("dd.MM.yyyy")
                              .format(new Date(System.currentTimeMillis())),
                    true)
                .addOption(OptionType.STRING, "description", "Beschreibung für die Erinnerung.",
                    true),
            new SubcommandData("remove", "Entferne eine Erinnerung.")
                .addOption(OptionType.STRING, "name", "Name der Erinnerung", true),
            new SubcommandData("edit", "Ändere eine Erinnerung")
                .addOptions(
                    new OptionData(OptionType.STRING, "property", "Eigenschaft die geändert werden soll.")
                        .addChoice("Name", "name")
                        .addChoice("Datum", "date")
                        .addChoice("Beschreibung", "description")
                        .setRequired(true)
                )
                .addOption(OptionType.STRING, "value", "Der neue Wert.", true)
        );

    jda.updateCommands().addCommands(cmd).queue();
  }

  private void startAlertCheckerTask() {
    new Timer().schedule(new TimerTask() {
      @Override
      public void run() {

      }
    }, 1000L, 1000L);
  }

  public boolean exists(String name) {
    return false;
  }

  public void addAlert(String name, String date, String description, String createdBy) {
    sendAlert(name, date, description, createdBy);
  }



  private void sendAlert(String name, String date, String description, String createdBy) {
    var embed = new EmbedBuilder()
        .setColor(Color.ORANGE)
        .setTitle("Erinnerung")
        .addField("Name", name, false)
        .addField("Datum", date, false)
        .addField("Beschreibung", description, false)
        .setFooter("Hinzugefügt von " + createdBy)
        .build();

    jda.getTextChannelById(1155210664084787300L)
        .sendMessage("everyone..").setEmbeds(embed).queue();
  }
}
