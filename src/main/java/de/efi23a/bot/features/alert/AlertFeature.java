package de.efi23a.bot.features.alert;

import static com.mongodb.client.model.Filters.eq;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.efi23a.bot.database.MongoConfig;
import jakarta.annotation.PostConstruct;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.bson.Document;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Alert Feature von Tomas Keder.
 */
@Component
@RequiredArgsConstructor
public class AlertFeature {

  /**
   * Die Channel-ID für die Erinnerungen.
   */
  private static final String ALERT_CHANNEL_ID = "ALERT_CHANNEL_ID";
  /**
   * Die Rollen-ID, die gepingt werden soll.
   */
  private static final String ALERT_ROLE_ID = "ALERT_ROLE_ID";
  /**
   * Die Zeit (in Stunden), wann die erste Erinnerung abgeschickt werden soll.
   * Optimal: 72 Stunden → Unter 72 Stunden wird eine Erinnerung geschickt.
   */
  private static final String ALERT_FIRST_REMINDER = "ALERT_FIRST_REMINDER";
  /**
   * Die Zeit (in Stunden), wann die letzte Erinnerung abgeschickt werden soll.
   * Optimal: 24 Stunden → Unter 24 Stunden wird eine Erinnerung geschickt.
   */
  private static final String ALERT_LAST_REMINDER = "ALERT_LAST_REMINDER";

  private final JDA jda;
  private final MongoConfig mongoConfig;
  private MongoClient client;
  private MongoDatabase db;
  private MongoCollection<Document> alerts;
  private final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

  @PostConstruct
  void postConstruct() {
    client = mongoConfig.mongoClient();
    db = client.getDatabase(System.getenv("DATABASE"));
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
                        + sdf.format(new Date(System.currentTimeMillis())),
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

  @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
  private void startAlertCheckerTask() {
    var alerts = getAlerts();

    for (var alert : alerts) {
      var alertDate = Duration.between(Instant.now(), alert.getDate("date").toInstant());
      var lastReminder = getAlertLastReminder(alert);
      int firstReminderHours = Integer.parseInt(System.getenv(ALERT_FIRST_REMINDER));
      int lastReminderHours = Integer.parseInt(System.getenv(ALERT_LAST_REMINDER));

      if ((alertDate.get(ChronoUnit.SECONDS) / 60 / 60) < lastReminderHours
          && lastReminder == null
          || (Duration.between(Instant.now(), lastReminder.toInstant())
          .getSeconds() / 60 / 60) > lastReminderHours) {
        alert.replace("lastReminder", Instant.now());
        updateAlert(alert);
        sendAlert(alert);
        return;
      }

      if (Duration.between(Instant.now(), lastReminder.toInstant())
          .getSeconds() / 60 / 60 > firstReminderHours) {
        alert.replace("lastReminder", Instant.now());
        updateAlert(alert);
        sendAlert(alert);
        return;
      }

      if ((alertDate.getSeconds() / 60 / 60) < -36) {
        removeAlert(alert.getString("name"));
      }
    }
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
  public void addAlert(String name, Date date, String description, String createdBy) {
    var document = new Document();

    document.put("name", name);
    document.put("date", date);
    document.put("description", description);
    document.put("createdBy", createdBy);
    document.put("lastReminder", null);

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
  public void editAlert(String name, String property, Object value) {
    var filter = eq("name", name);

    if (exists(name)) {
      var doc = alerts.find(filter).first();
      doc.replace(property, value);

      if (property.equalsIgnoreCase("date")) {
        doc.replace("lastReminder", null);
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

  private Date getAlertLastReminder(Document document) {
    Instant lastReminder = (Instant) document.get("lastReminder");
    return lastReminder != null ? Date.from(lastReminder) : null;
  }

  private void sendAlert(Document alert) {
    var embed = new EmbedBuilder()
        .setColor(Color.ORANGE)
        .setTitle("Erinnerung")
        .addField("Name", alert.getString("name"), false)
        .addField("Datum", sdf.format(alert.getDate("date")), false)
        .addField("Beschreibung", alert.getString("description"), false)
        .setFooter("Hinzugefügt von " + alert.getString("createdBy"))
        .build();

    jda.getTextChannelById(Long.parseLong(System.getenv(ALERT_CHANNEL_ID)))
        .sendMessage("||"
            + jda.getRoleById(Long.parseLong(System.getenv(ALERT_ROLE_ID))).getAsMention()
            + "||")
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
        .addField("Datum",sdf.format(alert.getDate("date")), false)
        .addField("Beschreibung", alert.getString("description"), false)
        .setFooter("Hinzugefügt von " + alert.getString("createdBy"))
        .build();
  }
}
