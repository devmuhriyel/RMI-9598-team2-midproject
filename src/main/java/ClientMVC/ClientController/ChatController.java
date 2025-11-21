package ClientMVC.ClientController;

import ClientMVC.ClientCallbackImplementation;
import ClientMVC.ClientModel.UnifiedClientModel;
import ClientMVC.ClientView.Chat.ChatView;
import ClientMVC.ClientView.Chat.ChatJFXController;
import Common.Objects.ClientObjects.Client;
import Common.Objects.ClientObjects.Conversation;
import Common.Objects.ClientObjects.GroupChat;
import Common.Objects.ClientObjects.Message;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.File;
import java.rmi.RemoteException;
import java.util.List;

public class ChatController {
    private UnifiedClientModel model;
    private ChatView view;
    private ChatJFXController chatJFXController;
    private ClientCallbackImplementation clientCallback;
    private String currentChatPartner;
    private String currentGroupID;
    private boolean isGroupChat = false;
    private boolean isCommunityChat = false;

    public ChatController(UnifiedClientModel model, ChatView view, ChatJFXController uiElements, ClientCallbackImplementation callback) {
        this.model = model;
        this.view = view;
        this.chatJFXController = uiElements;
        this.clientCallback = callback;
        setupListeners();
    }

    private void setupListeners() {
        chatJFXController.setSendButtonListener(() -> {
            try {
                if (isCommunityChat) {
                    sendCommunityMessage();
                } else if (isGroupChat) {
                    sendGroupMessage();
                } else {
                    sendPrivateMessage();
                }
            } catch (RemoteException e) {
                System.err.println("Error sending message: " + e.getMessage());
                e.printStackTrace();
            }
        });

        chatJFXController.setHomeButtonListener(() -> {
            if (isCommunityChat) {
                try {
                    model.leaveCommunityChat(model.getUsername());
                } catch (RemoteException e) {
                    System.err.println("Failed to leave community chat: " + e.getMessage());
                }
            }
        });

        chatJFXController.setHomeButtonListener(() -> {
            handleHomeButton();
        });

        chatJFXController.setFileButtonListener(() -> {
            try {
                if (isCommunityChat) {
                    sendCommunityFile();
                } else if (isGroupChat) {
                    sendGroupFile();
                } else {
                    sendPrivateFile();
                }
            } catch (RemoteException e) {
                System.err.println("Error sending file: " + e.getMessage());
                e.printStackTrace();
            }
        });

        // Set file open listener - delegates to controller for data retrieval
        chatJFXController.setFileOpenListener(fileName -> {
            handleFileOpen(fileName);
        });
    }

    private void handleHomeButton() {
        // Leave community chat if active
        if (isCommunityChat) {
            try {
                model.leaveCommunityChat(model.getUsername());
            } catch (RemoteException e) {
                System.err.println("Failed to leave community chat: " + e.getMessage());
            }
        }

        chatJFXController.closeWindow();
    }

    private void handleFileOpen(String fileName) {
        try {
            Message fileMessage = findFileMessage(fileName);

            if (fileMessage == null) {
                chatJFXController.showError("File data not found. The file may have been deleted.");
                return;
            }
            chatJFXController.handleFileSave(fileName, fileMessage.getFileData(), model);

        } catch (Exception e) {
            System.err.println("Error retrieving file: " + e.getMessage());
            e.printStackTrace();
            chatJFXController.showError("Failed to retrieve file: " + e.getMessage());
        }
    }

    private Message findFileMessage(String fileName) throws RemoteException {
        System.out.println("=== Searching for file: " + fileName + " ===");

        if (isCommunityChat) {
            System.err.println("⚠ Warning: Community chat files cannot be retrieved from history");
            return null;

        } else if (isGroupChat) {
            // Search in group chat history
            System.out.println("Searching in group chat: " + currentGroupID);

            Common.Objects.ClientObjects.GroupChat groupChat = model.getGroupChatHistoryWith(
                    model.createJSONStringRequest("request", "GET_GROUP_CHAT_HISTORY", model.getUsername()),
                    model.getUsername(),
                    currentGroupID
            );

            if (groupChat != null && groupChat.getMessages() != null) {
                System.out.println("Found " + groupChat.getMessages().size() + " messages in group");
                for (Message msg : groupChat.getMessages()) {
                    if (msg.isFile() && msg.getFileName().equals(fileName)) {
                        System.out.println("✓ Found file in group chat!");
                        return msg;
                    }
                }
            }

            // ✓ ALSO CHECK LOCAL CLIENT DATA
            Client clientData = clientCallback.getClientData();
            if (clientData != null && clientData.getGroupChats() != null) {
                for (Common.Objects.ClientObjects.GroupChat gc : clientData.getGroupChats()) {
                    if (gc.getGroupID().equals(currentGroupID) && gc.getMessages() != null) {
                        System.out.println("Checking local group data: " + gc.getMessages().size() + " messages");
                        for (Message msg : gc.getMessages()) {
                            if (msg.isFile() && msg.getFileName().equals(fileName)) {
                                System.out.println("✓ Found file in LOCAL group data!");
                                return msg;
                            }
                        }
                    }
                }
            }

        } else {
            // Search in private chat history
            System.out.println("Searching in private chat with: " + currentChatPartner);

            Client clientData = clientCallback.getClientData();
            if (clientData != null && clientData.getConversations() != null) {
                for (Conversation conv : clientData.getConversations()) {
                    if (conv.getRecipient().equals(currentChatPartner)) {
                        if (conv.getMessages() != null) {
                            System.out.println("Found " + conv.getMessages().size() + " messages in conversation");
                            for (Message msg : conv.getMessages()) {
                                if (msg.isFile() && msg.getFileName().equals(fileName)) {
                                    System.out.println("✓ Found file in conversation!");
                                    return msg;
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }

        System.err.println("✗ File not found: " + fileName);
        return null;
    }

    //Private Messaging Methods
    public void sendPrivateMessage() throws RemoteException {
        String messageText = chatJFXController.getMessageField().getText();
        String receiver = chatJFXController.getReceiver().getText();
        model.sendPrivateMessage(
                model.getUsername(),
                receiver,
                messageText,
                model.createJSONStringRequest("request", "SEND_MESSAGE", model.getUsername())
        );

        Platform.runLater(() -> chatJFXController.getMessageField().clear());
    }

    public void sendPrivateFile() throws RemoteException {
        File file = chatJFXController.showFileChooser();
        if (file != null) {
            String receiver = chatJFXController.getReceiver().getText();

            if (receiver == null || receiver.trim().isEmpty()) {
                return;
            }
            model.sendPrivateFile(
                    model.getUsername(),
                    receiver,
                    file,
                    model.createJSONStringRequest("request", "SEND_FILE", model.getUsername())
            );
        }
    }

    //Group Messaging Methods
    public void sendGroupMessage() throws RemoteException {
        String messageText = chatJFXController.getMessageField().getText();
        model.sendGroupMessage(
                model.getUsername(),
                currentGroupID,
                messageText,
                model.createJSONStringRequest("request", "SEND_GROUP_MESSAGE", model.getUsername())
        );

        Platform.runLater(() -> chatJFXController.getMessageField().clear());
    }

    public void sendGroupFile() throws RemoteException {
        File file = chatJFXController.showFileChooser();
        if (file != null) {
            if (currentGroupID == null || currentGroupID.trim().isEmpty()) {
                return;
            }
            model.sendGroupFile(
                    model.getUsername(),
                    currentGroupID,
                    file,
                    model.createJSONStringRequest("request", "SEND_GROUP_FILE", model.getUsername())
            );
        }
    }

    //Community Chat Methods
    public void sendCommunityMessage() throws RemoteException {
        String messageText = chatJFXController.getMessageField().getText();

        if (messageText == null || messageText.trim().isEmpty()) {
            return;
        }
        model.sendCommunityMessage(
                model.getUsername(),
                messageText,
                model.createJSONStringRequest("request", "SEND_COMMUNITY_MESSAGE", model.getUsername())
        );

        Platform.runLater(() -> chatJFXController.getMessageField().clear());
    }

    public void sendCommunityFile() throws RemoteException {
        File file = chatJFXController.showFileChooser();
        if (file != null) {
            model.sendCommunityFile(
                    model.getUsername(),
                    file,
                    model.createJSONStringRequest("request", "SEND_COMMUNITY_FILE", model.getUsername())
            );
        }
    }

    //Private Message Callback Methods
    public void onPrivateMessageSent(Message message) throws RemoteException {
        Platform.runLater(() -> {
            chatJFXController.addChatMessage(
                    message.getSender(),
                    message.getContent(),
                    model.getUsername()
            );
        });
    }

    public void onPrivateMessageReceived(Message message) throws RemoteException {
        Platform.runLater(() -> {
            chatJFXController.addChatMessage(
                    message.getSender(),
                    message.getContent(),
                    model.getUsername()
            );
        });
    }

    public void onPrivateFileSent(Message message) throws RemoteException {
        Platform.runLater(() -> {
            chatJFXController.addChatMessage(
                    message.getSender(),
                    "[FILE: " + message.getFileName() + "]",
                    model.getUsername()
            );
        });
    }

    public void onPrivateFileReceived(Message message) throws RemoteException {
        Platform.runLater(() -> {
            chatJFXController.addChatMessage(
                    message.getSender(),
                    "[FILE: " + message.getFileName() + "]",
                    model.getUsername()
            );
        });
    }

    //Group Message Callback Methods
    public void onGroupMessageSent(Message message) throws RemoteException {
        Platform.runLater(() -> {
            chatJFXController.addChatMessage(
                    message.getSender(),
                    message.getContent(),
                    model.getUsername()
            );
        });
    }

    public void onGroupMessageReceived(Message message) throws RemoteException {
        Platform.runLater(() -> {
            chatJFXController.addChatMessage(
                    message.getSender(),
                    message.getContent(),
                    model.getUsername()
            );
        });
    }

    public void onGroupFileSent(Message message) throws RemoteException {
        Platform.runLater(() -> {
            chatJFXController.addChatMessage(
                    message.getSender(),
                    "[FILE: " + message.getFileName() + "]",
                    model.getUsername()
            );
        });
    }

    public void onGroupFileReceived(Message message) throws RemoteException {
        Platform.runLater(() -> {
            chatJFXController.addChatMessage(
                    message.getSender(),
                    "[FILE: " + message.getFileName() + "]",
                    model.getUsername()
            );
        });
    }

    //Community Message Callback Methods
    public void onCommunityMessageSent(Message message) throws RemoteException {
        Platform.runLater(() -> {
            chatJFXController.addChatMessage(
                    message.getSender(),
                    message.getContent(),
                    model.getUsername()
            );
        });
    }

    public void onCommunityMessageReceived(Message message) throws RemoteException {
        Platform.runLater(() -> {
            if (message.isFile()) {
                chatJFXController.addChatMessage(
                        message.getSender(),
                        "[FILE: " + message.getFileName() + "]",
                        model.getUsername()
                );
            } else {
                chatJFXController.addChatMessage(
                        message.getSender(),
                        message.getContent(),
                        model.getUsername()
                );
            }
        });
    }

    //History Methods
    public void loadChatHistory(String receiver) throws RemoteException {
        chatJFXController.clearMessages();
        model.getChatHistoryWith(
                model.createJSONStringRequest("request", "GET_CHAT_HISTORY", model.getUsername()),
                model.getUsername(),
                receiver
        );
    }

    public void loadGroupChatHistory(String groupID) throws RemoteException {
        chatJFXController.clearMessages();
        model.getGroupChatHistoryWith(
                model.createJSONStringRequest("request", "GET_GROUP_CHAT_HISTORY", model.getUsername()),
                model.getUsername(),
                groupID
        );
    }

    //Callback methods for history
    public void onGetChatHistories(List<Conversation> allChats) throws RemoteException {
        System.out.println("Received all chat histories: " + allChats.size());
    }

    public void onGetChatHistoryWith(Conversation chatWith) throws RemoteException {
        Platform.runLater(() -> {
            chatJFXController.clearMessages();
            if (chatWith != null && chatWith.getMessages() != null) {
                for (Message msg : chatWith.getMessages()) {
                    if (msg.isFile()) {
                        chatJFXController.addChatMessage(
                                msg.getSender(),
                                "[FILE: " + msg.getFileName() + "]",
                                model.getUsername()
                        );
                    } else {
                        chatJFXController.addChatMessage(
                                msg.getSender(),
                                msg.getContent(),
                                model.getUsername()
                        );
                    }
                }
            }
        });
    }

    public void onGetGroupChatHistories(List<GroupChat> allGroupChats) throws RemoteException {
        System.out.println("Received all group chat histories: " + allGroupChats.size());
    }

    public void onGetGroupChatHistoryWith(GroupChat groupChatWith) throws RemoteException {
        Platform.runLater(() -> {
            chatJFXController.clearMessages();
            if (groupChatWith != null && groupChatWith.getMessages() != null) {
                for (Message msg : groupChatWith.getMessages()) {
                    if (msg.isFile()) {
                        chatJFXController.addChatMessage(
                                msg.getSender(),
                                "[FILE: " + msg.getFileName() + "]",
                                model.getUsername()
                        );
                    } else {
                        chatJFXController.addChatMessage(
                                msg.getSender(),
                                msg.getContent(),
                                model.getUsername()
                        );
                    }
                }
            }
        });
    }

    //Setup Methods
    public void setCurrentChatPartner(String username) {
        this.currentChatPartner = username;
        this.isGroupChat = false;
        this.isCommunityChat = false;
        this.currentGroupID = null;
        chatJFXController.setLabel(username);
    }

    public void setCurrentGroupChat(String groupID, String groupName) {
        this.currentGroupID = groupID;
        this.isGroupChat = true;
        this.isCommunityChat = false;
        this.currentChatPartner = null;
        chatJFXController.setLabel(groupName);
    }

    public void setCurrentCommunityChat() {
        this.isCommunityChat = true;
        this.isGroupChat = false;
        this.currentChatPartner = null;
        this.currentGroupID = null;
        chatJFXController.setLabel("Community Chat");
        try {
            model.joinCommunityChat(model.getUsername(), clientCallback);
        } catch (RemoteException e) {
            System.err.println("Failed to join community chat: " + e.getMessage());
            e.printStackTrace();
        }
    }
}