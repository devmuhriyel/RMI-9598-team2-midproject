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
    private final HomeView view; // kept for future navigation beyond chat
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

        ui.setFriendClickListener(friendName -> {
            try {
                openChatWithFriend(friendName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void openChatWithFriend(String friendName) throws IOException {
        ChatView chatView = new ChatView();
        Stage stage = new Stage();
        chatView.start(stage);

        ChatModel chatModel = new ChatModel(model.getSocket(), friendName); // ⚡ model provides socket
        new ChatController(chatModel, chatView, chatView.getFXController());
        new Thread(chatModel, "Chat Listener").start();

        // Preload basic history from XML
        java.util.List<String> history = model.getPrivateHistory(friendName);
        if (history != null) {
            for (String line : history) {
                chatView.getFXController().addMessage(line);
            }
        }
    }
}
