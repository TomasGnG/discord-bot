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

@Slf4j
@Component
@RequiredArgsConstructor
public class MailForwardingFeature {

  private static final String INBOX_FOLDER = "INBOX";

  private static final String MAIL_URL_VARIABLE = "MAIL_URL";

  private static final String MAIL_CHANNEL_ID = "MAIL_CHANNEL_ID";
  private final JDA jda;

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

  private void handleMailForwarding(@NotNull Session session, @NotNull String url,
                                    @NotNull TextChannel textChannel)
      throws MessagingException, IOException {

    try (Store store = openStore(session, url); Folder folder = openFolder(store)) {
      for (Message message : getNewMails(folder)) {
        MessageEmbed messageEmbed = buildEmbed(message);
        textChannel.sendMessageEmbeds(messageEmbed).queue();

        message.setFlag(Flags.Flag.SEEN, true);
      }
    }
  }

  @NotNull
  public Session createSession() {
    return Session.getInstance(getMailProperties());
  }

  @NotNull
  public Store openStore(@NotNull Session session, @NotNull String url) throws MessagingException {
    Store store = session.getStore(new URLName(url));
    store.connect();
    return store;
  }

  @NotNull
  public Folder openFolder(@NotNull Store store) throws MessagingException {
    Folder folder = store.getFolder(INBOX_FOLDER);
    folder.open(Folder.READ_WRITE);
    return folder;
  }

  @NotNull
  public List<Message> getNewMails(@NotNull Folder emailFolder) throws MessagingException {
    return Arrays.asList(emailFolder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false)));
  }

  @NotNull
  public Properties getMailProperties() {
    Properties properties = new Properties();
    properties.put("mail.store.protocol", "imap");
    properties.put("mail.imap.starttls.enable", "true");
    return properties;
  }
  //</editor-fold>

  //<editor-fold desc="Environment Variables">

  @Nullable
  @Contract(pure = true)
  public String fetchMailUrl() {
    return System.getenv(MAIL_URL_VARIABLE);
  }

  @Nullable
  @Contract(pure = true)
  public String fetchMailChannelId() {
    return System.getenv(MAIL_CHANNEL_ID);
  }
  //</editor-fold>

  //<editor-fold desc="Embed Building">

  @NotNull
  public MessageEmbed buildEmbed(@NotNull Message message) throws MessagingException, IOException {
    return new EmbedBuilder()
        .setTitle(message.getSubject())
        .setAuthor(buildAddressString(message.getFrom()))
        .setDescription(buildBody(message))
        .build();
  }

  @NotNull
  private String buildAddressString(@NotNull Address[] addresses) {
    return Arrays.stream(addresses)
        .map(Address::toString)
        .collect(Collectors.joining(", "));
  }

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
