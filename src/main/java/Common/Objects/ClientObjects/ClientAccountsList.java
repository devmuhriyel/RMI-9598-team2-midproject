package Common.Objects.ClientObjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ClientAccountsList implements Serializable {
    private List<Client> clientAccounts;

    public ClientAccountsList() {
        this.clientAccounts = new ArrayList<>();
    }

    public ClientAccountsList(List<Client> clients) {
        this.clientAccounts = clients != null ? clients : new ArrayList<>();
    }

    public void setClients(List<Client> clients) {
        this.clientAccounts = clients;
    }

    public List<Client> getClients() {
        return clientAccounts;
    }
}