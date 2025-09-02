package ClientMVC;

public class ClientController {
    private ClientModel mdl;
    private ClientView vw;

    public ClientController(ClientModel model, ClientView view) {
        this.mdl = model;
        this.vw = view;
    }


}
