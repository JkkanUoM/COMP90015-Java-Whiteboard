package client;

import java.io.Serializable;

public class Chat implements Serializable {
    private String recipient;
    private String message;

    public Chat(String message) {
        this.message = message;
    }

    public Chat(String recipient, String message) {
        this.recipient = recipient;
        this.message = message;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
