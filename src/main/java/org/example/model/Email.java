package org.example.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class Email implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String sender;
    private List<String> receiver;
    private String subject;
    private String content;
    private LocalDateTime timestamp;

    public Email(int id, String sender, List<String>  receiver, String subject, String content, LocalDateTime timestamp) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.subject = subject;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public int getId() {
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

    public void setId(int id) {
        this.id = id;
    }
}


