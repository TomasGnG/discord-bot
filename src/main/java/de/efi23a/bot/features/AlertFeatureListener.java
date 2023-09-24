package de.efi23a.bot.features;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;

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

    if(!command.equalsIgnoreCase("alert"))
      return;
    if(subcommand == null)
      return;

    if(subcommand.equalsIgnoreCase("add")) {
      var name = event.getOption("name").getAsString();
      var date = event.getOption("date").getAsString();
      var description = event.getOption("description").getAsString();

      if(alertFeature.exists(name)) {
        event.reply("Eine Erinnerung mit diesem Namen wurde bereits hinzugefügt. "
            + "Benutze ``/alert edit " + name + "`` !").setEphemeral(true).queue();
        return;
      }

      try {
        var sdf = new SimpleDateFormat("dd.MM.yyyy");

        var dateInstanz = sdf.parse(date);

        if(sdf.parse(sdf.format(new Date(System.currentTimeMillis()))).after(dateInstanz))
          throw new Exception();
      } catch (Exception e) {
        event.reply("Das eingetragene Datum(``" + date + "``) ist ungültig!").setEphemeral(true).queue();
        return;
      }

      alertFeature.addAlert(name, date, description, event.getMember().getEffectiveName());
      event.reply("Du hast eine neue Erinnerung hinzugefügt.").setEphemeral(true).queue();
    }
  }
}
