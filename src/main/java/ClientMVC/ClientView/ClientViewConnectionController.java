package ClientMVC.ClientView;

import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class ClientViewConnectionController {

    @FXML
    private Text applicationName;
    @FXML
    private Button connectButton;
    @FXML
    private Label statusLabel;
    @FXML
    private TextField ipAddressTextField;
    @FXML
    private TextField portTextField;

    private Runnable connectButtonListener;

    @FXML
    public void initialize() {
        applyInsetShadow(portTextField);
        applyInsetShadow(ipAddressTextField);
        applyRaisedShadow(connectButton);
        addHoverScale(connectButton, 1.05);

        // Delegate click to listener (if set by Controller)
        connectButton.setOnAction(event -> {
            if (connectButtonListener != null) {
                connectButtonListener.run();
            }
        });
    }

    // --- Styling helpers ---
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
    @FXML
    private void handleConnect() {
        System.out.println("[DEBUG] handleConnect() triggered from FXML.");
        if (connectButtonListener != null) {
            connectButtonListener.run();
        }

    }

    // --- Listener injection ---
    public void setConnectButtonListener(Runnable listener) {
        this.connectButtonListener = listener;
    }

    // --- Getters for Controller ---
    public String getIpAddress() {
        return ipAddressTextField.getText();
    }

    public int getPort() {
        return Integer.parseInt(portTextField.getText());
    }

    public Label getStatusLabel() {
        return statusLabel;
    }

    public Button getConnectButton() {
        return connectButton;
    }
}
