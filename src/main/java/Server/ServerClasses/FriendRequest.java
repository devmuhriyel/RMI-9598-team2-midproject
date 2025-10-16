package Server.ServerClasses;

public class FriendRequest {
    //VARIABLES
    private String requester;
    private String responder;
    private String status;

    //CONSTRUCTORS
    public FriendRequest(String rqstr, String rspndr, String stts) {
        this.requester = rqstr;
        this.responder = rspndr;
        this.status = stts;
    }

    //SETTERS
    public void setRequester(String rqstr) {
        this.requester = rqstr;
    }
    public void setResponder(String rspndr) {
        this.responder = rspndr;
    }
    public void setStatus(String stts) {
        this.status = stts;
    }

    //GETTERS
    public String getRequester() {
        return this.requester;
    }
    public String getResponder() {
        return this.responder;
    }
    public String getStatus() {
        return this.status;
    }
}
