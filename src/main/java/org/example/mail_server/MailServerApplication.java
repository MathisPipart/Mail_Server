package org.example.mail_server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.mail_server.controller.MailServerController;

import java.io.*;

public class MailServerApplication extends Application {
    private static final int PORT = 8189;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MailServerApplication.class.getResource("mailServer-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Mail Server");
        stage.setScene(scene);
        stage.show();

        // Récupérer le contrôleur associé à la vue FXML
        MailServerController mailServerController = fxmlLoader.getController();

        // Lancer le serveur sur un thread séparé
        Thread serverThread = new Thread(mailServerController::startServer);
        serverThread.setDaemon(true); // Permet de fermer ce thread lorsque l'application JavaFX se termine
        serverThread.start();
    }
}
