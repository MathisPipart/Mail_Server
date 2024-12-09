package org.example.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import org.example.model.Email;
import org.example.model.User;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MailServerController {
    private final Map<String, User> users = new HashMap<>();
    private final Map<String, Integer> emailCounters = new HashMap<>(); // ID unique pour chaque utilisateur
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10);
    private volatile boolean running = true;

    @FXML
    private TextArea logArea;

    public void startServer() {
        logMessage("Starting the server...");
        loadUsersFromFile();

        try (ServerSocket serverSocket = new ServerSocket(8189)) {
            logMessage("Server started. Waiting for clients...");
            while (running) {
                Socket clientSocket = serverSocket.accept();
                logMessage("Client connected.");
                threadPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            logMessage("Server error: " + e.getMessage());
        } finally {
            stopServer();
        }
    }

    private void loadUsersFromFile() {
        File file = getWritableDataFile();
        if (!file.exists()) {
            logMessage("data.txt does not exist. Starting fresh.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(";");
                String email = parts[0];
                User user = new User(email);

                if (parts.length > 1) {
                    String[] emailParts = parts[1].split("\\|");
                    for (String emailData : emailParts) {
                        String[] emailFields = emailData.split(",");
                        if (emailFields.length == 6) {
                            Email emailObj = new Email(
                                    Integer.parseInt(emailFields[0]),
                                    emailFields[1],
                                    Arrays.asList(emailFields[2].split(";")),
                                    emailFields[3],
                                    emailFields[4],
                                    LocalDateTime.parse(emailFields[5])
                            );
                            user.getMailBox().addEmail(emailObj);
                            emailCounters.put(email, Math.max(emailCounters.getOrDefault(email, 0), emailObj.getId()) + 1);
                        }
                    }
                }

                users.put(email, user);
            }
        } catch (IOException e) {
            logMessage("Error reading data.txt: " + e.getMessage());
        }
    }

    private File getWritableDataFile() {
        File directory = new File("data");
        if (!directory.exists()) {
            directory.mkdir();
        }
        return new File(directory, "data.txt");
    }

    private synchronized void addEmailToFile(String userEmail, Email email) throws IOException {
        File file = getWritableDataFile();
        List<String> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean updated = false;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith(userEmail + ";")) {
                    updated = true;
                    if (email != null) {
                        line = line + serializeEmail(email) + "|"; // Ajouter l'email si non null
                    }
                }
                lines.add(line);
            }

            if (!updated) {
                // Créer une nouvelle ligne pour l'utilisateur s'il n'existe pas déjà
                lines.add(userEmail + (email != null ? ";" + serializeEmail(email) + "|" : ";"));
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            for (String updatedLine : lines) {
                writer.write(updatedLine);
                writer.newLine();
            }
        }
    }


    private String serializeEmail(Email email) {
        return email.getId() + "," +
                email.getSender() + "," +
                String.join(";", email.getReceiver()) + "," +
                email.getSubject() + "," +
                email.getContent() + "," +
                email.getTimestamp();
    }

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
                    Object receivedObject = inStream.readObject();

                    if (receivedObject instanceof Email) {
                        handleEmail((Email) receivedObject, out);
                    } else if (receivedObject instanceof String) {
                        handleCommand((String) receivedObject, out);
                    } else if (receivedObject instanceof User) {
                        handleUser((User) receivedObject, out);
                    } else {
                        out.println("Invalid object received.");
                        System.err.println("Invalid object received: " + receivedObject);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                logMessage("Error handling client: " + e.getMessage());
            }
        }


        private void handleUser(User user, PrintWriter out) {
            String email = user.getEmail();

            synchronized (users) {
                if (!users.containsKey(email)) {
                    // Au lieu de créer un nouvel utilisateur, on envoie un message d'erreur
                    out.println("Error: User does not exist.");
                    logMessage("Connection attempt with non-existent user: " + email);
                } else {
                    // Si l'utilisateur existe, on le considère comme "connecté"
                    out.println("User connected successfully.");
                    logMessage("User connected: " + email);
                }
            }
        }



        private void handleEmail(Email email, PrintWriter out) {
            // Générer un ID unique pour cet email
            String senderEmail = email.getSender();
            int newId = emailCounters.getOrDefault(senderEmail, 0);
            email.setId(newId);
            emailCounters.put(senderEmail, newId + 1);

            boolean success = true;

            for (String receiver : email.getReceiver()) {
                User user = users.computeIfAbsent(receiver, User::new);
                user.getMailBox().addEmail(email);

                try {
                    addEmailToFile(receiver, email); // Ajouter au fichier
                } catch (Exception e) {
                    logMessage("Error adding email to file: " + e.getMessage());
                    success = false; // Marquer comme échoué si une écriture échoue
                }
            }

            if (success) {
                out.println("Mail received successfully with ID: " + email.getId());
            } else {
                out.println("Error processing email.");
            }
        }


        private void handleCommand(String command, PrintWriter out) {
            if (command.startsWith("RETRIEVE_MAILS:")) {
                handleRetrieveMails(command, out);
            } else if (command.startsWith("REGISTER_USER:")) {
                handleRegisterUser(command, out);
            } else if (command.startsWith("DELETE_MAIL:")) {
                handleDeleteMail(command, out);
            } else if (command.startsWith("CHECK_USER:")) {
                handleCheckUser(command, out);
            } else {
                out.println("Unknown command.");
            }
        }

        private void handleCheckUser(String command, PrintWriter out) {
            String userEmail = command.replace("CHECK_USER:", "").trim();
            synchronized (users) {
                if (users.containsKey(userEmail)) {
                    out.println("User exists");
                } else {
                    out.println("Error: User does not exist.");
                }
            }
        }


        private void handleDeleteMail(String command, PrintWriter out) {
            String[] parts = command.replace("DELETE_MAIL:", "").trim().split(",");
            if (parts.length < 2) {
                out.println("Error: Invalid DELETE_MAIL command format.");
                return;
            }

            String userEmail = parts[0];
            int emailId;

            try {
                emailId = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                out.println("Error: Invalid email ID.");
                return;
            }

            User user = users.get(userEmail);
            if (user == null) {
                out.println("Error: User not found.");
                return;
            }

            synchronized (user.getMailBox()) {
                List<Email> emails = user.getMailBox().getEmails();
                Email emailToDelete = null;

                for (Email email : emails) {
                    if (email.getId() == emailId) {
                        emailToDelete = email;
                        break;
                    }
                }

                if (emailToDelete != null) {
                    emails.remove(emailToDelete);
                    try {
                        updateEmailFile(userEmail, emails); // Met à jour `data.txt`
                        out.println("Mail deleted successfully.");
                    } catch (IOException e) {
                        out.println("Error: Unable to update file.");
                        logMessage("Error updating file after deleting mail: " + e.getMessage());
                    }
                } else {
                    out.println("Error: Mail not found.");
                }
            }
        }

        private synchronized void updateEmailFile(String userEmail, List<Email> emails) throws IOException {
            File file = getWritableDataFile();
            List<String> lines = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith(userEmail + ";")) {
                        StringBuilder newLine = new StringBuilder(userEmail + ";");
                        for (Email email : emails) {
                            newLine.append(serializeEmail(email)).append("|");
                        }
                        lines.add(newLine.toString());
                    } else {
                        lines.add(line);
                    }
                }
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
                for (String updatedLine : lines) {
                    writer.write(updatedLine);
                    writer.newLine();
                }
            }
        }


        private void handleRetrieveMails(String command, PrintWriter out) {
            String userEmail = command.replace("RETRIEVE_MAILS:", "").trim();

            User user = users.computeIfAbsent(userEmail, User::new);

            synchronized (user.getMailBox()) {
                if (user.getMailBox().getEmails().isEmpty()) {
                    out.println("Mail:"); // Aucun email
                } else {
                    for (Email email : user.getMailBox().getEmails()) {
                        out.println("Mail:" + serializeEmail(email));
                    }
                }
            }

            out.println("END_OF_MAILS"); // Fin de l'envoi
        }


        private void handleRegisterUser(String command, PrintWriter out) {
            String userEmail = command.replace("REGISTER_USER:", "").trim();
            if (!users.containsKey(userEmail)) {
                User newUser = new User(userEmail);
                users.put(userEmail, newUser);

                try {
                    // Ajouter l'utilisateur dans le fichier
                    addEmailToFile(userEmail, null);
                    out.println("User registered successfully.");
                } catch (IOException e) {
                    logMessage("Error adding user to file: " + e.getMessage());
                    out.println("Error registering user.");
                }
            } else {
                out.println("User already exists.");
            }
        }

    }

    private void logMessage(String message) {
        if (logArea != null) {
            Platform.runLater(() -> logArea.appendText(message + "\n"));
        }
    }
}
