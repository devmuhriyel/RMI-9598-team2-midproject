package ClientMVC;


public class ClientController {
    private ClientModel mdl;
    private ClientView vw;
    private FXController fController;

    public ClientController(ClientModel model, ClientView view, FXController uiElements) {
        this.mdl = model;
        this.vw = view;
        this.fController = uiElements;

        setupListeners();
        mdl.setMessageListener(msg -> {
            javafx.application.Platform.runLater(() -> {
                fController.addMessage(msg);
            });
        });
    }

    private void setupListeners(){
        fController.setSendButtonListener(() -> sendMSG());
    }

    private void sendMSG(){
        String textToSend = fController.getMessageField().getText();
        if (!textToSend.isEmpty()) {
            mdl.sendMSG(textToSend);
            fController.getMessageField().clear();
        }
    }
}
