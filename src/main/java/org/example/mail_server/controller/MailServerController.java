package org.example.mail_server.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.example.mail_server.model.Email;
import org.example.mail_server.model.MailBox;
import org.example.mail_server.model.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Vector;

public class MailServerController {
    private User user;
    private MailBox mailBox;


    @FXML
    public void initialize() {
        user = new User("mathis.pipart@edu.unito.it");
    }

    public void startServer() {
        System.out.println("Finestra del socket server");
        try (ServerSocket serverSocket = new ServerSocket(8189)) {
            Socket incoming = serverSocket.accept();
            System.out.println("Client connected");

            try (ObjectInputStream inStream = new ObjectInputStream(incoming.getInputStream());
                 OutputStream outStream = incoming.getOutputStream();
                 PrintWriter out = new PrintWriter(outStream, true)) {

                out.println("Hello! Waiting for a list of emails.");

                // Réception des emails
                Vector<String> emailList = null;
                try {
                    emailList = (Vector<String>) inStream.readObject();
                } catch (ClassNotFoundException e) {
                    System.out.println("Error reading object: " + e.getMessage());
                }

                if (emailList != null) {
                    System.out.println("Received email list:");
                    for (String email : emailList) {
                        System.out.println("Email: " + email);
                        out.println("Echo: " + email); // Répondre au client avec chaque email
                    }
                } else {
                    out.println("No emails received.");
                }
            } finally {
                incoming.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addMail(){
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
    }
}