package com.example.todo;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.TasksScopes;

public class TodoController {
    @FXML private Button completeButton;
    @FXML private DatePicker datePicker;
    @FXML private Button deleteButton;
    @FXML private TextField descriptionText;
    @FXML private Label errorLabel;
    @FXML private SplitPane mainPane;
    @FXML private CheckBox noDueDateCheckBox;
    @FXML private SplitPane splitPane;
    @FXML private ListView<TodoTask> taskList;
    @FXML private ListView<TodoTask> taskListDone;
    @FXML private CheckBox urgentCheckBox;

    ObservableList<TodoTask> list = FXCollections.observableArrayList();
    ObservableList<TodoTask> listDone = FXCollections.observableArrayList();

    private Tasks service;

    private static final String APPLICATION_NAME = "Google Tasks Api";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private static final List<String> SCOPES = Collections.singletonList(TasksScopes.TASKS);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    @FXML
    public void initialize() throws Exception {
        datePicker.setValue(LocalDate.now());
        toggleButtons(list.isEmpty() && listDone.isEmpty());

        SplitPane.Divider divider = splitPane.getDividers().get(0);
        divider.positionProperty().addListener((observable, oldvalue, newvalue) -> divider.setPosition(0.5));

       service = getService(GoogleNetHttpTransport.newTrustedTransport());

        taskList.getSelectionModel().clearSelection();
        taskListDone.getSelectionModel().clearSelection();
    }

    @FXML
    void addNewTask(ActionEvent event) {
        if (addTaskValidate())
            addTaskCommit();
    }

    @FXML
    void addTaskClicked(MouseEvent event) {
        urgentCheckBox.setDisable(false);
        taskList.getSelectionModel().clearSelection();
        taskListDone.getSelectionModel().clearSelection();

        Scene scene = (Scene) mainPane.getScene();

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                enterPressed();
            }
        });
    }

    @FXML
    void deleteTask(ActionEvent event) {
        list.remove(taskList.getSelectionModel().getSelectedItem());
        listDone.remove(taskListDone.getSelectionModel().getSelectedItem());
        taskList.refresh();
        taskListDone.refresh();

        toggleButtons(list.isEmpty() && listDone.isEmpty());
        taskList.getSelectionModel().clearSelection();
        taskListDone.getSelectionModel().clearSelection();
    }

    @FXML
    void doneListClicked(MouseEvent event) {
        if (!listDone.isEmpty()) {
            completeButton.setText("Mark as incomplete");
        }

        completeButton.setDisable(listDone.isEmpty());
        deleteButton.setDisable(listDone.isEmpty());
        urgentCheckBox.setDisable(true);
        taskList.getSelectionModel().clearSelection();
    }

    @FXML
    void listClicked(MouseEvent event) {
        if (!list.isEmpty()) {
            completeButton.setText("Mark as complete");
        }

        completeButton.setDisable(list.isEmpty());
        deleteButton.setDisable(list.isEmpty());
        urgentCheckBox.setDisable(true);
        taskListDone.getSelectionModel().clearSelection();
    }

    @FXML
    void markAsComplete(ActionEvent event) {
        TodoTask task = null;

        if (taskList.isFocused()) {
            task = taskList.getSelectionModel().getSelectedItem();
        } else if (taskListDone.isFocused()) {
            task = taskListDone.getSelectionModel().getSelectedItem();
        }

        if (task != null) {
            if (task.isCompleted()) {
                if (task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now())) {
                    printError("Cannot mark a task incomplete if it's past the due date");
                    return;
                }
                listDone.remove(task);
                list.add(task);
                task.setCompleted(false);
                sortListByDate(list);
                taskList.setItems(list);
            } else {
                list.remove(task);
                listDone.add(task);
                task.setCompleted(true);
               task.setCompletionDate(LocalDate.now());
                sortListByDate(listDone);
                taskListDone.setItems(listDone);
            }
        }
    }

    @FXML
    void noDueDateSelected(ActionEvent event) {
        datePicker.setDisable(noDueDateCheckBox.isSelected());
    }

    @FXML
    void urgentSelected(ActionEvent event) {
        noDueDateCheckBox.setDisable(urgentCheckBox.isSelected());
        noDueDateCheckBox.setSelected(urgentCheckBox.isSelected());
        datePicker.setDisable(urgentCheckBox.isSelected());
    }

    private void addTaskCommit() {
        TodoTask task = new TodoTask(descriptionText.getText(), datePicker.getValue(), urgentCheckBox.isSelected());
        list.add(task);

        sortListByDate(list);
        taskList.setItems(list);
        descriptionText.setText("");
        errorLabel.setText("");

        toggleButtons(list.isEmpty() && listDone.isEmpty());
        datePicker.setDisable(false);
        noDueDateCheckBox.setSelected(false);
        noDueDateCheckBox.setDisable(false);
        urgentCheckBox.setSelected(false);
        urgentCheckBox.setDisable(false);
        datePicker.setValue(LocalDate.now());
    }

    private boolean addTaskValidate() {
        // Check for invalid input
        if (descriptionText.getText().equals("")) {
            printError("Cannot create an empty task");
            return false;
        } else if (datePicker.getValue().isBefore(LocalDate.now())) {
            printError("Cannot create a task in the past");
            return false;
        }

        // Check for duplicates
        if (isDuplicate()) {
            printError("Cannot create duplicate tasks");
            return false;
        }
        return true;
    }

    private void enterPressed() {
        if (descriptionText.isFocused()) {
             addNewTask(null);
        }
    }

    private static Tasks getService(final NetHttpTransport HTTP_TRANSPORT) throws Exception {
        InputStream in = TodoController.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        Credential credential =  new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        System.out.println("Got the credentials");
        return new Tasks.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private boolean isDuplicate() {
        for (TodoTask task : list) {
            if (descriptionText.getText().equals(task.getDescription())) {
                if (datePicker.isDisabled()) {
                    if (task.getDueDate() == null) {
                        return true;
                    }
                } else {
                    if (task.getDueDate() != null
                            && task.getDueDate().isEqual(datePicker.getValue())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void printError(String text) {
        errorLabel.setText(text);
        errorLabel.setTextFill(Color.RED);
    }

    public void sortListByDate(ObservableList<TodoTask> list) {
        list.sort((t1, t2) -> {
            if (t1.getDueDate() == null) {
                return 1;
            } else if (t2.getDueDate() == null) {
                return -1;
            }

            if (t1.getDueDate().isAfter(t2.getDueDate()) || t1.getDueDate().isEqual(t2.getDueDate())) {
                return 1;
            }
            if (t1.getDueDate().isBefore(t2.getDueDate())) {
                return -1;
            }
            return 0;
        });
    }

    // private LocalDate toLocalDate(DateTime dt) {
    //     try {
    //         DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    //         return LocalDate.parse(dt.toStringRfc3339(), f);
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //         return null;
    //     }
    // }

    private void toggleButtons(boolean listEmpty) {
        completeButton.setDisable(listEmpty);
        deleteButton.setDisable(listEmpty);
    }
}