package ClientMVC.ClientController;

import ClientMVC.ClientCallbackImplementation;
import ClientMVC.ClientModel.UnifiedClientModel;
import ClientMVC.ClientView.Account.AccountView;
import ClientMVC.ClientView.Chat.ChatView;
import ClientMVC.ClientView.Home.HomeJFXController;
import ClientMVC.ClientView.Home.HomeView;
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
        loadClientData();
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
            List<String> allUsers = ui.getAllUsers();

            if (q.isEmpty()) {
                ui.renderFriends(ui.getOriginalFriends());
                ui.renderGroups(ui.getOriginalGroups());
                return;
            }
            List<String> filteredUsers = filterList(allUsers, q);
            ui.renderFriends(filteredUsers);
            ui.renderGroups(filterList(ui.getOriginalGroups(), q));
        });

        ui.setFriendClickListener(friendName -> {
            System.out.println("Friend clicked: " + friendName);
            Platform.runLater(() -> {
                try {
                    openChatWithFriend(friendName);
                } catch (IOException e) {
                    System.err.println("Failed to open chat: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        });

        ui.setGroupClickListener(groupName -> {
            System.out.println("Group clicked: " + groupName);
            Platform.runLater(() -> {
                try {
                    if (groupName.equals("Community Chat")) {
                        openCommunityChat();
                    } else {
                        openGroupChat(groupName);
                    }
                } catch (IOException e) {
                    System.err.println("Failed to open group chat: " + e.getMessage());
                    e.printStackTrace();
                }
            });
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

    // =========================================================
    // === UPDATED loadClientData() to fetch ALL users ===
    // =========================================================
    private void loadClientData() {
        Client clientData = clientCallback.getClientData();
        if (clientData != null) {
            System.out.println("Loading client data into HomeView...");

            model.setUsername(clientData.getUsername());
            ui.setUserLabel(clientData.getUsername());

            List<String> friends = clientData.getFriends();
            if (friends != null && !friends.isEmpty()) {
                ui.setFriends(friends);
                System.out.println("✓ Loaded " + friends.size() + " friends: " + friends);
            } else {
                ui.setFriends(new ArrayList<>());
                System.out.println("✓ No friends to load");
            }

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

            // --- NEW: Load ALL users for Search ---
            try {
                // NOTE: This call relies on the model.fetchAllUsernames() method you must implement
                List<String> allUsersInSystem = model.fetchAllUsernames();

                if (allUsersInSystem != null) {
                    ui.setAllUsers(allUsersInSystem); // Pass the complete list of ALL users to the View
                    System.out.println("✓ Loaded all users for search.");
                }
            } catch (RemoteException e) {
                System.err.println("Failed to load all usernames: " + e.getMessage());
                e.printStackTrace();
            }
            // ------------------------------------

            System.out.println("✓ Client data loaded successfully for: " + clientData.getUsername());
        } else {
            System.err.println("⚠ Warning: No client data available to load");
        }
    }
    // =========================================================

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
            List<String> members = new ArrayList<>();
            if (!membersInput.isEmpty()) {
                String[] membersArray = membersInput.split(",");
                for (String member : membersArray) {
                    String trimmedMember = member.trim();
                    if (!trimmedMember.isEmpty() && !trimmedMember.equals(model.getUsername())) {
                        members.add(trimmedMember);
                    }
                }
            }
            try {
                // Build JSON payload for server
                StringBuilder json = new StringBuilder();
                json.append("{")
                        .append("\"creator\":\"").append(model.getUsername()).append("\",")
                        .append("\"groupName\":\"").append(groupName).append("\",")
                        .append("\"members\":[");
                for (int i = 0; i < members.size(); i++) {
                    if (i > 0) json.append(',');
                    json.append("\"").append(members.get(i)).append("\"");
                }
                json.append("]}");

                model.createGroupChat(json.toString());
            } catch (Exception e) {
                System.err.println("Failed to create group: " + e.getMessage());
                e.printStackTrace();
            }
            // Optimistically refresh local UI; server callback will also refresh
            refreshUserData();
        }
    }

    public void refreshUserData() {
        // Keep username current
        ui.setUserLabel(model.getUsername());
        try {
            List<GroupChat> updatedGroups = model.getGroupChatHistories("{}", model.getUsername());
            List<String> groupNames = new ArrayList<>();
            if (updatedGroups != null) {
                for (GroupChat gc : updatedGroups) {
                    groupNames.add(gc.getGroupName());
                }
            }
            ui.setGroups(groupNames);
        } catch (Exception e) {
            System.err.println("Failed to refresh group data: " + e.getMessage());
        }
    }

    public void updateFriendsList(List<String> friends) {
        Platform.runLater(() -> {
            System.out.println("HomeController: Received and updating friends list.");
            ui.setFriends(friends);
            ui.renderFriends(friends);
        });
    }

    private void openChatWithFriend(String friendName) throws IOException {
        System.out.println("Opening chat with: " + friendName);
        try {
            ChatView chatView = new ChatView();
            Stage chatStage = new Stage();
            chatView.start(chatStage);

            ChatController chatController = new ChatController(
                    model,
                    chatView,
                    chatView.getFXController(),
                    clientCallback
            );

            clientCallback.setClientChatController(chatController);
            chatController.setCurrentChatPartner(friendName);
            chatController.loadChatHistory(friendName);

            System.out.println("✓ Chat window opened for: " + friendName);

        } catch (Exception e) {
            System.err.println("Error opening chat with " + friendName);
            e.printStackTrace();
        }
    }

    private void openGroupChat(String groupName) throws IOException {
        System.out.println("Opening group chat: " + groupName);
        try {
            // Find the group ID from client data
            Client clientData = clientCallback.getClientData();
            String groupID = null;

            if (clientData != null && clientData.getGroupChats() != null) {
                for (GroupChat gc : clientData.getGroupChats()) {
                    if (gc.getGroupName().equals(groupName)) {
                        groupID = gc.getGroupID();
                        break;
                    }
                }
            }

            if (groupID == null) {
                System.err.println("Could not find group ID for: " + groupName);
                return;
            }

            ChatView chatView = new ChatView();
            Stage chatStage = new Stage();
            chatView.start(chatStage);

            ChatController chatController = new ChatController(
                    model,
                    chatView,
                    chatView.getFXController(),
                    clientCallback
            );

            clientCallback.setClientChatController(chatController);
            chatController.setCurrentGroupChat(groupID, groupName);
            chatController.loadGroupChatHistory(groupID);

            System.out.println("✓ Group chat window opened for: " + groupName);

        } catch (Exception e) {
            System.err.println("Error opening group chat " + groupName);
            e.printStackTrace();
        }
    }

    private void openCommunityChat() throws IOException {
        System.out.println("Opening community chat");
        // TODO: Implement community chat
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