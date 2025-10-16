package ClientMVC.ClientController;

import ClientMVC.ClientModel.UnifiedClientModel;
import ClientMVC.ClientView.Account.AccountView;
import ClientMVC.ClientView.Home.HomeJFXController;
import ClientMVC.ClientView.Home.HomeView;
import ClientMVC.ClientView.Chat.ChatView;
import javafx.application.Platform;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;
import java.util.ArrayList;
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
        ui.setSearchListener(queryText -> {
            String q = (queryText == null) ? "" : queryText.trim().toLowerCase();
            if (q.isEmpty()) {
                ui.renderFriends(ui.getOriginalFriends());
                ui.renderGroups(ui.getOriginalGroups());
                return;
            }
            List<String> allUsers = model.getAllUsers();
            List<String> matchedUsers = filterList(allUsers, q);
            ui.renderFriends(matchedUsers);
            ui.renderGroups(filterList(ui.getOriginalGroups(), q));
        });
        ui.setFriendClickListener(friendName -> {
            try {
                openChatWithFriend(friendName);
                view.openChatWithFriend();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        ui.setGroupClickListener(groupName -> {
            try {
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
        ui.setAddFriendListener(username -> {
            System.out.println("HomeController: addFriend called with username: " + username);
            addFriend(username);
        });
        ui.setCreateGroupListener(this::createGroup);
        
        model.setChatHistoryListener(response -> {
            if ("GROUP_CREATED".equals(response)) {
                Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                    alert.setTitle("Group Created");
                    alert.setHeaderText(null);
                    alert.setContentText("Group has been created successfully!");
                    alert.showAndWait();
                    refreshUserData();
                });
            } else if ("GROUP_CREATE_FAILED".equals(response)) {
                Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setTitle("Group Creation Failed");
                    alert.setHeaderText(null);
                    alert.setContentText("Failed to create group. Please try again.");
                    alert.showAndWait();
                });
            }
        });
        
        model.setForceLogoutListener(message -> {
            Platform.runLater(() -> {
                System.out.println("Force logged out: " + message);
            });
        });

        model.setFriendRequestListener(response -> {
            Platform.runLater(() -> {
                switch (response) {
                    case "REQUEST_SENT":
                        System.out.println("Friend request sent successfully");
                        break;
                    case "REQUEST_FAILED":
                        System.out.println("Failed to send friend request");
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                        alert.setTitle("Friend Request Failed");
                        alert.setHeaderText(null);
                        alert.setContentText("Failed to send friend request. Client may not exist or request already sent.");
                        alert.showAndWait();
                        break;
                    case "REQUEST_ACCEPTED":
                        System.out.println("Friend request was accepted");
                        refreshUserData();
                        break;
                    case "REQUEST_DECLINED":
                        System.out.println("Friend request was declined");
                        break;
                    case "FRIENDS_UPDATED":
                        refreshUserData();
                        break;
                    default:
                        if (response.startsWith("SENT_REQUESTS:")) {
                            String requestData = response.substring("SENT_REQUESTS:".length());
                            System.out.println("Received sent requests: " + requestData);
                        } else if (response.startsWith("RECEIVED_REQUESTS:")) {
                            String requestData = response.substring("RECEIVED_REQUESTS:".length());
                            System.out.println("Received friend requests: " + requestData);
                        }
                        break;
                }
            });
        });

        refreshUserData();
    }

    private void addFriend(String username) {
        model.addFriend(username);
        System.out.println("HomeController: Sending friend request to " + username);
    }

    private void createGroup(String groupName) {
        // Show a dialog to select members for the group
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("Create Group");
        dialog.setHeaderText("Create new group: " + groupName);
        dialog.setContentText("Enter comma-separated usernames to add to the group (optional):");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String membersInput = result.get().trim();
            if (!membersInput.isEmpty()) {
                String[] membersArray = membersInput.split(",");
                List<String> members = new ArrayList<>();
                for (String member : membersArray) {
                    String trimmedMember = member.trim();
                    if (!trimmedMember.isEmpty() && !trimmedMember.equals(model.getUsername())) {
                        members.add(trimmedMember);
                    }
                }
                model.createGroup(groupName, members);
            } else {
                model.createGroup(groupName, new ArrayList<>());
            }
            refreshUserData();
        }
    }

    public void refreshUserData() {
        Platform.runLater(() -> {
            List<String> friends = model.getFriends();
            List<String> groups = model.getGroups();
            ui.setFriends(friends);
            ui.setGroups(groups);
            for (String group : groups) {
                System.out.println("Client is in group: " + group);
            }
        });
    }

    private void openChatWithFriend(String friendName) throws IOException {
        model.requestChatHistory(friendName);
        ChatView chatView = new ChatView();
        Stage stage = new Stage();
        chatView.start(stage);
        chatView.getFXController().setLabel(friendName);
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