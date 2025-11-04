package ClientMVC.ClientController;

import ClientMVC.ClientCallbackImplementation;
import ClientMVC.ClientModel.UnifiedClientModel;
import ClientMVC.ClientView.Chat.ChatView;
import ClientMVC.ClientView.Chat.ChatJFXController;
import Common.Objects.ClientObjects.Conversation;
import Common.Objects.ClientObjects.GroupChat;
import Common.Objects.ClientObjects.Message;
import javafx.application.Platform;

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
            // When closing chat, leave community chat if active
            if (isCommunityChat) {
                try {
                    model.leaveCommunityChat(model.getUsername());
                    System.out.println("✓ Left community chat");
                } catch (RemoteException e) {
                    System.err.println("Failed to leave community chat: " + e.getMessage());
                }
            }
            // Close the chat window to return to Home
            try {
                if (chatJFXController.getHomeButton() != null) {
                    chatJFXController.getHomeButton().getScene().getWindow().hide();
                }
            } catch (Exception ignored) { }
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

        chatJFXController.setFileOpenListener(fileName -> {
            // Handle file open - implement file download/open logic
        });
    }

    //Private Messaging Methods
    public void sendPrivateMessage() throws RemoteException {
        String messageText = chatJFXController.getMessageField().getText();
        String receiver = chatJFXController.getReceiver().getText();

        if (messageText == null || messageText.trim().isEmpty()) {
            System.out.println("Cannot send empty message");
            return;
        }

        if (receiver == null || receiver.trim().isEmpty()) {
            System.out.println("No receiver selected");
            return;
        }

        System.out.println("Sending message from " + model.getUsername() + " to " + receiver + ": " + messageText);

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
                System.out.println("No receiver selected");
                return;
            }

            System.out.println("Sending file from " + model.getUsername() + " to " + receiver + ": " + file.getName());

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

        if (messageText == null || messageText.trim().isEmpty()) {
            System.out.println("Cannot send empty message");
            return;
        }

        if (currentGroupID == null || currentGroupID.trim().isEmpty()) {
            System.out.println("No group selected");
            return;
        }

        System.out.println("Sending group message from " + model.getUsername() + " to group " + currentGroupID + ": " + messageText);

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
                System.out.println("No group selected");
                return;
            }

            System.out.println("Sending file to group " + currentGroupID + ": " + file.getName());

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
            System.out.println("Cannot send empty message");
            return;
        }

        System.out.println("Sending community message from " + model.getUsername() + ": " + messageText);

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
            System.out.println("Sending file to community chat: " + file.getName());

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
        System.out.println("Message sent successfully: " + message.getContent());
    }

    public void onPrivateMessageReceived(Message message) throws RemoteException {
        Platform.runLater(() -> {
            chatJFXController.addChatMessage(
                    message.getSender(),
                    message.getContent(),
                    model.getUsername()
            );
        });
        System.out.println("Message received from " + message.getSender() + ": " + message.getContent());
    }

    public void onPrivateFileSent(Message message) throws RemoteException {
        Platform.runLater(() -> {
            chatJFXController.addChatMessage(
                    message.getSender(),
                    "[FILE: " + message.getFileName() + "]",
                    model.getUsername()
            );
        });
        System.out.println("File sent successfully: " + message.getFileName());
    }

    public void onPrivateFileReceived(Message message) throws RemoteException {
        Platform.runLater(() -> {
            chatJFXController.addChatMessage(
                    message.getSender(),
                    "[FILE: " + message.getFileName() + "]",
                    model.getUsername()
            );
        });
        System.out.println("File received from " + message.getSender() + ": " + message.getFileName());
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
        System.out.println("Group message sent successfully: " + message.getContent());
    }

    public void onGroupMessageReceived(Message message) throws RemoteException {
        Platform.runLater(() -> {
            chatJFXController.addChatMessage(
                    message.getSender(),
                    message.getContent(),
                    model.getUsername()
            );
        });
        System.out.println("Group message received from " + message.getSender() + ": " + message.getContent());
    }

    public void onGroupFileSent(Message message) throws RemoteException {
        Platform.runLater(() -> {
            chatJFXController.addChatMessage(
                    message.getSender(),
                    "[FILE: " + message.getFileName() + "]",
                    model.getUsername()
            );
        });
        System.out.println("Group file sent successfully: " + message.getFileName());
    }

    public void onGroupFileReceived(Message message) throws RemoteException {
        Platform.runLater(() -> {
            chatJFXController.addChatMessage(
                    message.getSender(),
                    "[FILE: " + message.getFileName() + "]",
                    model.getUsername()
            );
        });
        System.out.println("Group file received from " + message.getSender() + ": " + message.getFileName());
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
        System.out.println("Community message sent successfully: " + message.getContent());
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
        System.out.println("Community message received from " + message.getSender());
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
        System.out.println("Loaded chat history with " + chatWith.getRecipient());
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
        System.out.println("Loaded group chat history for " + groupChatWith.getGroupName());
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

        // Join the community chat on server
        try {
            model.joinCommunityChat(model.getUsername(), clientCallback);
            System.out.println("✓ Joined community chat");
        } catch (RemoteException e) {
            System.err.println("Failed to join community chat: " + e.getMessage());
            e.printStackTrace();
        }
    }
}