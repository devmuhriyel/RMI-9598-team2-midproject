package ClientMVC.ClientController;

import ClientMVC.ClientCallbackImplementation;
import ClientMVC.ClientModel.UnifiedClientModel;
import ClientMVC.ClientView.Account.AccountView;
import ClientMVC.ClientView.Home.HomeJFXController;
import ClientMVC.ClientView.Home.HomeView;
import ClientMVC.ClientView.Chat.ChatView;
import Common.Objects.ClientObjects.FriendRequest;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

public class HomeController {
    private final UnifiedClientModel model;
    private final HomeView view;
    private final HomeJFXController ui;
    private ClientCallbackImplementation clientCallback;

    public HomeController(UnifiedClientModel model, HomeView view, HomeJFXController ui, ClientCallbackImplementation callback) {
        this.model = model;
        this.view = view;
        this.ui = ui;
        this.clientCallback = callback;
        callback.setClientHomeController(this);
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

                    new AccountController(model, accountView, accountView.getFXController(), clientCallback);

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
            ui.renderFriends(filterList(ui.getOriginalFriends(), q));
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
            try {
                sendFriendRequest(username);
            } catch (RemoteException e) {
                System.err.println("Error sending friend request: " + e.getMessage());
                e.printStackTrace();
            }
        });

        ui.setCreateGroupListener(this::createGroup);
        refreshUserData();
    }

    private void sendFriendRequest(String targetusername) throws RemoteException {
        System.out.print("Sending friend request to: " + targetusername);
        Gson gson = new Gson();
        String jsonString = gson.toJson(new FriendRequest(model.getUsername(), targetusername, "pending"));
        model.sendFriendRequest(jsonString);
   }

   public void handleFriendRequestSent(){
        Platform.runLater(() -> {
            System.out.println("Friend request send successfully!");
        });
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
            }
            refreshUserData();
        }
    }

    public void refreshUserData() {
//        Platform.runLater(() -> {
////            List<String> friends = model.getFriends();
////            List<String> groups = model.getGroups();
//            ui.setFriends(friends);
//            ui.setGroups(groups);
//            for (String group : groups) {
//                System.out.println("Client is in group: " + group);
//            }
//        });
    }

    private void openChatWithFriend(String friendName) throws IOException {
//        model.requestChatHistory(friendName);
//        ChatView chatView = new ChatView();
//        Stage stage = new Stage();
//        chatView.start(stage);
//        chatView.getFXController().setLabel(friendName);
//        ChatController chatController = new ChatController(model, chatView, chatView.getFXController());
//        chatController.loadChatHistory(model.getUsername(), friendName);
    }

    private void openGroupChat(String groupName) throws IOException {
//        model.requestGroupChatHistory(groupName);
//        ChatView chatView = new ChatView();
//        Stage stage = new Stage();
//        chatView.start(stage);
//        chatView.getFXController().setLabel("Group: " + groupName);
//        ChatController chatController = new ChatController(model, chatView, chatView.getFXController());
//        chatController.loadGroupChatHistory(model.getUsername(), groupName);
    }

    private void openCommunityChat() throws IOException {
//        ChatView chatView = new ChatView();
//        Stage stage = new Stage();
//        chatView.start(stage);
//        chatView.getFXController().setLabel("Community Chat");
//        ChatController chatController = new ChatController(model, chatView, chatView.getFXController());
//        chatController.loadCommunityChatHistory(model.getUsername());
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