package Common.Objects.ClientObjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GroupChat implements Serializable {
    private String groupID;
    private String groupName;
    private List<String> receivers;
    private List<Message> messages;

    public GroupChat() {
        this.receivers = new ArrayList<>();
        this.messages = new ArrayList<>();
    }

    public GroupChat(String groupID, String groupName, List<String> receivers, List<Message> messages) {
        this.groupID = groupID;
        this.groupName = groupName;
        this.receivers = receivers != null ? receivers : new ArrayList<>();
        this.messages = messages != null ? messages : new ArrayList<>();
    }

    // SETTERS
    public void setGroupID(String groupID) {
        this.groupID = groupID;
    }
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    public void setReceivers(List<String> receivers) {
        this.receivers = receivers;
    }
    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    // GETTERS
    public String getGroupID() {
        return groupID;
    }
    public String getGroupName() {
        return groupName;
    }
    public List<String> getReceivers() {
        return receivers;
    }
    public List<Message> getMessages() {
        return messages;
    }

    // Helper methods
    public void addReceiver(String username) {
        if (!receivers.contains(username)) {
            receivers.add(username);
        }
    }

    public void removeReceiver(String username) {
        receivers.remove(username);
    }

    public boolean isMember(String username) {
        return receivers.contains(username);
    }
}