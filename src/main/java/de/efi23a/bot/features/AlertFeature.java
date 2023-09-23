package de.efi23a.bot.features;

import jakarta.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
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
                .addOption(OptionType.STRING, "name", "Name der Erinnerung", true)
        );

    jda.updateCommands().addCommands(cmd).queue();
  }

}
