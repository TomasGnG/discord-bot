package de.efi23a.bot.features;

import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.URLName;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.FlagTerm;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Die Mailweiterleitungsfunktion kann sich in einen IMAP-Server einloggen und die dort
 * neuen eingegangenen Mails in einen Discord-Textkanal weiterleiten.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MailForwardingFeature {

  /**
   * Der Name des Ordners, in dem die neuen Mails gesucht werden sollen.
   */
  private static final String INBOX_FOLDER = "INBOX";
  /**
   * Der Name der Umgebungsvariable, die die URL des IMAP-Servers enthält.
   */
  private static final String MAIL_URL_VARIABLE = "MAIL_URL";
  /**
   * Der Name der Umgebungsvariable, die die Channel-ID des Ziel-Discord-Textkanals enthält.
   */
  private static final String MAIL_CHANNEL_ID = "MAIL_CHANNEL_ID";
  private final JDA jda;

  /**
   * Validiert regelmäßig die Umgebungsvariablen und leitet neue Mails weiter.
   *
   * @throws MessagingException Wird geworfen, wenn eine Mail nicht gelesen werden kann.
   * @throws IOException        Wird geworfen, wenn der Inhalt einer Mail nicht gelesen werden kann.
   */
  @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
  private void runScheduledTask() throws MessagingException, IOException {
    log.info("Running mail task");

    String mailChannelId = fetchMailChannelId();
    String url = fetchMailUrl();

    if (mailChannelId == null) {
      throw new IllegalStateException("Mail channel id is null");
    }
    if (url == null) {
      throw new IllegalStateException("Mail url is null");
    }

    TextChannel textChannel = jda.getTextChannelById(mailChannelId);
    if (textChannel == null) {
      throw new IllegalStateException("Invalid mail channel id");
    }

    handleMailForwarding(createSession(), url, textChannel);

    log.info("Finished mail task");
  }

  //<editor-fold desc="Mail Logik">

  /**
   * Leitet neue Mails aus dem IMAP-Server in den Discord-Textkanal weiter.
   *
   * @param session     Die Session, die für die Verbindung zum IMAP-Server verwendet wird.
   * @param url         Die URL des IMAP-Servers.
   * @param textChannel Der Textkanal, in den die Mails weitergeleitet werden sollen.
   * @throws MessagingException Wird geworfen, wenn eine Mail nicht gelesen werden kann.
   * @throws IOException        Wird geworfen, wenn der Inhalt einer Mail nicht gelesen werden kann.
   */
  private void handleMailForwarding(@NotNull Session session, @NotNull String url,
                                    @NotNull TextChannel textChannel)
      throws MessagingException, IOException {
    for (Message message : fetchMail(session, url)) {
      MessageEmbed messageEmbed = buildEmbed(message);
      textChannel.sendMessageEmbeds(messageEmbed).queue();

      message.setFlag(Flags.Flag.SEEN, true);
    }
  }

  /**
   * Erstellt eine neue Sitzung für die Verbindung zu einem IMAPs-Server.
   *
   * @return Die erstellte Sitzung.
   */
  @NotNull
  public Session createSession() {
    return Session.getInstance(getMailProperties());
  }

  /**
   * Sammelt alle neuen Mails aus dem IMAP-Server.
   *
   * @param session Die Sitzung, die für die Verbindung zum IMAP-Server verwendet wird.
   * @param url     Die URL des IMAP-Servers.
   * @return Die Liste der neuen Mails.
   * @throws MessagingException Wird geworfen, wenn eine Mail nicht gelesen werden kann.
   */
  @NotNull
  public List<Message> fetchMail(@NotNull Session session, @NotNull String url)
      throws MessagingException {

    try (Store store = openStore(session, url); Folder folder = openFolder(store)) {
      return getNewMails(folder);
    }
  }


  /**
   * Stellt eine Verbindung zum IMAP-Server her.
   *
   * @param session Die Sitzung, die für die Verbindung zum IMAP-Server verwendet wird.
   * @param url     Die URL des IMAP-Servers.
   * @return Der geöffnete Store.
   * @throws MessagingException Wird geworfen, wenn eine Mail nicht gelesen werden kann.
   */
  @NotNull
  public Store openStore(@NotNull Session session, @NotNull String url) throws MessagingException {
    Store store = session.getStore(new URLName(url));
    store.connect();
    return store;
  }

  /**
   * Öffnet den Standard-Posteingang des IMAP-Servers, um neue Mails zu lesen.
   *
   * @param store Der Store, der für die Verbindung zum IMAP-Server verwendet wird.
   * @return Der geöffnete Posteingang.
   * @throws MessagingException Wird geworfen, wenn eine Mail nicht gelesen werden kann.
   */
  @NotNull
  public Folder openFolder(@NotNull Store store) throws MessagingException {
    Folder folder = store.getFolder(INBOX_FOLDER);
    folder.open(Folder.READ_WRITE);
    return folder;
  }

  /**
   * Sucht nach ungelesenen Mails in einem Ordner.
   *
   * @param emailFolder Der Ordner, in dem nach neuen Mails gesucht werden soll.
   * @return Die Liste der neuen Mails.
   * @throws MessagingException Wird geworfen, wenn eine Mail nicht gelesen werden kann.
   */
  @NotNull
  public List<Message> getNewMails(@NotNull Folder emailFolder) throws MessagingException {
    return Arrays.asList(emailFolder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false)));
  }

  /**
   * Erstellt die {@link Properties} für die Verbindung zum IMAPs-Server.
   *
   * @return Die erstellten Properties.
   */
  @NotNull
  public Properties getMailProperties() {
    Properties properties = new Properties();
    properties.put("mail.store.protocol", "imap");
    properties.put("mail.imap.starttls.enable", "true");
    return properties;
  }
  //</editor-fold>

  //<editor-fold desc="Environment Variables">

  /**
   * Liest die URL des Ziel-IMAP-Servers aus den Umgebungsvariablen.
   *
   * @return Die URL des Ziel-IMAP-Servers.
   */
  @Nullable
  @Contract(pure = true)
  public String fetchMailUrl() {
    return System.getenv(MAIL_URL_VARIABLE);
  }

  /**
   * Liest die Channel-ID des Ziel-Discord-Textkanals aus den Umgebungsvariablen.
   *
   * @return Die Channel-ID des Ziel-Discord-Textkanals.
   */
  @Nullable
  @Contract(pure = true)
  public String fetchMailChannelId() {
    return System.getenv(MAIL_CHANNEL_ID);
  }
  //</editor-fold>

  //<editor-fold desc="Embed Building">

  /**
   * Baut ein {@link MessageEmbed} aus einer {@link Message E-Mail-Nachricht}.
   *
   * @param message Die E-Mail-Nachricht, aus der das Embed gebaut werden soll.
   * @return Das gebaute Embed.
   * @throws MessagingException Wird geworfen, wenn eine Mail nicht gelesen werden kann.
   * @throws IOException        Wird geworfen, wenn der Inhalt einer Mail nicht gelesen werden kann.
   */
  @NotNull
  public MessageEmbed buildEmbed(@NotNull Message message) throws MessagingException, IOException {
    return new EmbedBuilder()
        .setTitle(message.getSubject())
        .setAuthor(buildAddressString(message.getFrom()))
        .setDescription(buildBody(message))
        .build();
  }

  /**
   * Baut einen Adressen-String aus einem Array von Adressen.
   *
   * @param addresses Das Array von Adressen, aus dem der String gebaut werden soll.
   * @return Der gebaute Adressen-String.
   */
  @NotNull
  private String buildAddressString(@NotNull Address[] addresses) {
    return Arrays.stream(addresses)
        .map(Address::toString)
        .collect(Collectors.joining(", "));
  }

  /**
   * Baut einen String aus dem Inhalt einer E-Mail-Nachricht.
   *
   * @param message Die E-Mail-Nachricht, aus der der String gebaut werden soll.
   * @return Der gebaute String.
   * @throws MessagingException Wird geworfen, wenn eine Mail nicht gelesen werden kann.
   * @throws IOException        Wird geworfen, wenn der Inhalt einer Mail nicht gelesen werden kann.
   */
  @NotNull
  private String buildBody(@NotNull Message message) throws MessagingException, IOException {
    Object content = message.getContent();

    if (content instanceof String) {
      return (String) content;
    }
    StringBuilder stringBuilder = new StringBuilder();
    buildBodyRecursively(content, stringBuilder);

    return stringBuilder.toString();
  }

  @ApiStatus.Internal
  private void buildBodyRecursively(Object content, StringBuilder stringBuilder)
      throws MessagingException, IOException {
    if (content instanceof MimeMultipart mimeMultipart) {
      for (int i = 0; i < mimeMultipart.getCount(); i++) {
        BodyPart bodyPart = mimeMultipart.getBodyPart(i);

        buildBodyRecursively(bodyPart.getContent(), stringBuilder);
      }
    } else if (content instanceof String string) {
      stringBuilder.append(string);
    }
  }
  //</editor-fold>

}
