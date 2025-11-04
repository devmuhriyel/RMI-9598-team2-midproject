package AdminMVC;

import Common.Interfaces.AdminCallbackInterface;
import AdminMVC.AdminControllers.AdminController;
import AdminMVC.AdminControllers.AdminLoginController;
import AdminMVC.AdminModel.UnifiedAdminModel;
import Common.Objects.ClientObjects.Client;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminCallbackImplementation extends UnicastRemoteObject implements AdminCallbackInterface {
    private AdminController adminController;
    private AdminLoginController adminLoginController;
    private UnifiedAdminModel model;

    public AdminCallbackImplementation() throws RemoteException {
        super();
    }

    public void setAdminController(AdminController controller) {
        this.adminController = controller;
    }

    public void setAdminLoginController(AdminLoginController controller) {
        this.adminLoginController = controller;
    }

    public void setModel(UnifiedAdminModel model) {
        this.model = model;
    }

    @Override
    public void onReceiveActiveUsersList(List<String> activeUsernames) throws RemoteException {
        Platform.runLater(() -> {
            if (model != null) {
                model.updateActiveUsersList(activeUsernames);
            } else {
            }
        });
    }

    @Override
    public void onReceiveUsersList(List<Client> users) throws RemoteException {
        for (Client user : users) {
        }
        Platform.runLater(() -> {
            if (model != null) {
                model.updateUsersList(users);
            } else {
            }
        });
    }

    @Override
    public void onReceiveGroupsList(String groupsJson) throws RemoteException {
        Platform.runLater(() -> {
            if (model != null) {
                try {
                    Gson gson = new Gson();
                    List<Map<String, String>> groupsList = new ArrayList<>();
                    JsonArray groupsArray = JsonParser.parseString(groupsJson).getAsJsonArray();
                    for (int i = 0; i < groupsArray.size(); i++) {
                        JsonObject group = groupsArray.get(i).getAsJsonObject();
                        Map<String, String> groupMap = new HashMap<>();
                        String groupName = group.has("groupName") ? group.get("groupName").getAsString() : "Unknown";
                        groupMap.put("groupName", groupName);
                        String groupID = group.has("groupID") ? group.get("groupID").getAsString() : "";
                        groupMap.put("groupID", groupID);
                        String owner = group.has("owner") ? group.get("owner").getAsString() : "Unknown";
                        groupMap.put("owner", owner);
                        StringBuilder membersBuilder = new StringBuilder();
                        int memberCount = 0;
                        if (group.has("receivers") && group.get("receivers").isJsonArray()) {
                            JsonArray receivers = group.get("receivers").getAsJsonArray();
                            memberCount = receivers.size();

                            for (int j = 0; j < receivers.size(); j++) {
                                if (j > 0) membersBuilder.append(", ");
                                membersBuilder.append(receivers.get(j).getAsString());
                            }
                        }
                        String members = membersBuilder.length() > 0 ? membersBuilder.toString() : "No members";
                        groupMap.put("members", members);
                        groupMap.put("memberCount", String.valueOf(memberCount));
                        groupsList.add(groupMap);
                    }
                    model.updateGroupsList(groupsList);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
            }
        });
    }

    @Override
    public void onReceiveCommunityMessage(String messageJson) throws RemoteException {
        Platform.runLater(() -> {
            try {
                if (model != null) {
                    model.receiveCommunityMessage(messageJson);
                } else {
                }
            } catch (Exception e) {
                System.err.println("Error handling community message: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onSuccessfulSignUp() throws RemoteException {
        Platform.runLater(() -> {
            if (adminController != null) {
                adminController.handleSuccessfulSignUp();
            }
        });
    }

    @Override
    public void onFailedSignUp() throws RemoteException {
        Platform.runLater(() -> {
            if (adminController != null) {
                adminController.handleFailedSignUp();
            }
        });
    }

    @Override
    public void onSuccessfulLogin() throws RemoteException {
        Platform.runLater(() -> {
            if (model != null) {
                model.notifyLoginSuccess();
            } else {
            }
            if (adminController != null) {
                adminController.handleSuccessfulLogin();
            }
        });
    }

    @Override
    public void onFailedLogin() throws RemoteException {
        Platform.runLater(() -> {
            if (model != null) {
                model.notifyLoginFailure();
            } else {
            }
            if (adminLoginController != null) {
                adminLoginController.handleFailedLogin();
            } else {
                System.err.println("  ✗ AdminLoginController is null in callback!");
            }

            if (adminController != null) {
                adminController.handleFailedLogin();
            }
        });
    }

    @Override
    public void onSuccessfulLogout() throws RemoteException {
        Platform.runLater(() -> {
            if (adminController != null) {
                adminController.handleSuccessfulLogout();
            }
        });
    }

    @Override
    public void onSuccessfulPasswordChange() throws RemoteException {
        Platform.runLater(() -> {
            if (adminController != null) {
                adminController.handleSuccessfulPasswordChange();
            }
        });
    }

    @Override
    public void onFailedPasswordChange() throws RemoteException {
        Platform.runLater(() -> {
            if (adminController != null) {
                adminController.handleFailedPasswordChange();
            }
        });
    }

    @Override
    public void onSuccessfulCodeVerification() throws RemoteException {
        Platform.runLater(() -> {
            if (adminController != null) {
                adminController.handleSuccessfulCodeVerification();
            }
        });
    }

    @Override
    public void onFailedCodeVerification() throws RemoteException {
        Platform.runLater(() -> {
            if (adminLoginController != null) {
                adminLoginController.handleFailedCodeVerification();
            }
            if (adminController != null) {
                adminController.handleFailedCodeVerification();
            }
        });
    }

    @Override
    public void onSearchUserResults() throws RemoteException {
        Platform.runLater(() -> {
            if (adminController != null) {
                adminController.handleSearchUserResults();
            }
        });
    }

    @Override
    public void onSearchGroupResults() throws RemoteException {
        Platform.runLater(() -> {
            if (adminController != null) {
                adminController.handleSearchGroupResults();
            }
        });
    }

    @Override
    public void onDeleteUser() throws RemoteException {
        Platform.runLater(() -> {
            if (adminController != null) {
                adminController.handleDeleteUser();
            }
        });
    }

    @Override
    public void onDeleteGroup() throws RemoteException {
        Platform.runLater(() -> {
            if (adminController != null) {
                adminController.handleDeleteGroup();
            }
        });
    }

    @Override
    public void onGetChatHistoryOfUser() throws RemoteException {
        Platform.runLater(() -> {
            if (adminController != null) {
                adminController.handleGetChatHistoryOfUser();
            }
        });
    }

    @Override
    public void onGetChatHistoryOfGroup() throws RemoteException {
        Platform.runLater(() -> {
            if (adminController != null) {
                adminController.handleGetChatHistoryOfGroup();
            }
        });
    }

    @Override
    public void onGetChatHistoryOfCommunity() throws RemoteException {
        Platform.runLater(() -> {
            if (adminController != null) {
                adminController.handleGetChatHistoryOfCommunity();
            }
        });
    }

    @Override
    public void onCommunityMessageSent() throws RemoteException {
        Platform.runLater(() -> {
            if (adminController != null) {
                adminController.handleCommunityMessageSent();
            }
        });
    }

    @Override
    public void onCommunityMessageReceived() throws RemoteException {
        Platform.runLater(() -> {
            if (adminController != null) {
                adminController.handleCommunityMessageReceived();
            }
        });
    }
}