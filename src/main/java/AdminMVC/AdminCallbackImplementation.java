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
        System.out.println("✓ AdminController set in callback");
    }

    public void setAdminLoginController(AdminLoginController controller) {
        this.adminLoginController = controller;
        System.out.println("✓ AdminLoginController set in callback");
    }

    public void setModel(UnifiedAdminModel model) {
        this.model = model;
        System.out.println("✓ Model set in callback");
    }

    @Override
    public void onReceiveActiveUsersList(List<String> activeUsernames) throws RemoteException {
        System.out.println("✓ Received active users list: " + activeUsernames.size() + " online");
        Platform.runLater(() -> {
            if (model != null) {
                model.updateActiveUsersList(activeUsernames);
            } else {
                System.err.println("✗ Model is null in onReceiveActiveUsersList");
            }
        });
    }

    @Override
    public void onReceiveUsersList(List<Client> users) throws RemoteException {
        System.out.println("✓ Received " + users.size() + " users from server");

        for (Client user : users) {
            System.out.println("  User: " + user.getUsername() + " | Password: " + user.getPassword());
        }

        Platform.runLater(() -> {
            if (model != null) {
                System.out.println("  → Updating model with users list");
                model.updateUsersList(users);  // THIS TRIGGERS THE LOOP
            } else {
                System.err.println("✗ Model is null in onReceiveUsersList");
            }
        });
    }

    @Override
    public void onReceiveGroupsList(String groupsJson) throws RemoteException {
        System.out.println("✓ Received groups data from server");
        Platform.runLater(() -> {
            if (model != null) {
                try {
                    Gson gson = new Gson();
                    List<Map<String, String>> groupsList = new ArrayList<>();

                    JsonArray groupsArray = JsonParser.parseString(groupsJson).getAsJsonArray();
                    for (int i = 0; i < groupsArray.size(); i++) {
                        JsonObject group = groupsArray.get(i).getAsJsonObject();
                        Map<String, String> groupMap = new HashMap<>();
                        groupMap.put("groupName", group.has("groupName") ? group.get("groupName").getAsString() : "");
                        groupMap.put("groupID", group.has("groupID") ? group.get("groupID").getAsString() : "");
                        groupMap.put("username", group.has("owner") ? group.get("owner").getAsString() : "");

                        // Count members if available
                        if (group.has("receivers") && group.get("receivers").isJsonArray()) {
                            int memberCount = group.get("receivers").getAsJsonArray().size();
                            groupMap.put("memberCount", String.valueOf(memberCount));
                        } else {
                            groupMap.put("memberCount", "0");
                        }

                        groupsList.add(groupMap);
                    }

                    System.out.println("  → Parsed " + groupsList.size() + " groups");
                    model.updateGroupsList(groupsList);
                } catch (Exception e) {
                    System.err.println("✗ Error parsing groups data: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.err.println("✗ Model is null in onReceiveGroupsList");
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
        System.out.println("✓ Admin callback: Login successful - ENTERED CALLBACK");
        Platform.runLater(() -> {
            System.out.println("  → Platform.runLater executed for login success");

            if (model != null) {
                System.out.println("  → Notifying model of login success");
                model.notifyLoginSuccess();
            } else {
                System.err.println("  ✗ Model is null in callback!");
            }

            if (adminLoginController != null) {
                System.out.println("  → Notifying login controller");
                adminLoginController.handleSuccessfulLogin();
            } else {
                System.err.println("  ✗ AdminLoginController is null in callback!");
            }

            if (adminController != null) {
                System.out.println("  → Notifying admin controller");
                adminController.handleSuccessfulLogin();
            }

            System.out.println("✓ All login success notifications completed");
        });
    }

    @Override
    public void onFailedLogin() throws RemoteException {
        System.out.println("✗ Admin callback: Login failed - ENTERED CALLBACK");
        Platform.runLater(() -> {
            System.out.println("  → Platform.runLater executed for login failure");

            if (model != null) {
                System.out.println("  → Notifying model of login failure");
                model.notifyLoginFailure();
            } else {
                System.err.println("  ✗ Model is null in callback!");
            }

            if (adminLoginController != null) {
                System.out.println("  → Notifying login controller");
                adminLoginController.handleFailedLogin();
            } else {
                System.err.println("  ✗ AdminLoginController is null in callback!");
            }

            if (adminController != null) {
                adminController.handleFailedLogin();
            }

            System.out.println("✓ All login failure notifications completed");
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
            if (adminLoginController != null) {
                adminLoginController.handleSuccessfulCodeVerification();
            }
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