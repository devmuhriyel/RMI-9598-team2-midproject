package ClientMVC.ClientView.Home;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class HomeJFXController {
    
    @FXML
    private Button userProfileButton;
    
    @FXML
    private VBox messagesArea; // "All" messages area
    
    @FXML
    private VBox messagesArea1; // "Groups" messages area
    
    @FXML
    private Label allLabel;
    
    @FXML
    private Label groupsLabel;
    
    @FXML
    private ImageView logoImage;
    
    @FXML
    private Label userLabel;
    
    private Runnable userProfileButtonListener;
    
    @FXML
    private void initialize() {
        System.out.println("HomeJFXController initialized");
        
        // Set up user profile button listener
        if (userProfileButton != null) {
            userProfileButton.setOnAction(event -> {
                if (userProfileButtonListener != null) {
                    userProfileButtonListener.run();
                }
            });
        }
    }

    // Getters for MVC access
    public Button getUserProfileButton() {
        return userProfileButton;
    }

    
    // Method to update user label with username
    public void setUserLabel(String username) {
        if (userLabel != null) {
            userLabel.setText(username);
        }
    }
}
