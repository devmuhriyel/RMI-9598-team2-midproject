package Client.View;

import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
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
}

