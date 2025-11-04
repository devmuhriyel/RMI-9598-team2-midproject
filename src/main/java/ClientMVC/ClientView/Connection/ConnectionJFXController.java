package ClientMVC.ClientView.Connection;

import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ConnectionJFXController {

    @FXML
    private Button connectButton;
    @FXML
    private Label statusLabel;
    @FXML
    private TextField ipAddressTextField;
    @FXML
    private TextField portTextField;
    @FXML
    private Button minimizeButton, closeButton;
    @FXML
    private AnchorPane titleBar;

    private Runnable connectButtonListener;
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        applyInsetShadow(portTextField);
        applyInsetShadow(ipAddressTextField);
        applyRaisedShadow(connectButton);
        addHoverScale(connectButton, 1.05);

        connectButton.setOnAction(event -> {
            if (connectButtonListener != null) {
                connectButtonListener.run();
            }
        });

        // Drag the window when mouse is pressed and dragged
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

        // Frameless Button
        closeButton.setOnMouseClicked(event -> {
            ((Stage) closeButton.getScene().getWindow()).close();
        });

        minimizeButton.setOnMouseClicked(event -> {
            ((Stage) minimizeButton.getScene().getWindow()).setIconified(true);
        });
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


    public void showInvalidInputLabel() {
        statusLabel.setText("Invalid IP/PORT...");
        statusLabel.setTextFill(Color.rgb(217, 27, 36));
        statusLabel.setStyle("-fx-font-size: 12px;");
        addShakeAnimation();
    }

    public void showEmptyFieldsLabel() {
        statusLabel.setText("Please fill in all fields...");
        statusLabel.setTextFill(Color.rgb(255, 165, 0)); // Orange color
        statusLabel.setStyle("-fx-font-size: 12px;");
        addShakeAnimation();
    }

    public void showInvalidPortLabel() {
        statusLabel.setText("Port must be a valid number...");
        statusLabel.setTextFill(Color.rgb(217, 27, 36));
        statusLabel.setStyle("-fx-font-size: 12px;");
        addShakeAnimation();
    }

    // Helper method to reduce code duplication
    private void addShakeAnimation() {
        TranslateTransition shake = new TranslateTransition(Duration.millis(50), statusLabel);
        shake.setFromX(0);
        shake.setByX(10);
        shake.setCycleCount(4);
        shake.setAutoReverse(true);
        shake.play();
    }

    @FXML
    private void handleConnect() {
        if (connectButtonListener != null) {
            connectButtonListener.run();
        }
    }

    //SETTERS AND GETTERS - Now returns raw strings for model to validate
    public void setConnectButtonListener(Runnable listener) {
        this.connectButtonListener = listener;
    }

    public String getIpAddress() {
        return ipAddressTextField.getText(); // Return raw text, let model handle trimming
    }

    public String getPortText() {
        return portTextField.getText(); // Return raw text, let model handle validation
    }

    // Keep legacy method for backward compatibility, but not recommended
    @Deprecated
    public int getPort() throws NumberFormatException {
        return Integer.parseInt(portTextField.getText().trim());
    }

    public Button getConnectButton() {
        return connectButton;
    }

}