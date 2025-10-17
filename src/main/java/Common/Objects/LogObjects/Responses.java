package Common.Objects.LogObjects;

public class Responses {
    //VARIABLES
    private String messageType;
    private String[] receiver;
    private String message;

    //CONSTRUCTORS
    public Responses(String messageType, String[] receiver, String message) {
        this.messageType = messageType;
        this.receiver = receiver;
        this.message = message;
    }

    //SETTERS
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
    public void setReceiver(String[] receiver) {
        this.receiver = receiver;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    //GETTERS
    public String getMessageType() {
        return messageType;
    }
    public String[] getReceiver() {
        return receiver;
    }
    public String getMessage() {
        return message;
    }
}
