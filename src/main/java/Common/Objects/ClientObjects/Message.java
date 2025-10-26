package Common.Objects.ClientObjects;

import java.io.Serializable;
import java.util.List;

public class Message implements Serializable {
    private String sender;
    private String receiver;  // For private messages
    private List<String> receivers;  // For group messages
    private String content;
    private String fileData;  // Base64 encoded file data
    private String fileName;  // Original file name
    private boolean isFile;   // Flag to indicate if this is a file message

    // No-arg constructor for Gson
    public Message() {
        this.isFile = false;
    }

    // Constructor for private text message
    public Message(String sender, String receiver, String content) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.isFile = false;
    }

    // Constructor for group text message
    public Message(String sender, List<String> receivers, String content) {
        this.sender = sender;
        this.receivers = receivers;
        this.content = content;
        this.isFile = false;
    }

    // Constructor for private file message
    public Message(String sender, String receiver, String fileName, String fileData, boolean isFile) {
        this.sender = sender;
        this.receiver = receiver;
        this.fileName = fileName;
        this.fileData = fileData;
        this.isFile = isFile;
        this.content = "[FILE: " + fileName + "]";
    }

    // Constructor for group file message
    public Message(String sender, List<String> receivers, String fileName, String fileData, boolean isFile) {
        this.sender = sender;
        this.receivers = receivers;
        this.fileName = fileName;
        this.fileData = fileData;
        this.isFile = isFile;
        this.content = "[FILE: " + fileName + "]";
    }

    // Getters and setters
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }

    public List<String> getReceivers() { return receivers; }
    public void setReceivers(List<String> receivers) { this.receivers = receivers; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getFileData() { return fileData; }
    public void setFileData(String fileData) { this.fileData = fileData; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public boolean isFile() { return isFile; }
    public void setFile(boolean file) { isFile = file; }

    @Override
    public String toString() {
        return "Message{" +
                "sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", receivers=" + receivers +
                ", content='" + content + '\'' +
                ", isFile=" + isFile +
                '}';
    }
}