package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.example.controller.MailServerController;

import java.io.*;

public class MailServerApplication extends Application {

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MailServerApplication.class.getResource("mailServer-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Mail Server");
        stage.setScene(scene);
        scene.getRoot().requestFocus();

        MailServerController mailServerController = fxmlLoader.getController();

        stage.show();

        // Run the server on a separate thread
        Thread serverThread = new Thread(mailServerController::startServer);
        serverThread.setDaemon(true); // Closes this thread when the application ends
        serverThread.start();
    }
}
