package ClientMVC.ClientController;

import ClientMVC.ClientCallbackImplementation;
import ClientMVC.ClientModel.UnifiedClientModel;
import ClientMVC.ClientView.Account.AccountView;
import ClientMVC.ClientView.Home.HomeJFXController;
import ClientMVC.ClientView.Home.HomeView;
import ClientMVC.ClientView.Chat.ChatView;
import Common.Objects.ClientObjects.Client;
import Common.Objects.ClientObjects.FriendRequest;
import Common.Objects.ClientObjects.GroupChat;
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
        loadClientData(); // Load data immediately after initialization
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
    }

    private void loadClientData() {
        Client clientData = clientCallback.getClientData();
        if (clientData != null) {
            System.out.println("Loading client data into HomeView...");

            // Set username
            model.setUsername(clientData.getUsername());
            ui.setUserLabel(clientData.getUsername());

            // Load friends list
            List<String> friends = clientData.getFriends();
            if (friends != null && !friends.isEmpty()) {
                ui.setFriends(friends);
                System.out.println("✓ Loaded " + friends.size() + " friends: " + friends);
            } else {
                ui.setFriends(new ArrayList<>());
                System.out.println("✓ No friends to load");
            }

            // Load group chats
            List<GroupChat> groupChats = clientData.getGroupChats();
            if (groupChats != null && !groupChats.isEmpty()) {
                List<String> groupNames = new ArrayList<>();
                for (GroupChat gc : groupChats) {
                    groupNames.add(gc.getGroupName());
                }
                ui.setGroups(groupNames);
                System.out.println("✓ Loaded " + groupNames.size() + " groups: " + groupNames);
            } else {
                ui.setGroups(new ArrayList<>());
                System.out.println("✓ No groups to load");
            }

            System.out.println("✓ Client data loaded successfully for: " + clientData.getUsername());
        } else {
            System.err.println("⚠ Warning: No client data available to load");
        }
    }

    private void sendFriendRequest(String targetusername) throws RemoteException {
        System.out.println("Sending friend request to: " + targetusername);
        Gson gson = new Gson();
        String jsonString = gson.toJson(new FriendRequest(model.getUsername(), targetusername, "pending"));
        model.sendFriendRequest(jsonString);
    }

    public void handleFriendRequestSent(){
        Platform.runLater(() -> {
            System.out.println("Friend request sent successfully!");
        });
    }

    private void createGroup(String groupName) {
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
        // Reload client data from callback
        loadClientData();
    }

    // In HomeController.java

    public void updateFriendsList(List<String> friends) {
        Platform.runLater(() -> {
            System.out.println("HomeController: Received and updating friends list.");
            ui.setFriends(friends);
            ui.renderFriends(friends);
        });
    }

    private void openChatWithFriend(String friendName) throws IOException {
        // TODO: Implement chat view opening
    }

    private void openGroupChat(String groupName) throws IOException {
        // TODO: Implement group chat view opening
    }

    private void openCommunityChat() throws IOException {
        // TODO: Implement community chat view opening
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