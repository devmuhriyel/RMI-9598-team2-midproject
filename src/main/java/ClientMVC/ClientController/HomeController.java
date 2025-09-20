package ClientMVC.ClientController;

import ClientMVC.ClientModel.UnifiedClientModel;
import ClientMVC.ClientView.Account.AccountView;
import ClientMVC.ClientView.Home.HomeJFXController;
import ClientMVC.ClientView.Home.HomeView;
import ClientMVC.ClientView.Chat.ChatView;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HomeController {
    private final UnifiedClientModel model;
    private final HomeView view;
    private final HomeJFXController ui;

    public HomeController(UnifiedClientModel model, HomeView view, HomeJFXController ui) {
        this.model = model;
        this.view = view;
        this.ui = ui;
        initialize();
    }

    private void initialize() {
        //forda UserProfile
        ui.setAccountButtonListener(() -> {
            Platform.runLater(() -> {
                try {
                    view.getStage().close();

                    AccountView accountView = new AccountView();
                    Stage accountStage = new Stage();
                    accountView.start(accountStage);
                    new AccountController(model, accountView, accountView.getFXController());

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        });
        if (model.getUsername() != null) {
            ui.setUserLabel(model.getUsername());
        }

        refreshUserData();
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

        if (model.getUsername() != null) {
            ui.setUserLabel(model.getUsername());
        }

        ui.setFriends(model.getFriends());
        ui.setGroups(model.getGroups());

        // Set up search functionality
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

        // Handle friend clicks
        ui.setFriendClickListener(friendName -> {
            try {
                openChatWithFriend(friendName);
                view.openChatWithFriend();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // Handle group clicks
        ui.setGroupClickListener(groupName -> {
            try {
                // Check if it's community chat
                if (groupName.equals("Community Chat")) {
                    openCommunityChat();
                } else {
                    openGroupChat(groupName);
                }
                view.openChatWithFriend();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // Set up force logout handler
        model.setForceLogoutListener(message -> {
            Platform.runLater(() -> {
                System.out.println("Force logged out: " + message);
                // Handle logout UI updates
            });
        });
    }

    public void refreshUserData() {
        Platform.runLater(() -> {
            // Refresh friends and groups from the model
            List<String> friends = model.getFriends();
            List<String> groups = model.getGroups();

            System.out.println("Refreshing user data - Friends: " + friends.size() + ", Groups: " + groups.size());

            // Update the UI
            ui.setFriends(friends);
            ui.setGroups(groups);

            // Log the groups for debugging
            for (String group : groups) {
                System.out.println("User is in group: " + group);
            }
        });
    }

    private void openChatWithFriend(String friendName) throws IOException {
        model.requestChatHistory(friendName);

        ChatView chatView = new ChatView();
        Stage stage = new Stage();
        chatView.start(stage);
        chatView.getFXController().setLabel(friendName);

        // Pass the unified model to chat controller
        ChatController chatController = new ChatController(model, chatView, chatView.getFXController());
        chatController.loadChatHistory(model.getUsername(), friendName);
    }

    private void openGroupChat(String groupName) throws IOException {
        model.requestGroupChatHistory(groupName);

        ChatView chatView = new ChatView();
        Stage stage = new Stage();
        chatView.start(stage);
        chatView.getFXController().setLabel("Group: " + groupName);

        ChatController chatController = new ChatController(model, chatView, chatView.getFXController());
        chatController.loadGroupChatHistory(model.getUsername(), groupName);
    }

    private void openCommunityChat() throws IOException {
        ChatView chatView = new ChatView();
        Stage stage = new Stage();
        chatView.start(stage);
        chatView.getFXController().setLabel("Community Chat");

        ChatController chatController = new ChatController(model, chatView, chatView.getFXController());
        chatController.loadCommunityChatHistory(model.getUsername());
    }

    private List<String> filterList(List<String> source, String queryLower) {
        List<String> result = new ArrayList<>();
        for (String item : source) {
            if (item != null && item.toLowerCase().startsWith(queryLower)) {
                result.add(item);
            }
        }
        return result;
    }
}