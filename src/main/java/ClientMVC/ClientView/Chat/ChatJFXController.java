package ClientMVC.ClientView.Chat;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.AnchorPane;
import javafx.geometry.Pos;

public class ChatJFXController {
    @FXML
    private TextField messageField;
    @FXML
    private Button sendButton;
    @FXML
    private VBox messagesArea;

    @FXML
    private Button home;
    @FXML
    private Label label;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private Rectangle rectangle;

    @FXML
    private AnchorPane anchorPane;

    private Runnable sendButtonListener;
    private Runnable homeButtonListener;

    @FXML
    public void initialize() {
        System.out.println("FXController initialized");
        System.out.println("sendButton is " + (sendButton == null ? "null" : "ready"));

        if (sendButton != null) {
            sendButton.setOnAction(event -> {
                if (sendButtonListener != null) {
                    sendButtonListener.run();
                }
            });
        }
        
        if (home != null) {
            home.setOnAction(event -> {
                if (homeButtonListener != null) {
                    homeButtonListener.run();
                }
            });
        }
    }

    public void addMessage(String message) {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Label messageLabel = new javafx.scene.control.Label(message);
            messageLabel.setStyle("-fx-background-color: rgba(211, 211, 211, 0.7); -fx-padding: 8; -fx-background-radius: 10; -fx-text-fill: black; -fx-wrap-text: true;");
            messageLabel.setMaxWidth(400); // Limit message width for better display
            
            // Create HBox to align message to the right
            javafx.scene.layout.HBox messageContainer = new javafx.scene.layout.HBox();
            messageContainer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            messageContainer.getChildren().add(messageLabel);
            
            if (messagesArea != null) {
                messagesArea.getChildren().add(messageContainer);
                // Auto-scroll to bottom when new message is added
                messagesArea.layout();
                scrollPane.setVvalue(1.0);
            }
        });
    }
    
    public void clearMessages() {
        javafx.application.Platform.runLater(() -> {
            if (messagesArea != null) {
                messagesArea.getChildren().clear();
            }
        });
    }

    public void setSendButtonListener(Runnable sendButtonListener) {
        this.sendButtonListener = sendButtonListener;
    }
    
    public void setHomeButtonListener(Runnable homeButtonListener) {
        this.homeButtonListener = homeButtonListener;
    }

    // Getters for MVC access
    public TextField getMessageField() { return messageField; }
    public VBox getMessagesArea() { return messagesArea; }
    public Button getSendButton() { return sendButton; }
    public Button getHomeButton() { return home; }
}
