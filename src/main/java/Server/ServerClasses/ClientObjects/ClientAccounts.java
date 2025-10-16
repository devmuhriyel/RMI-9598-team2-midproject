package Server.ServerClasses.ClientObjects;

import java.util.List;

public class ClientAccounts {
    private List<Client> clientAccounts;

    public ClientAccounts(List<Client> clnt) {
        this.clientAccounts = clnt;
    }

    public void setUsers(List<Client> clnt) {
        this.clientAccounts = clnt;
    }

    public List<Client> getClients() {
        return clientAccounts;
    }
}
