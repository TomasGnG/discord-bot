package de.efi23a.bot.database.model;

import java.util.Date;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Database Model.
 */
@Setter
@Getter
@Document("task")
public class TaskModel {

  @Id
  private String id;

  private String title;
  private String description;
  private Date date;
  private int importance;

  /**
   * Constructor.
   *
   * @param title task title
   * @param description task description
   * @param date task date (to when?)
   * @param importance task importance
   */
  public TaskModel(String title, String description, Date date, int importance) {
    this.id = UUID.randomUUID().toString();
    this.title = title;
    this.description = description;
    this.date = date;
    this.importance = importance;
  }
}
