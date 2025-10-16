package Server.ServerClasses;

public class Message {
    //VARIABLES
    private String sender;
    private String receiver;
    private String content;

    //CONSTRUCTORS
    public Message(String sndr, String rcvr, String cntnt) {
        this.sender = sndr;
        this.receiver = rcvr;
        this.content = cntnt;
    }

    //SETTERS
    public void setSender(String sndr) {
        this.sender = sndr;
    }
    public void setReceiver(String rcvr) {
        this.receiver = rcvr;
    }
    public void setContent(String cntnt) {
        this.content = cntnt;
    }

    //GETTERS
    public String getSender() {
        return sender;
    }
    public String getReceiver() {
        return receiver;
    }
    public String getContent() {
        return content;
    }
}
