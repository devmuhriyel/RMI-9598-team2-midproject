package Common.Objects.ClientObjects;

import java.util.List;

public class Conversation {
    //VARIABLES
    private List<Message> messages;
    private String recipient;

    //CONSTRUCTORS
    public Conversation(List<Message> messages, String recipient) {
        this.messages = messages;
        this.recipient = recipient;
    }

    //SETTERS
    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    //GETTERS
    public List<Message> getMessages() {
        return messages;
    }
    public String getRecipient() {
        return recipient;
    }
}
