package Common.Objects.LogObjects;

import java.io.Serializable;
import java.util.List;

public class ResponsesList implements Serializable
{
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
