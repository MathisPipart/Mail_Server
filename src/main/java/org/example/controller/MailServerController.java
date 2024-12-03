package org.example.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import org.example.model.Email;
import org.example.model.MailBox;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MailServerController {
    private final MailBox mailBox = new MailBox();
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10); // Pool de 10 threads
    private volatile boolean running = true;

    @FXML
    private TextArea logArea;

    // Méthode pour démarrer le serveur
    public void startServer() {
        logMessage("Starting the server...");
        try (ServerSocket serverSocket = new ServerSocket(8189)) {
            logMessage("Server started, waiting for clients...");

            while (running) {
                try {
                    // Accepter les connexions entrantes
                    Socket incoming = serverSocket.accept();
                    logMessage("Client connected.");

                    // Confier la gestion du client à un thread
                    threadPool.execute(new ClientHandler(incoming));
                } catch (IOException e) {
                    if (running) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            stopServer();
        }
    }

    // Méthode pour arrêter le serveur
    public void stopServer() {
        running = false;
        System.out.println("Stopping the server...");
        threadPool.shutdown();
    }

    // Classe interne pour gérer les connexions clients
    private class ClientHandler implements Runnable {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                // Envoyer un message de bienvenue
                out.println("Welcome to the Mail Server!");

                while (!socket.isClosed()) {
                    try {
                        // Lire l'objet envoyé par le client
                        Object receivedObject = inStream.readObject();

                        if (receivedObject instanceof Email) {
                            Email email = (Email) receivedObject;

                            // Traiter l'email reçu
                            logMessage("Received email:");
                            logMessage("\tFrom: " + email.getSender());
                            logMessage("\tTo: " + email.getReceiver());
                            logMessage("\tSubject: " + email.getSubject());
                            logMessage("\tContent: " + email.getContent());

                            // Ajouter l'email à la boîte mail
                            synchronized (mailBox) {
                                mailBox.addEmail(email);
                            }

                            // Répondre au client
                            out.println("Mail received successfully.");
                        } else {
                            out.println("Invalid object received.");
                        }
                    } catch (EOFException | SocketException e) {
                        // Le client s'est déconnecté
                        logMessage("Client disconnected.");
                        break; // Sortir de la boucle immédiatement
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (!socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public void logMessage(String message) {
        if (logArea != null) {
            Platform.runLater(() -> logArea.appendText(message + "\n"));
        }
    }
}
