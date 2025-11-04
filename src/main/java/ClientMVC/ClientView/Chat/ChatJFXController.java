package ClientMVC.ClientView.Chat;

import ClientMVC.ClientModel.UnifiedClientModel;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.AnchorPane;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import javafx.geometry.Insets;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.util.function.Consumer;


public class ChatJFXController {
    @FXML
    private TextField messageField;
    @FXML
    private Button sendButton;
    @FXML
    private Button fileButton;
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
    private Runnable fileButtonListener;
    private Consumer<String> fileOpenListener;
    private Runnable sendButtonListener;
    private Runnable homeButtonListener;

    @FXML
    public void initialize() {
        System.out.println("ChatJFXController initialize() called");
        System.out.println("File button is: " + (fileButton != null ? "NOT NULL" : "NULL"));

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

        if (fileButton != null) {
            System.out.println("Setting up file button listener");
            fileButton.setOnAction(event -> {
                System.out.println("File button clicked!");
                if (fileButtonListener != null) {
                    System.out.println("File button listener is not null, calling it");
                    fileButtonListener.run();
                } else {
                    System.out.println("File button listener is NULL!");
                }
            });
        }

        if (messagesArea != null && scrollPane != null) {
            messagesArea.heightProperty().addListener((observable, oldValue, newValue) -> {
                Platform.runLater(() -> {
                    scrollPane.setVvalue(1.0);
                });
            });
            messagesArea.getChildren().addListener(new ListChangeListener<javafx.scene.Node>() {
                @Override
                public void onChanged(ListChangeListener.Change<? extends Node> change) {
                    while (change.next()) {
                        if (change.wasAdded()) {
                            Platform.runLater(() -> {
                                scrollPane.setVvalue(1.0);
                            });
                        }
                    }
                }
            });
        }
    }

    public void handleFileSave(String fileName, String fileData, UnifiedClientModel model) {
        Platform.runLater(() -> {
            try {
                // Create a file chooser for save location
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save File");
                fileChooser.setInitialFileName(fileName);

                // Set extension filter based on file name
                String extension = getFileExtension(fileName);
                if (!extension.isEmpty()) {
                    FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(
                            extension.toUpperCase() + " Files", "*." + extension
                    );
                    fileChooser.getExtensionFilters().add(filter);
                }
                fileChooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("All Files", "*.*")
                );

                File saveLocation = fileChooser.showSaveDialog(messageField.getScene().getWindow());

                if (saveLocation == null) {
                    System.out.println("File save cancelled by user");
                    return;
                }

                // Decode and save the file
                model.saveReceivedFile(
                        fileName,
                        fileData,
                        saveLocation.getParent()
                );

                // Rename the saved file to match user's choice
                File tempFile = new File(saveLocation.getParent(), fileName);
                if (!saveLocation.getName().equals(fileName)) {
                    tempFile.renameTo(saveLocation);
                }

                System.out.println("✓ File saved: " + saveLocation.getAbsolutePath());
                showSuccess("File saved successfully!");

                // Ask if user wants to open the file
                askToOpenFile(saveLocation);

            } catch (Exception e) {
                System.err.println("Error saving file: " + e.getMessage());
                e.printStackTrace();
                showError("Failed to save file: " + e.getMessage());
            }
        });
    }

    private void askToOpenFile(File file) {
        if (!Desktop.isDesktopSupported()) {
            return;
        }

        Alert confirmOpen = new Alert(Alert.AlertType.CONFIRMATION);
        confirmOpen.setTitle("Open File");
        confirmOpen.setHeaderText(null);
        confirmOpen.setContentText("Would you like to open the file?");

        confirmOpen.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    Desktop.getDesktop().open(file);
                } catch (Exception e) {
                    System.err.println("Failed to open file: " + e.getMessage());
                    showError("Could not open file: " + e.getMessage());
                }
            }
        });
    }

    public void addMessageWithSender(String senderName, String message, boolean isSender) {
        Platform.runLater(() -> {
            double maxBubbleWidth = 580.0 / 3.0;
            Label senderLabel = new Label(senderName);
            senderLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666; -fx-font-weight: bold;");
            senderLabel.setPadding(new Insets(0, 15, 2, 15));
            boolean isFileMessage = message.startsWith("[FILE: ") && message.endsWith("]");
            HBox messageContainer = new HBox();
            messageContainer.setPadding(new Insets(5, 10, 5, 10));
            messageContainer.setFillHeight(false);
            messageContainer.setAlignment(Pos.TOP_CENTER);
            VBox messageWithSender = new VBox(2);
            messageWithSender.setAlignment(isSender ? Pos.TOP_RIGHT : Pos.TOP_LEFT);
            if (isFileMessage) {
                String fileName = message.substring(7, message.length() - 1);
                VBox fileContainer = new VBox(5);
                fileContainer.setPadding(new Insets(10, 15, 10, 15));
                fileContainer.setMaxWidth(maxBubbleWidth);
                Label fileIcon = new Label("📎");
                fileIcon.setStyle("-fx-font-size: 20px;");
                Label fileNameLabel = new Label(fileName);
                fileNameLabel.setWrapText(true);
                fileNameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
                Button openFileButton = new Button("Open File");
                openFileButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5;");
                openFileButton.setOnAction(event -> {
                    if (fileOpenListener != null) {
                        fileOpenListener.accept(fileName);
                    }
                });
                fileContainer.getChildren().addAll(fileIcon, fileNameLabel, openFileButton);
                fileContainer.setStyle(isSender ?
                                "-fx-background-color: #007AFF; -fx-text-fill: white; -fx-background-radius: 18;" :
                                "-fx-background-color: #E5E5EA; -fx-text-fill: black; -fx-background-radius: 18;"
                );
                messageWithSender.getChildren().addAll(senderLabel, fileContainer);
            } else {
                Label messageLabel = new Label(message);
                messageLabel.setWrapText(true);
                messageLabel.setMaxWidth(maxBubbleWidth);
                messageLabel.setPrefWidth(Region.USE_COMPUTED_SIZE);
                messageLabel.setMinHeight(Region.USE_PREF_SIZE);
                messageLabel.setPadding(new Insets(10, 15, 10, 15));
                messageLabel.setStyle(isSender ?
                        "-fx-background-color: #007AFF; -fx-text-fill: white; -fx-background-radius: 18; -fx-font-size: 14px;" :
                        "-fx-background-color: #E5E5EA; -fx-text-fill: black; -fx-background-radius: 18; -fx-font-size: 14px;"
                );
                messageWithSender.getChildren().addAll(senderLabel, messageLabel);
            }   messageWithSender.setAlignment(isSender ? Pos.TOP_RIGHT : Pos.TOP_LEFT);
            if (isSender) {
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                messageContainer.getChildren().addAll(spacer, messageWithSender);
            } else {
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                messageContainer.getChildren().addAll(messageWithSender, spacer);
            }
            if (messagesArea != null) {
                messagesArea.getChildren().add(messageContainer);
                forceScrollToBottom();
            }
        });
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    public void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void showSuccess(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public File showFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Send");
        fileChooser.getExtensionFilters().addAll(

       new FileChooser.ExtensionFilter("All Files", "*.*"),
       new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
       new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.doc", "*.docx", "*.txt", "*.rtf"),
       new FileChooser.ExtensionFilter("Archives", "*.zip", "*.rar", "*.7z")
       );
        return fileChooser.showOpenDialog(messageField.getScene().getWindow());
    }

    private void forceScrollToBottom() {
        Platform.runLater(() -> {
            if (scrollPane != null) {
                scrollPane.setVvalue(1.0);
                Platform.runLater(() -> {
                    scrollPane.setVvalue(1.0);
                });
            }
        });
    }

    public void addChatMessage(String senderName, String message, String currentUser) {
        boolean isSender = senderName.equals(currentUser);
        addMessageWithSender(senderName, message, isSender);
    }

    public void clearMessages() {
        Platform.runLater(() -> {
            if (messagesArea != null) {
                messagesArea.getChildren().clear();
            }
        });
    }

    public void closeWindow() {
        Platform.runLater(() -> {
            Stage stage = (Stage) home.getScene().getWindow();
            stage.close();
        });
    }

    public void setFileButtonListener(Runnable fileButtonListener) {
        System.out.println("Setting file button listener: " + (fileButtonListener != null ? "NOT NULL" : "NULL"));
        this.fileButtonListener = fileButtonListener;
    }

    public void setFileOpenListener(Consumer<String> fileOpenListener) {
        this.fileOpenListener = fileOpenListener;
    }

    public void setSendButtonListener(Runnable sendButtonListener) {
        this.sendButtonListener = sendButtonListener;
    }

    public void setHomeButtonListener(Runnable homeButtonListener) {
        this.homeButtonListener = homeButtonListener;
    }

    public void setLabel(String label) {
        this.label.setText(label);
    }

    public TextField getMessageField() {
        return messageField;
    }

    public Button getHomeButton() {
        return home;
    }

    public Label getReceiver() {
        return label;
    }
}