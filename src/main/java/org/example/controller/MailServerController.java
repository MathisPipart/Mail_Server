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
    private static final File USERS_FILE = new File("data/users.txt");
    private static final File EMAILS_FILE = new File("data/emails.txt");

    private final Map<String, User> users = new HashMap<>();
    private int emailCount = 0;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10);
    private volatile boolean running = true;
    private final Map<Socket, User> activeUsers  = new HashMap<>();


    @FXML
    private TextArea logArea;

    public void startServer() {
        logMessage("Starting the server...");
        loadUsersFromFile();
        loadEmailsFromFile();

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
        if (!USERS_FILE.exists()) {
            logMessage("User file does not exist. Starting fresh.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                User user = new User(line);

                users.put(line, user);
            }
        } catch (IOException e) {
            logMessage("Error reading users.txt: " + e.getMessage());
        }
    }


    private void loadEmailsFromFile() {
        if (!EMAILS_FILE.exists()) {
            logMessage("Emails file does not exist. Starting fresh.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(EMAILS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;


                String[] emailParts = line.split(";");
                if (emailParts.length != 6) {
                    logMessage("Error processing email line : " + line);
                }

                Email emailObj = new Email(
                    Integer.parseInt(emailParts[0]),
                    emailParts[1],
                    Arrays.asList(emailParts[2].split("\\|")),
                    emailParts[3],
                    emailParts[4].replace("\\n", "\n"),
                    LocalDateTime.parse(emailParts[5])
                );

                // For each receiver in a email, add to their list
                emailObj.getReceiver().forEach(receiver -> {
                    final User user = this.users.get(receiver);
                    user.getMailBox().addEmail(emailObj);

                    // increment email count
                    emailCount = Math.max(emailCount, emailObj.getId()) + 1;
                });
            }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


    private synchronized void addEmailToFile(Email email) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(EMAILS_FILE, true))) {
            writer.write(email.toString());
            writer.newLine();
        }
    }


    private void logMessage(String message) {
        if (logArea != null) {
            Platform.runLater(() -> logArea.appendText(message + "\n"));
        }
    }


    public void stopServer() {
        running = false;
        logMessage("Stopping the server...");
        threadPool.shutdown();
    }



    private class ClientHandler implements Runnable {
        private final Socket socket;
        private User userConnected;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                out.println("Welcome to the Mail Server!");

                label:
                while (!socket.isClosed()) {
                    Object receivedObject = inStream.readObject();

                    switch (receivedObject) {
                        case String disconnectText -> {
                            if (disconnectText.equals("DISCONNECT")) {
                                handleDisconnect();
                                break label;
                            } else {
                                handleCommand((String) receivedObject, out);
                            }
                        }
                        case Email email -> handleEmail(email, out);
                        case User user -> handleUser(user, out);
                        case null, default -> {
                            out.println("Invalid object received.");
                            logMessage("Invalid object received: " + receivedObject);
                        }
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
                    out.println("Error: User does not exist.");
                    logMessage("Connection attempt with non-existent user: " + email);
                } else {
                    this.userConnected = users.get(email);

                    // Register user as logged in
                    synchronized (activeUsers) {
                        activeUsers.put(socket, user);
                    }
                    out.println("User connected successfully.");
                    logMessage("User connected: " + email);
                }
            }
        }


        private void handleEmail(Email email, PrintWriter out) {
            // Generate a unique ID for this email
            email.setId(emailCount);
            emailCount++;

            boolean success = true;

            for (String receiver : email.getReceiver()) {
                User user = users.computeIfAbsent(receiver, User::new);
                user.getMailBox().addEmail(email);

                try {
                    addEmailToFile(email);
                    logMessage(receiver + " received this email : " + email.getSubject());
                } catch (Exception e) {
                    logMessage("Error adding email to file: " + e.getMessage());
                    success = false;
                }
            }
            // Inform the client
            if (success) {
                out.println("Mail received successfully with ID: " + email.getId());
            } else {
                out.println("Error processing email.");
            }
        }


        private void handleCommand(String command, PrintWriter out) {
            if (command.startsWith("DISCONNECT")) {
                handleDisconnect();
            } else if (command.startsWith("RETRIEVE_MAILS:")) {
                handleRetrieveMails(command, out);
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
                    logMessage("User exists: " + userEmail);
                } else {
                    out.println("Error: User does not exist.");
                    logMessage("User does not exist: " + userEmail);
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
                        removeEmailFromFile(emailToDelete);
                        out.println("Mail deleted successfully.");
                        logMessage(userEmail + " deleted this email : "+ emailToDelete.getSubject());
                    } catch (IOException e) {
                        out.println("Error: Unable to update file.");
                        logMessage("Error updating file after deleting mail: " + e.getMessage());
                    }
                } else {
                    out.println("Error: Mail not found.");
                }
            }
        }


        private synchronized void removeEmailFromFile(Email emailToDelete) throws IOException {
            File emailsFile = EMAILS_FILE;
            File tempFile = new File(emailsFile.getAbsolutePath() + ".tmp");

            try (BufferedReader reader = new BufferedReader(new FileReader(emailsFile));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue; // Skip empty lines
                    }

                    // Extract the email ID (the first field before the first semicolon)
                    String[] emailParts = line.split(";");
                    if (emailParts.length == 0) {
                        logMessage("Malformed line in emails file: " + line);
                        continue; // Skip malformed lines
                    }

                    int emailIdInFile = Integer.parseInt(emailParts[0]);

                    // Skip writing the email if its ID matches the email to delete
                    if (emailIdInFile == emailToDelete.getId()) {
                        logMessage("Deleted email from file: " + line);
                        continue;
                    }

                    // Write the email back to the temp file
                    writer.write(line);
                    writer.newLine();
                }
            }

            // Replace the original file with the updated temp file
            if (!emailsFile.delete()) {
                throw new IOException("Failed to delete the original emails file.");
            }
            if (!tempFile.renameTo(emailsFile)) {
                throw new IOException("Failed to rename the temp file to emails file.");
            }
        }


        private void handleRetrieveMails(String command, PrintWriter out) {
            String userEmail = command.replace("RETRIEVE_MAILS:", "").trim();

            User user = users.get(userEmail);
            if (user == null) {
                out.println("Error: User not found.");
                return;
            }

            synchronized (user.getMailBox()) {
                if (user.getMailBox().getEmails().isEmpty()) {
                    out.println("No emails.txt found.");
                } else {
                    for (Email email : user.getMailBox().getEmails().sorted((m1, m2) -> m2.getTimestamp().compareTo(m1.getTimestamp()))) {
                        String serializedEmail = email.toString();
                        out.println("Mail:" + serializedEmail);
                    }
                }
            }

            out.println("END_OF_MAILS");
        }


        private void handleDisconnect() {
            try {
                if (userConnected != null) {
                    synchronized (activeUsers ) {
                        activeUsers .remove(socket);
                    }
                    logMessage("User disconnected: " + userConnected.getEmail());
                } else {
                    logMessage("Unknown client disconnected.");
                }

                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                logMessage("Error while disconnecting client: " + e.getMessage());
            }
        }

    }
}
