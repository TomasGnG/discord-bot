package de.efi23a.bot.features.alert;

import static com.mongodb.client.model.Filters.eq;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.efi23a.bot.database.MongoConfig;
import jakarta.annotation.PostConstruct;
import java.awt.Color;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.bson.Document;
import org.springframework.stereotype.Component;

/**
 * Alert Feature von Tomas Keder.
 */
@Component
@RequiredArgsConstructor
public class AlertFeature {

  private final JDA jda;
  private final MongoConfig mongoConfig;
  private MongoClient client;
  private MongoDatabase db;
  private MongoCollection<Document> alerts;

  @PostConstruct
  void postConstruct() {
    client = mongoConfig.mongoClient();
    db = client.getDatabase("TestCluster");
    alerts = db.getCollection("alerts");

    registerAlertCommand();
    startAlertCheckerTask();
  }

  private void registerAlertCommand() {
    var cmd = Commands.slash("alert", "Verwalte die Erinnerungen.")
        .addSubcommands(
            new SubcommandData("info", "Zeigt alle Details zu einer Erinnerung.")
                .addOption(OptionType.STRING, "name", "Name der Erinnerung", true),
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
                .addOption(OptionType.STRING, "name", "Erinnerung die bearbeitet werden soll.",
                    true)
                .addOptions(
                    new OptionData(OptionType.STRING, "property",
                        "Eigenschaft die geändert werden soll.")
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
        var sdf = new SimpleDateFormat("dd.MM.yyyy");
        var alerts = getAlerts();

        for (var alert : alerts) {
          try {
            var duration = Duration.between(new Date(System.currentTimeMillis()).toInstant(),
                sdf.parse(alert.getString("date")).toInstant());

            if (!alert.getBoolean("announcement1day")
                && (duration.get(ChronoUnit.SECONDS) / 60 / 60) < 24) {
              alert.replace("announcement1day", true);
              updateAlert(alert);
              sendAlert(alert);
            } else if (!alert.getBoolean("announcement3day")
                && !alert.getBoolean("announcement1day")
                && (duration.get(ChronoUnit.SECONDS) / 60 / 60) < 72) {
              alert.replace("announcement3day", true);
              updateAlert(alert);
              sendAlert(alert);
            }
          } catch (ParseException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }, 5 * 1000L, 5 * 60 * 1000L);
  }

  /**
   * Überprüft, ob eine Erinnerung bereits existiert.
   *
   * @param name Name der Erinnerung
   * @return true, falls eine Erinnerung mit dem Namen existiert. false wenn nicht.
   */
  public boolean exists(String name) {
    return alerts.find(eq("name", name)).first() != null;
  }

  /**
   * Fügt eine neue Erinnerung hinzu.
   *
   * @param name Name der Erinnerung.
   * @param date Datum für die Erinnerung.
   * @param description Beschreibung für die Erinnerung.
   * @param createdBy Ersteller von der Erinnerung.
   */
  public void addAlert(String name, String date, String description, String createdBy) {
    var document = new Document();

    document.put("name", name);
    document.put("date", date);
    document.put("description", description);
    document.put("createdBy", createdBy);
    document.put("announcement3day", false);
    document.put("announcement1day", false);

    alerts.insertOne(document);
  }

  private void updateAlert(Document updatedAlert) {
    var filter = eq("_id", updatedAlert.get("_id"));

    if (alerts.find(filter).first() != null) {
      alerts.replaceOne(filter, updatedAlert);
    }
  }

  /**
   * Ändert eine Erinnerung.
   *
   * @param name Name der Erinnerung.
   * @param property Eigenschaft, die geändert werden soll.
   * @param value Der neue Wert für die ausgewählte Eigenschaft.
   */
  public void editAlert(String name, String property, String value) {
    var filter = eq("name", name);

    if (exists(name)) {
      var doc = alerts.find(filter).first();
      doc.replace(property, value);

      if (property.equalsIgnoreCase("date")) {
        doc.replace("announcement3day", false);
        doc.replace("announcement1day", false);
      }

      alerts.replaceOne(filter, doc);
    }
  }

  /**
   * Entfernt eine Erinnerung.
   *
   * @param name Name der Erinnerung.
   */
  public void removeAlert(String name) {
    if (exists(name)) {
      alerts.deleteOne(eq("name", name));
    }
  }

  /**
   * Gibt alle hinzugefügten Erinnerungen zurück.
   *
   * @return alle hinzugefügten Erinnerungen.
   */
  public FindIterable<Document> getAlerts() {
    return alerts.find();
  }

  private Document getAlertByName(String name) {
    if (exists(name)) {
      return alerts.find(eq("name", name)).first();
    }
    return null;
  }

  private void sendAlert(Document alert) {
    var embed = new EmbedBuilder()
        .setColor(Color.ORANGE)
        .setTitle("Erinnerung")
        .addField("Name", alert.getString("name"), false)
        .addField("Datum", alert.getString("date"), false)
        .addField("Beschreibung", alert.getString("description"), false)
        .setFooter("Hinzugefügt von " + alert.getString("createdBy"))
        .build();

    jda.getTextChannelById(1155244343054053526L)
        .sendMessage("||" + jda.getRoleById(1154366957974474853L).getAsMention() + "||")
        .setEmbeds(embed).queue();
  }

  /**
   * Gibt eine Embed Message für eine Erinnerung zurück.
   *
   * @param name Name der Erinnerung.
   * @return EmbedMessage für die Erinnerung.
   */
  public MessageEmbed getAlertEmbedMessage(String name) {
    var alert = getAlertByName(name);
    return new EmbedBuilder()
        .setColor(Color.ORANGE)
        .setTitle("Erinnerung")
        .addField("Name", alert.getString("name"), false)
        .addField("Datum", alert.getString("date"), false)
        .addField("Beschreibung", alert.getString("description"), false)
        .setFooter("Hinzugefügt von " + alert.getString("createdBy"))
        .build();
  }
}
