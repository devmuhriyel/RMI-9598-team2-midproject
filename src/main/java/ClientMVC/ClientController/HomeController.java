package ClientMVC.ClientController;

import ClientMVC.ClientModel.HomeModel;
import ClientMVC.ClientView.Home.HomeJFXController;
import ClientMVC.ClientView.Home.HomeView;
import ClientMVC.ClientModel.ChatModel;
import ClientMVC.ClientView.Chat.ChatView;
import ClientMVC.ClientController.ChatController;
import javafx.stage.Stage;

import java.io.IOException;

public class HomeController {
    private final HomeModel model;
    private final HomeView view;
    private final HomeJFXController ui;

    public HomeController(HomeModel model, HomeView view, HomeJFXController ui) {
        this.model = model;
        this.view = view;
        this.ui = ui;
        initialize();
    }

    private void initialize() {
        if (model.getUsername() != null) {
            ui.setUserLabel(model.getUsername());
        }

        ui.setFriends(model.getFriends());
        ui.setGroups(model.getGroups());

        // Handle friend clicks (existing functionality)
        ui.setFriendClickListener(friendName -> {
            try {
                openChatWithFriend(friendName);
                view.openChatWithFriend();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // Handle group clicks (new functionality)
        ui.setGroupClickListener(groupName -> {
            try {
                openGroupChat(groupName);
                view.openChatWithFriend(); // Reuse the same method to close current view
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void openChatWithFriend(String friendName) throws IOException {
        model.requestChatHistory(friendName);
        ChatView chatView = new ChatView();
        Stage stage = new Stage();
        chatView.start(stage);
        chatView.getFXController().setLabel(friendName);

        ChatModel chatModel = new ChatModel(model.getSocket(), model.getUsername());
        ChatController chatController = new ChatController(chatModel, chatView, chatView.getFXController());

        // IMPORTANT: Set up the message listener to update the chat UI in real-time
        chatModel.setMessageListener(message -> {
            javafx.application.Platform.runLater(() -> {
                chatView.getFXController().addMessage(message, false);
            });
        });

        chatController.loadChatHistory(model.getUsername(), friendName);
        new Thread(chatModel, "Chat Listener").start();
    }

    private void openGroupChat(String groupName) throws IOException {
        model.requestGroupChatHistory(groupName);

        ChatView chatView = new ChatView();
        Stage stage = new Stage();
        chatView.start(stage);
        chatView.getFXController().setLabel("Group: " + groupName);

        ChatModel chatModel = new ChatModel(model.getSocket(), model.getUsername());
        ChatController chatController = new ChatController(chatModel, chatView, chatView.getFXController());

        // IMPORTANT: Set up the message listener for group messages too
        chatModel.setMessageListener(message -> {
            javafx.application.Platform.runLater(() -> {
                chatView.getFXController().addMessage(message, false);
            });
        });

        chatController.loadGroupChatHistory(model.getUsername(), groupName);
        new Thread(chatModel, "Group Chat Listener").start();
    }
}