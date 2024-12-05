package org.example.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MailBox implements Serializable {
    private static final long serialVersionUID = 1L;

    // ObservableList utilisée pour l'interface utilisateur
    private transient ObservableList<Email> emails;

    public MailBox() {
        // Initialise l'ObservableList pour la vue
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

    // Méthode pour sérialiser l'objet
    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        // Convertit l'ObservableList en ArrayList pour la sérialisation
        oos.writeObject(new ArrayList<>(emails));
    }

    // Méthode pour désérialiser l'objet
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        // Reconstruit l'ObservableList à partir de la liste désérialisée
        List<Email> emailList = (List<Email>) ois.readObject();
        this.emails = FXCollections.observableArrayList(emailList);
    }
}
