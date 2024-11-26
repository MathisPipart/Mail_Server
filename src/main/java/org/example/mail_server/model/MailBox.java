package org.example.mail_server.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class MailBox {
    private final ObservableList<Email> emails;

    public MailBox() {
        // Notify GUI on change
        this.emails = FXCollections.observableArrayList();
    }

    public ObservableList<Email> getEmails() {
        return emails;
    }

    public void addEmail(Email email) {
        emails.add(email);
    }

    public void deleteEmail(Email email) {
        emails.remove(email);
    }
}


