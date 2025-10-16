package Server.ServerClasses.LogObjects;

import java.util.List;

public class RequestsList {
    //VARIABLES
    private List<Requests> requestsList;

    //CONSTRUCTORS
    public RequestsList(List<Requests> requestsList) {
        this.requestsList = requestsList;
    }

    //SETTERS
    public void setRequestsList(List<Requests> requestsList) {
        this.requestsList = requestsList;
    }

    //GETTERS
    public List<Requests> getRequestsList() {
        return requestsList;
    }
}
