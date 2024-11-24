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
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();

        startServer();
    }

    public void startServer() {
        System.out.println("Finestra del socket server");
        try {
            ServerSocket s = new ServerSocket(8189);
            Socket incoming = s.accept( );
            try {
                ObjectInputStream inStream = new ObjectInputStream(incoming.getInputStream());
                OutputStream outStream = incoming.getOutputStream();

                PrintWriter out = new PrintWriter(outStream, true);

                out.println( "Hello! Waiting for data." );

                // echo client input
                Vector<Date> v = null;
                try {
                    v = ((Vector<Date>)inStream.readObject());
                } catch (ClassNotFoundException e) {System.out.println(e.getMessage());}

                if (v!=null)
                    for (int i=0; i<v.size(); i++) {
                        Date date = v.get(i);
                        System.out.println("Echo: " + date);
                        out.println("Echo: " + date);
                    }
            }
            finally {
                incoming.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}