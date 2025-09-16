package ClientMVC.ClientModel;

public class AdminModel {

    public void search(String keyword) {
        System.out.println("Searching for: " + keyword);
        // Later: hook up to database or XML
    }

    public void editUser(String username, String newPassword) {
        System.out.println("Editing user: " + username + ", new password: " + newPassword);
        // Later: update persistence
    }

    public void deleteUser(String username) {
        System.out.println("Deleting user: " + username);
        // Later: remove from persistence
    }
}
