package Common.Objects.AdminObjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AdminAccountsList implements Serializable {
    private List<Admin> adminAccounts;

    public AdminAccountsList() {
        this.adminAccounts = new ArrayList<>();
    }

    public AdminAccountsList(List<Admin> admins) {
        this.adminAccounts = admins != null ? admins : new ArrayList<>();
    }

    public void setAdmins(List<Admin> admins) {
        this.adminAccounts = admins;
    }

    public List<Admin> getAdmins() {
        return adminAccounts;
    }
}