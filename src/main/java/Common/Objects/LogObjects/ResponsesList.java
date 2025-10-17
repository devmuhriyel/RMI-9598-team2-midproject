package Common.Objects.LogObjects;

import java.util.List;

public class ResponsesList {
    //VARIABLES
    private List<Requests> requestsList;

    //CONSTRUCTORS
    public ResponsesList(List<Requests> requestsList) {
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
