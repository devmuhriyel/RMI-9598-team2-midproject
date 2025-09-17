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

        ui.setFriendClickListener(friendName -> {
            try {

                openChatWithFriend(friendName);
                view.openChatWithFriend();
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
        chatController.loadChatHistory(model.getUsername(), friendName);
        new Thread(chatModel, "Chat Listener").start();

    }
}
