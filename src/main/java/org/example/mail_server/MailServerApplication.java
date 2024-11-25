package org.example.mail_server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Scanner;
import java.util.Vector;

public class MailServerApplication extends Application {
    private static final int PORT = 8189;
    private TextArea logArea; // Zone pour afficher les logs

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

        ConnexionClient connexionClient = new ConnexionClient();
        connexionClient.startServer();
    }


}