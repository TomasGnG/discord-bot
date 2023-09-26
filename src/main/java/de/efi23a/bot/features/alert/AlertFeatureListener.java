package de.efi23a.bot.features.alert;

import jakarta.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

/**
 * Die Listener Klasse für das Alert Feature.
 */
@Component
@RequiredArgsConstructor
public class AlertFeatureListener extends ListenerAdapter {

  private final JDA jda;
  private final AlertFeature alertFeature;

  @PostConstruct
  void postConstruct() {
    jda.addEventListener(this);
  }

  @Override
  public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
    var command = event.getName();
    var subcommand = event.getSubcommandName();

    if (!command.equalsIgnoreCase("alert")) {
      return;
    }
    if (subcommand == null) {
      return;
    }

    if (subcommand.equalsIgnoreCase("info")) {
      var name = event.getOption("name").getAsString();

      if (!alertFeature.exists(name)) {
        event.reply("Eine Erinnerung mit diesem Namen gibt es nicht.").setEphemeral(true).queue();
        return;
      }

      event.replyEmbeds(alertFeature.getAlertEmbedMessage(name)).setEphemeral(true).queue();
    }
    if (subcommand.equalsIgnoreCase("list")) {
      var alerts = alertFeature.getAlerts();
      var builder = new StringBuilder();

      for (var alert : alerts) {
        builder.append("-> ").append(alert.get("name")).append(System.lineSeparator());
      }

      if(builder.isEmpty())
        builder.append("Es wurden keine Erinnerungen gefunden.");

      event.reply(builder.toString()).setEphemeral(true).queue();
      return;
    }
    if (subcommand.equalsIgnoreCase("add")) {
      var name = event.getOption("name").getAsString();
      var date = event.getOption("date").getAsString();
      final var description = event.getOption("description").getAsString();

      if (alertFeature.exists(name)) {
        event.reply("Eine Erinnerung mit diesem Namen wurde bereits hinzugefügt. "
            + "Benutze ``/alert edit " + name + "`` !").setEphemeral(true).queue();
        return;
      }

      try {
        var sdf = new SimpleDateFormat("dd.MM.yyyy");

        var dateInstanz = sdf.parse(date);

        if (sdf.parse(sdf.format(new Date(System.currentTimeMillis()))).after(dateInstanz)) {
          throw new Exception();
        }
      } catch (Exception e) {
        event.reply("Das eingetragene Datum(``" + date + "``) ist ungültig!").setEphemeral(true)
            .queue();
        return;
      }

      event.reply("Du hast eine neue Erinnerung hinzugefügt.").setEphemeral(true).queue();
      alertFeature.addAlert(name, date, description, event.getMember().getEffectiveName());
      return;
    }
    if (subcommand.equalsIgnoreCase("edit")) {
      var name = event.getOption("name").getAsString();
      var property = event.getOption("property").getAsString();
      var value = event.getOption("value").getAsString();

      if (!alertFeature.exists(name)) {
        event.reply("Eine Erinnerung mit diesem Namen gibt es nicht.").setEphemeral(true).queue();
        return;
      }

      if (property.equalsIgnoreCase("date")) {
        try {
          var sdf = new SimpleDateFormat("dd.MM.yyyy");

          var dateInstanz = sdf.parse(value);

          if (sdf.parse(sdf.format(new Date(System.currentTimeMillis()))).after(dateInstanz)) {
            throw new Exception();
          }
        } catch (Exception e) {
          event.reply("Das eingetragene Datum(``" + value + "``) ist ungültig!").setEphemeral(true)
              .queue();
          return;
        }
      }

      alertFeature.editAlert(name, property, value);
      event.reply("Die Erinnerung '" + name + "' wurde geändert.").setEphemeral(true).queue();
    }
    if (subcommand.equalsIgnoreCase("remove")) {
      var name = event.getOption("name").getAsString();

      if (!alertFeature.exists(name)) {
        event.reply("Eine Erinnerung mit diesem Namen gibt es nicht.").setEphemeral(true).queue();
        return;
      }

      alertFeature.removeAlert(name);
      event.reply("Die Erinnerung '" + name + "' wurde gelöscht.").setEphemeral(true).queue();
    }
  }
}
