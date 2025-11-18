package ClientMVC.ClientController;

import ClientMVC.ClientCallbackImplementation;
import ClientMVC.ClientModel.UnifiedClientModel;
import ClientMVC.ClientView.Account.AccountView;
import ClientMVC.ClientView.Chat.ChatView;
import ClientMVC.ClientView.Home.HomeJFXController;
import ClientMVC.ClientView.Home.HomeView;
import ClientMVC.ClientView.Login.LoginView;
import Common.Objects.ClientObjects.Client;
import Common.Objects.ClientObjects.FriendRequest;
import Common.Objects.ClientObjects.GroupChat;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.rmi.RemoteException;
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
            try {
                sendFriendRequest(username);
            } catch (RemoteException e) {
                System.err.println("Error sending friend request: " + e.getMessage());
                e.printStackTrace();
            }
        });

        // Prefer new flow: name + select members from friends
        ui.setCreateGroupWithMembersListener(this::createGroupWithMembers);

        // Group member management listeners
        ui.setAddMemberToGroupListener((groupName, member) -> {
            try {
                addMemberToGroup(groupName, member);
            } catch (RemoteException e) {
                System.err.println("Failed to add member: " + e.getMessage());
            }
        });
        ui.setRemoveMemberFromGroupListener((groupName, member) -> {
            try {
                removeMemberFromGroup(groupName, member);
            } catch (RemoteException e) {
                System.err.println("Failed to remove member: " + e.getMessage());
            }
        });
        ui.setGroupMembersProvider(this::getMembersOfGroupByName);
    }

    private void loadClientData() {
        Client clientData = clientCallback.getClientData();
        if (clientData != null) {
            model.setUsername(clientData.getUsername());
            ui.setUserLabel(clientData.getUsername());

            List<String> friends = clientData.getFriends();
            if (friends != null && !friends.isEmpty()) {
                ui.setFriends(friends);
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
            } else {
                ui.setGroups(new ArrayList<>());
            }

            try {
                List<String> allUsersInSystem = model.fetchAllUsernames();
                if (allUsersInSystem != null) {
                    ui.setAllUsers(allUsersInSystem);
                }
            } catch (RemoteException e) {
                System.err.println("Failed to load all usernames: " + e.getMessage());
                e.printStackTrace();
            }

        } else {
            System.err.println("⚠ Warning: No client data available to load");
        }
    }
    // =========================================================

    private void sendFriendRequest(String targetusername) throws RemoteException {

        Gson gson = new Gson();
        String jsonString = gson.toJson(new FriendRequest(model.getUsername(), targetusername, "pending"));
        model.sendFriendRequest(jsonString);
    }

    public void handleFriendRequestSent(){
    }

    private void createGroup(String groupName) {
        try {
            String json = "{\"creator\":\"" + model.getUsername() + "\",\"groupName\":\"" + groupName + "\"}";
            model.createGroupChat(json);
        } catch (Exception e) {
            System.err.println("Failed to create group: " + e.getMessage());
            e.printStackTrace();
        }
        refreshUserData();
    }

    private void createGroupWithMembers(String groupName, List<String> members) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"creator\":\"").append(model.getUsername()).append("\",");
            sb.append("\"groupName\":\"").append(groupName).append("\",");
            sb.append("\"members\":[");
            if (members != null) {
                boolean first = true;
                for (String m : members) {
                    if (m == null || m.trim().isEmpty()) continue;
                    if (!first) sb.append(",");
                    sb.append("\"").append(m.trim()).append("\"");
                    first = false;
                }
            }
            sb.append("]}");

            model.createGroupChat(sb.toString());
        } catch (Exception e) {
            System.err.println("Failed to create group with members: " + e.getMessage());
            e.printStackTrace();
        }
        refreshUserData();
    }

    public void refreshUserData() {
        ui.setUserLabel(model.getUsername());
        try {
            // Request updated group chat data from server and update local data immediately
            String jsonString = String.format("{\"username\":\"%s\"}", model.getUsername());
            List<GroupChat> updatedGroups = model.getGroupChatHistories(jsonString, model.getUsername());
            if (updatedGroups != null && clientCallback.getClientData() != null) {
                // Update local client data immediately for real-time visibility
                clientCallback.getClientData().setGroupChats(updatedGroups);
                // Re-render groups UI with updated member lists
                Platform.runLater(() -> {
                    List<String> groupNames = new ArrayList<>();
                    for (GroupChat gc : updatedGroups) {
                        if (gc != null && gc.getGroupName() != null) {
                            groupNames.add(gc.getGroupName());
                        }
                    }
                    ui.setGroups(groupNames);
                });
            }
            System.out.println("✓ Updated group chat data from server.");
        } catch (Exception e) {
            System.err.println("Failed to request updated group chat data: " + e.getMessage());
        }
        try {
            String jsonString = String.format("{\"username\":\"%s\"}", model.getUsername());
            model.getFriendsList(jsonString);
            System.out.println("✓ Requested updated friends list from server.");
        } catch (RemoteException e) {
            System.err.println("Failed to request updated friends list: " + e.getMessage());
        }
    }

    public void updateGroupsUI(List<GroupChat> groupChats) {
        Platform.runLater(() -> {
            try {
                if (groupChats != null) {
                    List<String> groupNames = new ArrayList<>();
                    for (GroupChat gc : groupChats) {
                        if (gc != null && gc.getGroupName() != null) {
                            groupNames.add(gc.getGroupName());
                        }
                    }
                    ui.setGroups(groupNames);
                }
            } catch (Exception e) {
                System.err.println("Failed to update groups UI: " + e.getMessage());
            }
        });
    }

    private List<String> getMembersOfGroupByName(String groupName) {
        try {
            // Always try fresh fetch first to get latest member list
            List<GroupChat> updated = model.getGroupChatHistories("{}", model.getUsername());
            if (updated != null) {
                for (GroupChat gc : updated) {
                    if (gc != null && groupName.equals(gc.getGroupName())) {
                        // Update local cache with fresh data
                        if (clientCallback.getClientData() != null) {
                            clientCallback.getClientData().setGroupChats(updated);
                        }
                        return new ArrayList<>(gc.getReceivers());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch fresh group data: " + e.getMessage());
        }
        // Fallback: use cached data
        try {
            Client data = clientCallback.getClientData();
            if (data != null && data.getGroupChats() != null) {
                for (GroupChat gc : data.getGroupChats()) {
                    if (gc != null && groupName.equals(gc.getGroupName())) {
                        return new ArrayList<>(gc.getReceivers());
                    }
                }
            }
        } catch (Exception ignored) {}
        return new ArrayList<>();
    }

    private void addMemberToGroup(String groupName, String newMember) throws RemoteException {
        // Build JSON using groupName (server also accepts groupID, but name is fine here)
        String json = "{" +
                "\"requester\":\"" + model.getUsername() + "\"," +
                "\"groupName\":\"" + groupName + "\"," +
                "\"newMember\":\"" + newMember + "\"" +
                "}";
        model.addUserToGroup(json);
    }

    private void removeMemberFromGroup(String groupName, String member) throws RemoteException {
        String json = "{" +
                "\"requester\":\"" + model.getUsername() + "\"," +
                "\"groupName\":\"" + groupName + "\"," +
                "\"member\":\"" + member + "\"" +
                "}";
        model.removeUserFromGroup(json);
    }

    public void updateFriendsList(List<String> friends) {
        Platform.runLater(() -> {
            ui.setFriends(friends);
            ui.renderFriends(friends);
        });
    }

    private void openChatWithFriend(String friendName) throws IOException {
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
        } catch (Exception e) {
            System.err.println("Error opening chat with " + friendName);
            e.printStackTrace();
        }
    }

    private void openGroupChat(String groupName) throws IOException {
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
                try {
                    List<GroupChat> updated = model.getGroupChatHistories("{}", model.getUsername());
                    if (updated != null) {
                        for (GroupChat gc : updated) {
                            if (gc != null && groupName.equals(gc.getGroupName())) {
                                groupID = gc.getGroupID();
                                break;
                            }
                        }
                    }
                } catch (Exception fetchErr) {
                    System.err.println("Failed to fetch latest group list: " + fetchErr.getMessage());
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
        } catch (Exception e) {
            System.err.println("Error opening group chat " + groupName);
            e.printStackTrace();
        }
    }

    private void openCommunityChat() throws IOException {
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
            chatController.setCurrentCommunityChat();
            chatStage.setOnCloseRequest(event -> {
                try {
                    model.leaveCommunityChat(model.getUsername());
                } catch (RemoteException e) {
                    System.err.println("Failed to leave community chat: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Error opening community chat");
            e.printStackTrace();
        }
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

    public void onForceLogout(String reason) {
        Platform.runLater(() -> {

            // 1. Show the forced logout message
            ui.showAlert(
                    Alert.AlertType.WARNING,
                    "Logged Out",
                    reason
            );

            // 2. CLOSE the Home GUI
            try {
                if (view.getStage() != null) {
                    view.getStage().close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 3. Open login screen again
            try {
                view.getStage().hide();
                Stage loginStage = ClientMVC.ClientView.Login.LoginView.getPrimaryStage();
                if (loginStage != null) {
                    loginStage.show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}