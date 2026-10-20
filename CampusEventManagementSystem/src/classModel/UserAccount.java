/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package classModel;

/**
 *
 * @author bernard_07
 */
public class UserAccount 
{
    private String username;
    private String password;
    private String role; // "Organizer", "Participant", or "Administrator"

    public UserAccount(String username, String password, String role) 
    {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }

    // Check if the credentials and selected role match this account
    public boolean matches(String username, String password, String role) 
    {
        return this.username.equalsIgnoreCase(username.trim()) 
            && this.password.equals(password) 
            && this.role.equalsIgnoreCase(role);
    }
}
