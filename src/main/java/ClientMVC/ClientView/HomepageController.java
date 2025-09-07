package ClientMVC.ClientView;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HomepageController {

    @FXML
    private Label welcomeLabel;

    // You could pass username via static variable or session
    public void initialize() {
        welcomeLabel.setText("Welcome to the Chat App!");
    }

    @FXML
    private void handleStartChatButton() {
        // TODO: Navigate to chat screen
        System.out.println("Navigating to chat screen...");
    }

    @FXML
    private void handleLogoutButton() {
        MainLogin.loadScene("Login.fxml", "Chat App - Login");
    }
}