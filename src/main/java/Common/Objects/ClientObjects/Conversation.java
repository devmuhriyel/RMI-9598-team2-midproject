package Common.Objects.ClientObjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Conversation implements Serializable {
    private String recipient;
    private List<Message> messages;

    public Conversation() {
        this.messages = new ArrayList<>();
    }

    public Conversation(String recipient, List<Message> messages) {
        this.recipient = recipient;
        this.messages = messages != null ? messages : new ArrayList<>();
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}