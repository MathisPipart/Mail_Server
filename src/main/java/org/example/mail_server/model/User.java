package org.example.mail_server.model;

public class User {
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

