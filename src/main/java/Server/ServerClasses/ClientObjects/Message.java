package Server.ServerClasses.ClientObjects;

public class Message {
    //VARIABLES
    private String sender;
    private String receiver;
    private String message;

    //CONSTRUCTORS
    public Message(String sndr, String rcvr, String msg) {
        this.sender = sndr;
        this.receiver = rcvr;
        this.message = msg;
    }

    //SETTERS
    public void setSender(String sndr) {
        this.sender = sndr;
    }
    public void setReceiver(String rcvr) {
        this.receiver = rcvr;
    }
    public void setContent(String msg) {
        this.message = msg;
    }

    //GETTERS
    public String getSender() {
        return sender;
    }
    public String getReceiver() {
        return receiver;
    }
    public String getContent() {
        return message;
    }
}
