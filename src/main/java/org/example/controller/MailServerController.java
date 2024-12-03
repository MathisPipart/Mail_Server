package org.example.controller;
import javafx.fxml.FXML;
import org.example.model.Email;
import org.example.model.MailBox;
import org.example.model.User;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MailServerController {
    private User user;
    private MailBox mailBox;
    private volatile boolean running = true;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10); // Pool avec 10 threads

    @FXML
    public void initialize() {
        user = new User("mathis.pipart@edu.unito.it");
        mailBox = new MailBox();
        //addMail();
    }

    public void startServer() {
        System.out.println("Starting the server...");
        try (ServerSocket serverSocket = new ServerSocket(8189)) {
            System.out.println("Server started, waiting for clients...");
            while (running) {
                try {
                    // Accepter les connexions entrantes
                    Socket incoming = serverSocket.accept();
                    System.out.println("Client connected.");

                    // Confier la gestion de chaque client au threadPool
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

    public void stopServer() {
        running = false;
        System.out.println("Stopping the server...");
        threadPool.shutdown(); // Arrête le pool de threads
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

                // Lire l'objet envoyé par le client
                Object receivedObject = inStream.readObject();
                if (receivedObject instanceof Email) {
                    Email email = (Email) receivedObject;

                    // Traiter l'email reçu
                    System.out.println("Received email:");
                    System.out.println("From: " + email.getSender());
                    System.out.println("To: " + email.getReceiver());
                    System.out.println("Subject: " + email.getSubject());
                    System.out.println("Content: " + email.getContent());
                    System.out.println("Timestamp: " + email.getTimestamp());

                    // Ajouter l'email à la boîte mail
                    synchronized (mailBox) {
                        mailBox.addEmail(email);
                    }

                    // Répondre au client
                    out.println("Mail received successfully.");
                } else {
                    out.println("Invalid object received.");
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Client disconnected.");
            }
        }
    }

    /*private void addMail() {
        mailBox.addEmail(new Email("1", "mathis.pipart@gmail.com", List.of("example1@mail.com"), "Sujet 1", "Contenu 1", LocalDateTime.now()));
        mailBox.addEmail(new Email("2", "mathis.pipart@free.fr", Arrays.asList("example2@mail.com", "exampleA@mail.com", "example7@mail.com", "exampleE@mail.com"), "Sujet 2", "Contenu 2", LocalDateTime.now()));
        mailBox.addEmail(new Email("3", "paul.zerial@edu.esiee.fr", Arrays.asList("example3@mail.com", "exampleB@mail.com", "exampleC@mail.com"), "Sujet 3", "Contenu 3", LocalDateTime.now()));
        mailBox.addEmail(new Email("4", "alice.durand@gmail.com", List.of("example4@mail.com"), "Sujet 4", "Contenu 4", LocalDateTime.now()));
        mailBox.addEmail(new Email("5", "julien.martin@orange.fr", Arrays.asList("example5@mail.com", "exampleD@mail.com"), "Sujet 5", "Contenu 5", LocalDateTime.now()));
        mailBox.addEmail(new Email("6", "emma.lefevre@hotmail.com", List.of("example6@mail.com"), "Sujet 6", "Contenu 6", LocalDateTime.now()));
        mailBox.addEmail(new Email("7", "lucas.bernard@edu.univ.fr", Arrays.asList("example7@mail.com", "exampleE@mail.com"), "Sujet 7", "Contenu 7", LocalDateTime.now()));
        mailBox.addEmail(new Email("8", "charlotte.dubois@gmail.com", Arrays.asList("example8@mail.com", "exampleF@mail.com", "exampleG@mail.com"), "Sujet 8", "Contenu 8", LocalDateTime.now()));
        mailBox.addEmail(new Email("9", "nicolas.perrin@yahoo.fr", List.of("example9@mail.com"), "Sujet 9", "Contenu 9", LocalDateTime.now()));
        mailBox.addEmail(new Email("10", "lea.moreau@laposte.net", List.of("example10@mail.com"), "Sujet 10", "Contenu 10", LocalDateTime.now()));
        mailBox.addEmail(new Email("11", "marie.dupont@gmail.com", Arrays.asList("example11@mail.com", "exampleH@mail.com"), "Sujet 11", "Contenu 11", LocalDateTime.now()));
        mailBox.addEmail(new Email("12", "quentin.leroy@hotmail.fr", Arrays.asList("example12@mail.com", "exampleI@mail.com"), "Sujet 12", "Contenu 12", LocalDateTime.now()));
        mailBox.addEmail(new Email("13", "sophie.giraud@edu.univ.fr", List.of("example13@mail.com"), "Sujet 13", "Contenu 13", LocalDateTime.now()));
        mailBox.addEmail(new Email("14", "antoine.roche@orange.fr", Arrays.asList("example14@mail.com", "exampleJ@mail.com"), "Sujet 14", "Contenu 14", LocalDateTime.now()));
        mailBox.addEmail(new Email("15", "claire.benoit@yahoo.com", Arrays.asList("example15@mail.com", "exampleK@mail.com", "exampleL@mail.com"), "Sujet 15", "Contenu 15", LocalDateTime.now()));
        user.setMailBox(mailBox);
    }*/
}