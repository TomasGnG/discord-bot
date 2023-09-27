package de.efi23a.bot.task.controller;

import de.efi23a.bot.database.model.TaskModel;
import de.efi23a.bot.task.TaskProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web Endpoint Controller.
 */
@RestController
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class TaskController {

  // TODO: add auth for every endpoint

  private final TaskProvider provider;

  @GetMapping("/task")
  TaskModel getTask() {
    return null;
  }

}
