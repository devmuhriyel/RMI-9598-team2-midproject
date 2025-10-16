package Server.ServerClasses.ClientObjects;

import java.util.List;

public class ClientAccountsList {
    private List<Client> clientAccounts;

    public ClientAccountsList(List<Client> clnt) {
        this.clientAccounts = clnt;
    }

    public void setUsers(List<Client> clnt) {
        this.clientAccounts = clnt;
    }

    public List<Client> getClients() {
        return clientAccounts;
    }
}
