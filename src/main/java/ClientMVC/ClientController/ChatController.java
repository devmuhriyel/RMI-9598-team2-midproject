package ClientMVC.ClientController;

import ClientMVC.ClientModel.UnifiedClientModel;
import ClientMVC.ClientView.Chat.ChatView;
import ClientMVC.ClientView.Chat.ChatJFXController;
import ClientMVC.ClientView.Home.HomeView;
import ClientMVC.UserXMLHandler;
import java.io.File;

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
        model.setChatHistoryListener(message -> {
            if (message != null && !message.trim().isEmpty()) {
                System.out.println("ChatController received message: " + message);

                javafx.application.Platform.runLater(() -> {
                    if (message.equals("MESSAGE_RECEIVED")) {
                        refreshChatHistory();
                    } else {
                        handleIncomingMessage(message);
                    }
                });
            }
        });
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

        model.setFileReceivedListener(fileName -> {
            Platform.runLater(() -> {
                System.out.println("File received notification: " + fileName);
                refreshChatHistory();
            });
        });
    }

    private void handleIncomingMessage(String message) {
        String[] parts = message.split(": ", 2);
        if (parts.length == 2) {
            String sender = parts[0];
            String msg = parts[1];

            if (shouldShowMessage(sender)) {
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
            return true;
        } else if (isCommunityChat) {
            return true;
        } else {
            return sender.equals(currentChatPartner) || sender.equals(model.getUsername());
        }
    }

    private void setupListeners() {
        chatJFXController.setSendButtonListener(() -> sendMSG());
        chatJFXController.setHomeButtonListener(() -> goToHome());
        chatJFXController.setFileButtonListener(() -> handleFileSelection());
        chatJFXController.setFileOpenListener(fileName -> handleFileOpen(fileName));
    }

    private void handleFileSelection() {
        Platform.runLater(() -> {
            File selectedFile = chatJFXController.showFileChooser();
            if (selectedFile != null) {
                String receiver = chatJFXController.getReceiver().getText();
                boolean success = false;

                if (receiver.equals("Community Chat")) {
                    showErrorMessage("File sharing is not available in Community Chat");
                    return;
                } else if (receiver.startsWith("Group: ")) {
                    String groupName = receiver.substring(7);
                    success = model.sendGroupFile(selectedFile.getAbsolutePath(), groupName);
                } else {
                    success = model.sendFile(selectedFile.getAbsolutePath(), receiver);
                }

                if (success) {
                    String fileMessage = "[FILE: " + selectedFile.getName() + "]";
                    chatJFXController.addChatMessage(model.getUsername(), fileMessage, model.getUsername());
                    System.out.println("File sent successfully: " + selectedFile.getName());
                } else {
                    showErrorMessage("Failed to send file. Please check file size (max 10MB) and try again.");
                }
            }
        });
    }

    private void handleFileOpen(String fileName) {
        Platform.runLater(() -> {
            boolean opened = model.openFile(fileName);

            if (!opened) {
                boolean saved = model.saveReceivedFile(fileName);
                if (saved) {
                    model.openFile(fileName);
                } else {
                    showErrorMessage("Could not open file: " + fileName);
                }
            }
        });
    }

    private void showErrorMessage(String message) {
        chatJFXController.addChatMessage("System", message, model.getUsername());
    }

    private void goToHome() {
        currentChatPartner = null;
        isGroupChat = false;
        isCommunityChat = false;

        Platform.runLater(() -> {
            try {
                Stage currentStage = (Stage) chatJFXController.getHomeButton().getScene().getWindow();
                currentStage.close();

                HomeView homeView = new HomeView();
                Stage homeStage = new Stage();
                homeView.start(homeStage);

                new HomeController(model, homeView, homeView.getFXController());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void sendMSG() {
        String textToSend = chatJFXController.getMessageField().getText().trim();
        String receiver = chatJFXController.getReceiver().getText();

        if (!textToSend.isEmpty() && receiver != null && !receiver.isEmpty()) {
            if (receiver.equals("Community Chat")) {
                model.sendCommunityMessage(textToSend);
            } else if (receiver.startsWith("Group: ")) {
                String groupName = receiver.substring(7);
                chatJFXController.addChatMessage(model.getUsername(), textToSend, model.getUsername());
                model.sendGroupMessage(textToSend, groupName);
            } else {
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