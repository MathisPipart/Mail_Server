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
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MailServerController {
    private final MailBox mailBox = new MailBox();
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10); // Pool de 10 threads
    private volatile boolean running = true;
    private static final AtomicInteger emailCounter = new AtomicInteger(0); // Compteur global pour IDs uniques

    @FXML
    private TextArea logArea;

    // Méthode pour démarrer le serveur
    public void startServer() {
        logMessage("Starting the server...");

        // Charger les emails depuis le fichier
        loadEmailsFromFile();

        try (ServerSocket serverSocket = new ServerSocket(8189)) {
            logMessage("Server started, waiting for clients...");
            while (running) {
                try {
                    Socket incoming = serverSocket.accept();
                    logMessage("Client connected.");
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


    private void loadEmailsFromFile() {
        File file = getWritableDataFile();

        if (!file.exists()) {
            logMessage("data.txt does not exist. Starting fresh.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            int maxId = 0;

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(";");
                if (parts.length != 6) {
                    logMessage("Invalid line in data.txt: " + line);
                    continue;
                }

                // Construire l'email
                Email email = new Email(
                        Integer.parseInt(parts[0]), // ID
                        parts[1],                   // Expéditeur
                        Arrays.asList(parts[2].split(",")), // Destinataires
                        parts[3],                   // Sujet
                        parts[4],                   // Contenu
                        LocalDateTime.parse(parts[5]) // Timestamp
                );

                // Ajouter l'email à la boîte mail
                synchronized (mailBox) {
                    mailBox.addEmail(email);
                }

                // Mettre à jour le maximum d'ID
                maxId = Math.max(maxId, email.getId());
            }

            // Initialiser le compteur d'ID
            emailCounter.set(maxId);
            logMessage("Emails loaded from data.txt. Max ID: " + maxId);
        } catch (IOException e) {
            logMessage("Error reading data.txt: " + e.getMessage());
        }
    }


    // Méthode pour arrêter le serveur
    public void stopServer() {
        running = false;
        logMessage("Stopping the server...");
        threadPool.shutdown();
    }

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
                        Object receivedObject = inStream.readObject();

                        if (receivedObject instanceof Email) {
                            handleEmail((Email) receivedObject, out);
                        } else if (receivedObject instanceof String && ((String) receivedObject).startsWith("RETRIEVE_MAILS:")) {
                            handleRetrieveMails((String) receivedObject, out);
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

    // Gérer un email reçu
    private void handleEmail(Email email, PrintWriter out) {
        int generatedId = emailCounter.incrementAndGet();
        email = new Email(
                generatedId,
                email.getSender(),
                email.getReceiver(),
                email.getSubject(),
                email.getContent(),
                email.getTimestamp() != null ? email.getTimestamp() : LocalDateTime.now()
        );

        synchronized (mailBox) {
            mailBox.addEmail(email);
        }

        writeEmailToFile(email);

        out.println("Mail received successfully with ID: " + generatedId);
        logMessage("Email with ID " + generatedId + " successfully handled.");
    }

    // Gérer la récupération des emails
    private void handleRetrieveMails(String command, PrintWriter out) {
        String userEmail = command.replace("RETRIEVE_MAILS:", "").trim();

        synchronized (mailBox) {
            for (Email email : mailBox.getEmails()) {
                if (email.getReceiver().contains(userEmail)) {
                    out.println(serializeEmail(email));
                }
            }
        }
        out.println("END_OF_MAILS");
    }


    private String serializeEmail(Email email) {
        return email.getId() + ";" +
                email.getSender() + ";" +
                String.join(",", email.getReceiver()) + ";" +
                email.getSubject() + ";" +
                email.getContent() + ";" +
                email.getTimestamp();
    }

    private File getWritableDataFile() {
        String targetDir = "data";
        File directory = new File(targetDir);
        if (!directory.exists()) {
            directory.mkdir();
        }

        return new File(directory, "data.txt");
    }

    private void writeEmailToFile(Email email) {
        File file = getWritableDataFile();

        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(serializeEmail(email) + "\n");
            logMessage("Email written to data.txt with ID: " + email.getId());
        } catch (IOException e) {
            logMessage("Error writing to data.txt: " + e.getMessage());
        }
    }

    public void logMessage(String message) {
        if (logArea != null) {
            Platform.runLater(() -> logArea.appendText(message + "\n"));
        }
    }
}
