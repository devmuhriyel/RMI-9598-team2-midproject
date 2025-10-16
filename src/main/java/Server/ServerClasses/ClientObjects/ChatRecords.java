package Server.ServerClasses.ClientObjects;

import java.util.List;

public class ChatRecords {
    //VARIABLES
    private List<Conversation> conversation;

    //CONSTRUCTORS
    public ChatRecords(List<Conversation> conversationList) {
        this.conversation = conversationList;
    }

    //SETTERS
    public void setMessages(List<Conversation> mssgs){
        this.conversation = mssgs;
    }

    //GETTERS
    public List<Conversation> getMessages(){
        return this.conversation;
    }
}
