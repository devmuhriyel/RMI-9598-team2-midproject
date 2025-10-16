package Server.ServerClasses.LogObjects;

import Server.ServerClasses.AdminObjects.Admin;

import java.util.List;

public class LogsList {
    private List<Requests> requests;
    private List<Responses> responses;

    public LogsList(List<Requests> requests, List<Responses> responses) {
        this.requests = requests;
        this.responses = responses;
    }

    //SETTERS
    public void setRequests(List<Requests> requests) {
        this.requests = requests;

    }
    public void setResponses(List<Responses> responses) {
        this.responses = responses;
    }

    //GETTERS
    public List<Requests> getRequests() {
        return requests;
    }
    public List<Responses> getResponses() {
        return responses;
    }

}
