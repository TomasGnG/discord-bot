package de.efi23a.bot.task;

import de.efi23a.bot.database.model.TaskModel;
import de.efi23a.bot.database.repository.TaskRepository;
import lombok.Getter;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Date;

@Getter
@Component
public class TaskProvider {

  private final Log LOG = LogFactory.getLog(getClass().getName());

  private final TaskRepository taskRepository;

  @Autowired // autowired automatically gets taskRepository
  public TaskProvider(TaskRepository taskRepository) {
    this.taskRepository = taskRepository;
  }

  //... adds task with given arguments to database
  // TODO: add freshly-added task to scheduler
  // TODO: scheduler logic
  public TaskModel addTask(String title, String description, Date date, int importance) {
    return taskRepository.save(new TaskModel(title, description, date, importance));
  }

  //... sets task with Id 'oldId' to Id 'newId'
  //... deletes task Id 'oldId' from database
  public void setTaskId(String oldId, String newId) {
    TaskModel task = taskRepository.findTaskById(oldId);
    this.taskRepository.delete(task);
    task.setId(newId);
    this.taskRepository.save(task);
  }

}
