package Server.ServerClasses.LogObjects;

public class Requests {
    //VARIABLES
    private String messageType;
    private String sender;
    private String message;

    //CONSTRUCTORS
    public Requests(String messageType, String sender, String message) {
        this.messageType = messageType;
        this.sender = sender;
        this.message = message;
    }

    //SETTERS
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
    public void setSender(String sender) {
        this.sender = sender;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    //GETTERS
    public String getMessageType() {
        return messageType;
    }
    public String getSender() {
        return sender;
    }
    public String getMessage() {
        return message;
    }
}
