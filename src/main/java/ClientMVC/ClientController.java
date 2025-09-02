package ClientMVC;

public class ClientController {
    private ClientModel mdl;
    private ClientView vw;

    public ClientController(ClientModel model, ClientView view) {
        this.mdl = model;
        this.vw = view;

        setupListeners();
    }

    private void setupListeners(){
        vw.getSendButton().setOnAction(event -> {
            System.out.println("Send");
            String textToSend = vw.getMessageField().getText();
            if(!textToSend.isEmpty()){
                mdl.sendMSG(textToSend);
                vw.getMessageField().setText("");
            }
        });
    }
    private void sendMSG(String msg){

    }
}
