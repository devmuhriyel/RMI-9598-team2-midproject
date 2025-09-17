package ClientMVC.ClientController;

import ClientMVC.ClientModel.HomeModel;
import ClientMVC.ClientView.Home.HomeJFXController;
import ClientMVC.ClientView.Home.HomeView;
import ClientMVC.ClientModel.ChatModel;
import ClientMVC.ClientView.Chat.ChatView;
import ClientMVC.ClientController.ChatController;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

        // For search
        ui.setSearchListener(queryText -> {
            String q = (queryText == null) ? "" : queryText.trim().toLowerCase();
            if (q.isEmpty()) {
                ui.renderFriends(ui.getOriginalFriends());
                ui.renderGroups(ui.getOriginalGroups());
                return;
            }
            ui.renderFriends(filterList(ui.getOriginalFriends(), q));
            ui.renderGroups(filterList(ui.getOriginalGroups(), q));
        });

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
        chatController.loadChatHistory(model.getUsername(), friendName);
        new Thread(chatModel, "Chat Listener").start();
    }

    private void openGroupChat(String groupName) throws IOException {
        // Request group chat history from server
        model.requestGroupChatHistory(groupName);

        // Create new chat view
        ChatView chatView = new ChatView();
        Stage stage = new Stage();
        chatView.start(stage);
        chatView.getFXController().setLabel("Group: " + groupName);

        // Create chat model and controller
        ChatModel chatModel = new ChatModel(model.getSocket(), model.getUsername());
        ChatController chatController = new ChatController(chatModel, chatView, chatView.getFXController());

        // Load group chat history
        chatController.loadGroupChatHistory(model.getUsername(), groupName);

        new Thread(chatModel, "Group Chat Listener").start();
    }


    private List<String> filterList(List<String> source, String queryLower) {
        List<String> result = new ArrayList<>();
        for (String item : source) {
            if (item != null && item.toLowerCase().contains(queryLower)) {
                result.add(item);
            }
        }
        return result;
    }
}