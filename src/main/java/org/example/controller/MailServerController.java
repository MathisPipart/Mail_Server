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
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MailServerController {
    private final MailBox mailBox = new MailBox();
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10); // Pool de 10 threads
    private volatile boolean running = true;
    private static final AtomicInteger emailCounter = new AtomicInteger(0); // Compteur global pour générer des IDs uniques

    @FXML
    private TextArea logArea;

    // Méthode pour démarrer le serveur
    public void startServer() {
        logMessage("Starting the server...");

        // S'assurer que le fichier data.txt est prêt
        getWritableDataFile();

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
                        logMessage("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            logMessage("Error starting server: " + e.getMessage());
        } finally {
            stopServer();
        }
    }

    // Méthode pour arrêter le serveur
    public void stopServer() {
        running = false;
        logMessage("Stopping the server...");
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

                out.println("Welcome to the Mail Server!");

                while (!socket.isClosed()) {
                    try {
                        // Lire l'objet envoyé par le client
                        Object receivedObject = inStream.readObject();

                        if (receivedObject instanceof Email) {
                            Email email = (Email) receivedObject;

                            // Générer un ID unique pour l'email
                            int generatedId = emailCounter.incrementAndGet();
                            email = new Email(
                                    generatedId, // ID généré
                                    email.getSender(),
                                    email.getReceiver(),
                                    email.getSubject(),
                                    email.getContent(),
                                    email.getTimestamp() != null ? email.getTimestamp() : LocalDateTime.now()
                            );

                            logMessage("Generated ID for email: " + generatedId);

                            // Ajouter l'email à la boîte mail
                            synchronized (mailBox) {
                                mailBox.addEmail(email);
                            }

                            // Enregistrer l'email dans un fichier
                            writeEmailToFile(email);

                            // Répondre au client
                            out.println("Mail received successfully with ID: " + generatedId);
                        } else {
                            out.println("Invalid object received.");
                        }
                    } catch (EOFException | SocketException e) {
                        logMessage("Client disconnected.");
                        break;
                    } catch (IOException | ClassNotFoundException e) {
                        logMessage("Error handling client: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                logMessage("Error in client handler: " + e.getMessage());
            } finally {
                try {
                    if (!socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    logMessage("Error closing client socket: " + e.getMessage());
                }
            }
        }
    }

    // Méthode pour récupérer ou copier le fichier data.txt vers un emplacement accessible
    private File getWritableDataFile() {
        // Dossier cible pour le fichier
        String targetDir = "data";
        File directory = new File(targetDir);
        if (!directory.exists()) {
            directory.mkdir(); // Crée le dossier si nécessaire
        }

        // Fichier cible dans le dossier accessible
        File writableFile = new File(directory, "data.txt");

        // Copier le fichier s'il n'existe pas encore
        if (!writableFile.exists()) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("org/example/data/data.txt");
                 FileOutputStream out = new FileOutputStream(writableFile)) {

                if (in == null) {
                    logMessage("data.txt not found in resources!");
                    return writableFile;
                }

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                logMessage("data.txt copied to writable location: " + writableFile.getAbsolutePath());
            } catch (IOException e) {
                logMessage("Error copying data.txt: " + e.getMessage());
            }
        }
        return writableFile;
    }

    // Méthode pour écrire un email dans le fichier data.txt
    private void writeEmailToFile(Email email) {
        File file = getWritableDataFile();

        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write("ID: " + email.getId() + "\n");
            writer.write("From: " + email.getSender() + "\n");
            writer.write("To: " + email.getReceiver() + "\n");
            writer.write("Subject: " + email.getSubject() + "\n");
            writer.write("Content: " + email.getContent() + "\n");
            writer.write("Timestamp: " + email.getTimestamp() + "\n");
            writer.write("-------------------------------\n");
            logMessage("Email written to data.txt with ID: " + email.getId());
        } catch (IOException e) {
            logMessage("Error writing to data.txt: " + e.getMessage());
        }
    }

    // Méthode pour afficher les logs dans l'interface graphique
    public void logMessage(String message) {
        if (logArea != null) {
            Platform.runLater(() -> logArea.appendText(message + "\n"));
        }
    }
}
