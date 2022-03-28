module com.example.todo {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;

    requires org.kordamp.ikonli.javafx;
    requires com.google.api.services.tasks;
    requires com.google.api.client;
    requires google.api.client;
    requires transitive google.http.client.jackson2;
    requires google.oauth.client.jetty;
    requires google.oauth.client.java6;
    requires google.oauth.client;
    opens com.example.todo to javafx.fxml;
    exports com.example.todo;
}