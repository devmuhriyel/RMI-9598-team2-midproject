package Server.ServerClasses.ClientObjects;

import java.util.List;

public class ChatHistory {
    //VARIABLES
    private List<Message> message;

    //CONSTRUCTORS
    public ChatHistory(List<Message> mssgs) {
        this.message = mssgs;
    }

    //SETTERS
    public void setMessages(List<Message> mssgs){
        this.message = mssgs;
    }

    //GETTERS
    public List<Message> getMessages(){
        return this.message;
    }
}
