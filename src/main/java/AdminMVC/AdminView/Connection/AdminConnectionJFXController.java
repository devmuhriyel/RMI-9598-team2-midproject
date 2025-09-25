package AdminMVC.AdminView.Connection;

import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

public class AdminConnectionJFXController {

    @FXML private AnchorPane parent;
    @FXML private AnchorPane connectionFrame;
    @FXML private AnchorPane leftFrame;
    @FXML private AnchorPane inputFrame;
    @FXML private AnchorPane titleBar;
    @FXML private ImageView logoImage;
    @FXML private ImageView backgroundImage;
    @FXML private TextField ipAddressTextField;
    @FXML private TextField portTextField;
    @FXML private Label ipAddressLabel;
    @FXML private Label portLabel;
    @FXML private Label statusLabel;
    @FXML private Button connectButton;
    @FXML private Button closeButton;
    @FXML private Button minimizeButton;

    private Runnable connectButtonListener;
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        if (portTextField != null) applyInsetShadow(portTextField);
        if (ipAddressTextField != null) applyInsetShadow(ipAddressTextField);
        if (connectButton != null) {
            applyRaisedShadow(connectButton);
            addHoverScale(connectButton, 1.05);
        }

        if (connectButton != null) {
            connectButton.setOnAction(event -> {
                if (connectButtonListener != null) {
                    connectButtonListener.run();
                }
            });
        }

        if (titleBar != null) {
            titleBar.setOnMousePressed(event -> {
                Stage stage = (Stage) titleBar.getScene().getWindow();
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });
            titleBar.setOnMouseDragged(event -> {
                Stage stage = (Stage) titleBar.getScene().getWindow();
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            });
        }

        if (closeButton != null) {
            closeButton.setOnMouseClicked(event -> {
                ((Stage) closeButton.getScene().getWindow()).close();
            });
        }

        if (minimizeButton != null) {
            minimizeButton.setOnMouseClicked(event -> {
                ((Stage) minimizeButton.getScene().getWindow()).setIconified(true);
            });
        }

        if (ipAddressTextField != null) {
            ipAddressTextField.setText("127.0.0.1");
        }
        if (portTextField != null) {
            portTextField.setText("1234");
        }
    }

    private void applyInsetShadow(TextField tf) {
        InnerShadow innerDark = new InnerShadow();
        innerDark.setRadius(10);
        innerDark.setOffsetX(3);
        innerDark.setOffsetY(3);
        innerDark.setColor(Color.rgb(0, 0, 0, 0.55));

        InnerShadow innerLight = new InnerShadow();
        innerLight.setRadius(10);
        innerLight.setOffsetX(-3);
        innerLight.setOffsetY(-3);
        innerLight.setColor(Color.rgb(255, 255, 255, 0.10));

        innerLight.setInput(innerDark);
        tf.setEffect(innerLight);
    }

    private void applyRaisedShadow(Button btn) {
        DropShadow dark = new DropShadow();
        dark.setRadius(18);
        dark.setOffsetX(6);
        dark.setOffsetY(6);
        dark.setColor(Color.rgb(0, 0, 0, 0.55));

        DropShadow light = new DropShadow();
        light.setRadius(18);
        light.setOffsetX(-6);
        light.setOffsetY(-6);
        light.setColor(Color.rgb(255, 255, 255, 0.10));

        light.setInput(dark);
        btn.setEffect(light);
    }

    private void addHoverScale(Button btn, double scale) {
        ScaleTransition enter = new ScaleTransition(Duration.millis(50), btn);
        enter.setToX(scale);
        enter.setToY(scale);

        ScaleTransition exit = new ScaleTransition(Duration.millis(50), btn);
        exit.setToX(1);
        exit.setToY(1);

        btn.setOnMouseEntered(e -> enter.playFromStart());
        btn.setOnMouseExited(e -> exit.playFromStart());
    }

    public void showEmptyFieldsLabel() {
        if (statusLabel != null) {
            statusLabel.setText("Please fill in all fields...");
            statusLabel.setTextFill(Color.rgb(255, 165, 0)); // Orange color
            statusLabel.setStyle("-fx-font-size: 12px;");
            addShakeAnimation();
        }
    }

    public void showInvalidPortLabel() {
        if (statusLabel != null) {
            statusLabel.setText("Port must be a valid number...");
            statusLabel.setTextFill(Color.rgb(217, 27, 36));
            statusLabel.setStyle("-fx-font-size: 12px;");
            addShakeAnimation();
        }
    }

    public void showConnectionFailedLabel() {
        if (statusLabel != null) {
            statusLabel.setText("Connection failed...");
            statusLabel.setTextFill(Color.rgb(217, 27, 36));
            statusLabel.setStyle("-fx-font-size: 12px;");
            addShakeAnimation();
        }
    }

    public void showInvalidPortRangeLabel() {
        if (statusLabel != null) {
            statusLabel.setText("Port must be between 1024-65535...");
            statusLabel.setTextFill(Color.rgb(217, 27, 36));
            statusLabel.setStyle("-fx-font-size: 12px;");
            addShakeAnimation();
        }
    }

    public void showInvalidIpFormatLabel() {
        if (statusLabel != null) {
            statusLabel.setText("Invalid IP address format...");
            statusLabel.setTextFill(Color.rgb(217, 27, 36));
            statusLabel.setStyle("-fx-font-size: 12px;");
            addShakeAnimation();
        }
    }

    private void addShakeAnimation() {
        if (statusLabel != null) {
            TranslateTransition shake = new TranslateTransition(Duration.millis(50), statusLabel);
            shake.setFromX(0);
            shake.setByX(10);
            shake.setCycleCount(4);
            shake.setAutoReverse(true);
            shake.play();
        }
    }

    @FXML
    private void handleConnect() {
        System.out.println("[DEBUG] handleConnect() triggered from FXML.");
        if (connectButtonListener != null) {
            connectButtonListener.run();
        }
    }

    public String getIpAddress() {
        return ipAddressTextField != null ? ipAddressTextField.getText() : "";
    }

    public String getPortText() {
        return portTextField != null ? portTextField.getText() : "";
    }

    public Button getConnectButton() {
        return connectButton;
    }

    public AnchorPane getParent() { return parent; }

    // Setter for MVC pattern
    public void setConnectButtonListener(Runnable listener) {
        this.connectButtonListener = listener;
    }
}