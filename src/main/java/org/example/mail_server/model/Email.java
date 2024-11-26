package org.example.mail_server.model;

import java.time.LocalDateTime;
import java.util.List;

public class Email {
    private String id;
    private String sender;
    private List<String> receiver;
    private String subject;
    private String content;
    private LocalDateTime timestamp;

    public Email(String id, String sender, List<String>  receiver, String subject, String content, LocalDateTime timestamp) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.subject = subject;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public String getSender() {
        return sender;
    }

    public List<String>  getReceiver() {
        return receiver;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}


