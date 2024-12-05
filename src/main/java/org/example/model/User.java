package org.example.model;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L; // Recommandé pour la compatibilité

    private String email;
    private MailBox mailBox;

    public User(String email) {
        this.email = email;
        this.mailBox = new MailBox();
    }

    public String getEmail() {
        return email;
    }

    public MailBox getMailBox() {
        return mailBox;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setMailBox(MailBox mailBox) {
        this.mailBox = mailBox;
    }
}
