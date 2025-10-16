package Server.ServerClasses.AdminObjects;

public class Admin {
    //VARIABLES
    private String username;
    private String password;
    private String verificationCode;


    //CONSTRUCTORS
    public Admin(String uName, String pWord, String vCode) {
        this.username = uName;
        this.password = pWord;
        this.verificationCode = vCode;
    }

    //SETTERS
    public void setUsername(String username) {
        this.username = username;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setVerificationCode(String vCode) {
        this.verificationCode = vCode;
    }

    //GETTERS
    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
    public String getVerificationCode() {
        return verificationCode;
    }

}
