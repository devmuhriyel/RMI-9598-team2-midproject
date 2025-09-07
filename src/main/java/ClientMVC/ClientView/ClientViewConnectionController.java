package ClientMVC.ClientView;

import ClientMVC.ClientController.ClientControllerConnection;
import ClientMVC.NetworkManager;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

public class ClientViewConnectionController {

    @FXML
    private Text applicationName;
    @FXML
    private Button connectButton;
    @FXML
    private Label ipAddressLabel;
    @FXML
    private Label portLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private TextField ipAddressTextField;
    @FXML
    private TextField portTextField;
    @FXML
    private Label invalidInputLabel, connectedLabel;
    private NetworkManager networkManager;

    @FXML
    public void initialize() {
        applyInsetShadow(portTextField);
        applyInsetShadow(ipAddressTextField);

        // Raised (drop) shadow for the button
        applyRaisedShadow(connectButton);

        // Nice hover micro-interaction (optional)
        addHoverScale(connectButton, 1.05);
    }

    private void applyInsetShadow(TextField tf) {
        // Dark inner shadow bottom-right
        InnerShadow innerDark = new InnerShadow();
        innerDark.setRadius(10);
        innerDark.setOffsetX(3);
        innerDark.setOffsetY(3);
        innerDark.setColor(Color.rgb(0, 0, 0, 0.55));

        // Light inner highlight top-left
        InnerShadow innerLight = new InnerShadow();
        innerLight.setRadius(10);
        innerLight.setOffsetX(-3);
        innerLight.setOffsetY(-3);
        innerLight.setColor(Color.rgb(255, 255, 255, 0.10));

        // Chain: light uses dark as input so both apply
        innerLight.setInput(innerDark);
        tf.setEffect(innerLight);
    }

    private void applyRaisedShadow(Button btn) {
        // Dark drop shadow bottom-right
        DropShadow dark = new DropShadow();
        dark.setRadius(18);
        dark.setOffsetX(6);
        dark.setOffsetY(6);
        dark.setColor(Color.rgb(0, 0, 0, 0.55));

        // Light drop shadow top-left (very subtle on dark backgrounds)
        DropShadow light = new DropShadow();
        light.setRadius(18);
        light.setOffsetX(-6);
        light.setOffsetY(-6);
        light.setColor(Color.rgb(255, 255, 255, 0.10));

        // Chain
        light.setInput(dark);
        btn.setEffect(light);
    }

    private void addHoverScale(Button btn, double scale) {
        ScaleTransition enter = new ScaleTransition(Duration.millis(50), btn);
        enter.setToX(scale); enter.setToY(scale);

        ScaleTransition exit = new ScaleTransition(Duration.millis(50), btn);
        exit.setToX(1); exit.setToY(1);

        btn.setOnMouseEntered(e -> enter.playFromStart());
        btn.setOnMouseExited(e -> exit.playFromStart());
    }

    private void invalidInputLabel (Label statusLabel) {
        statusLabel.setText("Invalid IP/PORT...");
        statusLabel.setTextFill(Color.web("#A1D8B5"));
        statusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
    }

    private void connectedLabel (Label statusLabel) {
        statusLabel.setText("Connected Successfully...");
        statusLabel.setTextFill(Color.web("#D91B24"));
        statusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
    }

    public Label getStatusLabel() {
        return connectedLabel;
    }

    public Label getInvalidInputLabel() {
        return  invalidInputLabel;
    }


    //Flores

    // Getters so the MVC Controller can use them
    public String getIpAddress() {
        return ipAddressTextField.getText();
    }

    public String getPort() {
        return portTextField.getText();
    }

    public Button getConnectButton() {
        return connectButton;
    }

    // This method is just a hook — actual logic is in MVC Controller
    @FXML
    private void onConnect() {
        // Delegates handling to MVC Controller later
        if (onConnectHandler != null) {
            onConnectHandler.run();
        }
    }

    // Trick: let MVC Controller inject its handler
    private Runnable onConnectHandler;
    public void setOnConnectHandler(Runnable handler) {
        this.onConnectHandler = handler;
    }

    private ClientControllerConnection controller;

    public void setController(ClientControllerConnection controller) {
        this.controller = controller;
    }

    @FXML
    private void handleConnect() {
        String ip = ipAddressTextField.getText();

        int port;
        try {
            port = Integer.parseInt(portTextField.getText());
        } catch (NumberFormatException e) {
            invalidInputLabel(statusLabel); // show invalid input error
            return;
        }

        if (controller != null) {
            boolean success = controller.connectToServer(ip, port);
            if (success) {
                // Connected
                statusLabel.setText("Connected Successfully...");
                statusLabel.setTextFill(Color.web("#A1D8B5")); // green shade
                statusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                statusLabel.setTextAlignment(TextAlignment.CENTER);
            } else {
                // Failed
                statusLabel.setText("Invalid IP/PORT...");
                statusLabel.setTextFill(Color.web("#D91B24")); // red shade
                statusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                statusLabel.setTextAlignment(TextAlignment.CENTER);
            }
        } else {
            statusLabel.setText("Controller not set!");
        }
    }


    public void setNetworkManager(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

}

