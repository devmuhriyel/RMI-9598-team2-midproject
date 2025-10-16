package Server.ServerClasses.AdminObjects;

import java.util.List;

public class AdminAccountsList {
    private List<Admin> adminAccounts;

    public AdminAccountsList(List<Admin> admins) {
        this.adminAccounts = admins;
    }

    public void setAdmins(List<Admin> admins) {
        this.adminAccounts = admins;
    }

    public List<Admin> getAdmins() {
        return adminAccounts;
    }
}
