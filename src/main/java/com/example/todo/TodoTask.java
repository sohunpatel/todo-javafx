package com.example.todo;

import com.google.api.services.tasks.model.Task;
import java.io.Serializable;
import java.time.LocalDate;

public class TodoTask implements Serializable {
    private final Task task;
    private boolean completed;
    private LocalDate completionDate;
    private String description;
    private LocalDate dueDate;
    private boolean urgent;

    public TodoTask(String description, LocalDate date, boolean urgent) {
        this.description = description;
        this.dueDate = date;
        this.completionDate = null;
        this.completed = false;
        this.urgent = urgent;
        task = new Task();
        task.setTitle(description);
    }

    @Override
    public String toString() {
        String[] dateArray = null;
        String[] completionDateArray = null;
        if (dueDate != null) {
            dateArray = dueDate.toString().split("-");
        }
        if (completionDate != null) {
            completionDateArray = completionDate.toString().split("-");
        }

        if (urgent) {
            if (completed) {
                assert completionDateArray != null;
                return completionDateArray[2] + "/" + completionDateArray[1] +
                    "/" + completionDateArray[0] + " | " + description +
                    ((dateArray == null)
                         ? ""
                         : "  (due " + dateArray[2] + "/" + dateArray[1] + "/" +
                               dateArray[0] + ")");
            } else {
                return "URGENT | " + description;
            }
        } else {
            if (completed) {
                assert completionDateArray != null;
                return completionDateArray[2] + "/" + completionDateArray[1] +
                    "/" + completionDateArray[0] + " | " + description +
                    ((dateArray == null)
                         ? ""
                         : "   due: " + dateArray[2] + "/" + dateArray[1] +
                               "/" + dateArray[0] + ")");
            } else {
                return ((dateArray == null)
                            ? ""
                            : "   due: " + dateArray[2] + "/" + dateArray[1] +
                                  "/" + dateArray[0]) +
                    " | " + description;
            }
        }
    }

    public boolean isCompleted() { return completed; }

    public LocalDate getCompletionDate() { return completionDate; }

    public String getDescription() { return description; }

    public LocalDate getDueDate() { return dueDate; }

    public boolean isUrgent() { return urgent; }

    public void setCompleted(boolean completed) { this.completed = completed; }

    public void setCompletionDate(LocalDate completionDate) {
        this.completionDate = completionDate;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public void setUrgent(boolean urgent) { this.urgent = urgent; }
}
