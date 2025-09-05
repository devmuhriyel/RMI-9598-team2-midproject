package ClientMVC.ClientView;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;

public class ClientViewConnectionController {

    @FXML
    private Text applicationName;

    @FXML
    private Button connectButton;

    @FXML
    private Pane inputFrame;

    @FXML
    private Label ipAddressLabel;

    @FXML
    private TextField ipAddressTextField;

    @FXML
    private Pane leftFrame;

    @FXML
    private Label portLabel;

    @FXML
    private TextField portTextField;
}
