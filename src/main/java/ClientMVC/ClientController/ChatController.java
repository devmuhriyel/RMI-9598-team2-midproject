package ClientMVC.ClientController;

import ClientMVC.ClientModel.UnifiedClientModel;
import ClientMVC.ClientView.Chat.ChatView;
import ClientMVC.ClientView.Chat.ChatJFXController;
import ClientMVC.ClientView.Home.HomeView;
import ClientMVC.UserXMLHandler;

import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.List;

public class ChatController {
    private UnifiedClientModel model;
    private ChatView view;
    private ChatJFXController chatJFXController;
    private String currentChatPartner;
    private boolean isGroupChat = false;
    private boolean isCommunityChat = false;

    public ChatController(UnifiedClientModel model, ChatView view, ChatJFXController uiElements) {
        this.model = model;
        this.view = view;
        this.chatJFXController = uiElements;

        setupListeners();

        // Set up chat history listener for real-time updates
        model.setChatHistoryListener(message -> {
            if (message != null && !message.trim().isEmpty()) {
                System.out.println("ChatController received message: " + message);

                javafx.application.Platform.runLater(() -> {
                    if (message.equals("MESSAGE_RECEIVED")) {
                        // Refresh the entire chat history
                        refreshChatHistory();
                    } else {
                        // Handle individual message updates
                        handleIncomingMessage(message);
                    }
                });
            }
        });

        // Set up community message listener
        model.setCommunityMessageListener(message -> {
            if (message != null && !message.trim().isEmpty() && isCommunityChat) {
                javafx.application.Platform.runLater(() -> {
                    String[] parts = message.split(": ", 2);
                    if (parts.length == 2) {
                        String sender = parts[0];
                        String msg = parts[1];
                        chatJFXController.addChatMessage(sender, msg, model.getUsername());
                    }
                });
            }
        });
    }

    private void handleIncomingMessage(String message) {
        String[] parts = message.split(": ", 2);
        if (parts.length == 2) {
            String sender = parts[0];
            String msg = parts[1];

            if (shouldShowMessage(sender)) {
                // Only add if it's not from current user AND we haven't just sent it
                if (!sender.equals(model.getUsername())) {
                    System.out.println("Adding incoming message from: " + sender + " - " + msg);
                    chatJFXController.addChatMessage(sender, msg, model.getUsername());
                }
            }
        }
    }

    private boolean shouldShowMessage(String sender) {
        if (currentChatPartner == null) return false;

        if (isGroupChat) {
            return true; // Show all group messages
        } else if (isCommunityChat) {
            return true; // Show all community messages
        } else {
            return sender.equals(currentChatPartner) || sender.equals(model.getUsername());
        }
    }

    private void setupListeners(){
        chatJFXController.setSendButtonListener(() -> sendMSG());
        chatJFXController.setHomeButtonListener(() -> goToHome());
    }

    private void goToHome() {
        currentChatPartner = null;
        isGroupChat = false;
        isCommunityChat = false;

        Platform.runLater(() -> {
            try {
                // Close current chat window
                Stage currentStage = (Stage) chatJFXController.getHomeButton().getScene().getWindow();
                currentStage.close();

                // Open Home view wired to the same model so the user can pick another chat
                HomeView homeView = new HomeView();
                Stage homeStage = new Stage();
                homeView.start(homeStage);

                // Initialize controller with existing model to populate friends/groups
                new HomeController(model, homeView, homeView.getFXController());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void sendMSG(){
        String textToSend = chatJFXController.getMessageField().getText().trim();
        String receiver = chatJFXController.getReceiver().getText();

        if (!textToSend.isEmpty() && receiver != null && !receiver.isEmpty()) {

            // Send message through unified model first
            if (receiver.equals("Community Chat")) {
                // DON'T add community messages to UI immediately - let the broadcast handle it
                model.sendCommunityMessage(textToSend);
            } else if (receiver.startsWith("Group: ")) {
                String groupName = receiver.substring(7);
                // Add group message to UI immediately, then send
                chatJFXController.addChatMessage(model.getUsername(), textToSend, model.getUsername());
                model.sendGroupMessage(textToSend, groupName);
            } else {
                // Add private message to UI immediately, then send
                chatJFXController.addChatMessage(model.getUsername(), textToSend, model.getUsername());
                model.sendMessage(textToSend, receiver);
            }

            chatJFXController.getMessageField().clear();
        }
    }

    public void refreshChatHistory() {
        if (currentChatPartner != null && !isCommunityChat) {
            String currentUser = model.getUsername();
            System.out.println("Refreshing chat history for: " + currentChatPartner);

            if (isGroupChat) {
                loadGroupChatHistory(currentUser, currentChatPartner);
            } else {
                loadChatHistory(currentUser, currentChatPartner);
            }
        }
    }

    public void loadChatHistory(String currentUser, String friend) {
        this.currentChatPartner = friend;
        this.isGroupChat = false;
        this.isCommunityChat = false;

        UserXMLHandler xmlHandler = new UserXMLHandler();
        List<UserXMLHandler.ChatMessage> history = xmlHandler.getChatHistoryWithSenders(currentUser, friend);

        chatJFXController.clearMessages();

        System.out.println("Loading " + history.size() + " messages for chat with " + friend);
        for (UserXMLHandler.ChatMessage chatMsg : history) {
            chatJFXController.addChatMessage(chatMsg.getSender(), chatMsg.getMessage(), currentUser);
        }
    }

    public void loadGroupChatHistory(String currentUser, String groupName) {
        this.currentChatPartner = groupName;
        this.isGroupChat = true;
        this.isCommunityChat = false;

        UserXMLHandler xmlHandler = new UserXMLHandler();
        List<UserXMLHandler.ChatMessage> history = xmlHandler.getGroupChatHistoryWithSenders(currentUser, groupName);

        chatJFXController.clearMessages();

        System.out.println("Loading " + history.size() + " messages for group chat " + groupName);
        for (UserXMLHandler.ChatMessage chatMsg : history) {
            chatJFXController.addChatMessage(chatMsg.getSender(), chatMsg.getMessage(), currentUser);
        }
    }

    public void loadCommunityChatHistory(String currentUser) {
        this.currentChatPartner = "Community Chat";
        this.isGroupChat = false;
        this.isCommunityChat = true;

        chatJFXController.clearMessages();

        chatJFXController.addChatMessage("System", "Welcome to Community Chat! Chat with the community", currentUser);

        System.out.println("Community chat loaded - no history available");
    }

    public String getCurrentChatPartner() {
        return currentChatPartner;
    }

    public boolean isGroupChat() {
        return isGroupChat;
    }

    public boolean isCommunityChat() {
        return isCommunityChat;
    }
}