package de.efi23a.bot.database.repository;

import de.efi23a.bot.database.model.TaskModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;

public interface TaskRepository extends MongoRepository<TaskModel, String> {

  @Query("{title:'?0'}")
  TaskModel findTaskByTitle(String title);

  @Query("{id:'?0'}")
  TaskModel findTaskById(String id);

  @Query(value="{category:'?0'}")
  List<TaskModel> findTasksByCategory(String category);

  public long count();

}
