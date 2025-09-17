package ClientMVC.ClientController;

import ClientMVC.ClientModel.ChatModel;
import ClientMVC.ClientView.Chat.ChatView;
import ClientMVC.ClientView.Chat.ChatJFXController;
import ClientMVC.UserXMLHandler;

import java.util.List;

public class ChatController {
    private ChatModel mdl;
    private ChatView vw;
    private ChatJFXController chatJFXController;

    public ChatController(ChatModel model, ChatView view, ChatJFXController uiElements) {
        this.mdl = model;
        this.vw = view;
        this.chatJFXController = uiElements;

        setupListeners();
        mdl.setMessageListener(msg -> {
            javafx.application.Platform.runLater(() -> {
                // For incoming messages, assume they're from the friend (not sender)
                chatJFXController.addMessage(msg, false);
            });
        });
    }

    private void setupListeners(){
        chatJFXController.setSendButtonListener(() -> sendMSG());
        chatJFXController.setHomeButtonListener(() -> goToHome());
    }

    private void goToHome() {
        vw.goToHome();
    }

    // Replace the sendMSG() method in ChatController.java
    private void sendMSG(){
        String textToSend = chatJFXController.getMessageField().getText();
        String receiver = chatJFXController.getReceiver().getText();
        if (!textToSend.isEmpty()) {
            // Add message as sender (true)
            chatJFXController.addMessage(textToSend, true);

            // Check if this is a group chat (starts with "Group: ")
            if (receiver.startsWith("Group: ")) {
                String groupName = receiver.substring(7); // Remove "Group: " prefix
                mdl.sendGroupMessage(textToSend, groupName);
            } else {
                // Regular private message
                mdl.sendMSG(textToSend, receiver);
            }

            chatJFXController.getMessageField().clear();
        }
    }


    public void loadChatHistory(String currentUser, String friend) {
        UserXMLHandler xmlHandler = new UserXMLHandler();
        List<UserXMLHandler.ChatMessage> history = xmlHandler.getChatHistoryWithSenders(currentUser, friend);

        chatJFXController.clearMessages();
        for (UserXMLHandler.ChatMessage chatMsg : history) {
            boolean isSender = chatMsg.getSender().equals(currentUser);
            chatJFXController.addChatMessage(chatMsg.getSender(), chatMsg.getMessage(), currentUser);
        }
    }

    public void loadGroupChatHistory(String currentUser, String groupName) {
        UserXMLHandler xmlHandler = new UserXMLHandler();
        List<UserXMLHandler.ChatMessage> history = xmlHandler.getGroupChatHistoryWithSenders(currentUser, groupName);

        chatJFXController.clearMessages();
        for (UserXMLHandler.ChatMessage chatMsg : history) {
            boolean isSender = chatMsg.getSender().equals(currentUser);
            chatJFXController.addChatMessage(chatMsg.getSender(), chatMsg.getMessage(), currentUser);
        }
    }

    // You might also want to modify the sendMessage method to handle groups
    public void sendGroupMessage(String message, String groupName) {
        if (mdl != null) {
            mdl.sendGroupMessage(message, groupName);
        }
    }

    // Inner class to hold chat message data
    public static class ChatMessage {
        private final String sender;
        private final String message;

        public ChatMessage(String sender, String message) {
            this.sender = sender;
            this.message = message;
        }

        public String getSender() {
            return sender;
        }

        public String getMessage() {
            return message;
        }
    }
}