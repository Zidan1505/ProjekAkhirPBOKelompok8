package org.example.hotelsystem.model;

public class Employee {
    private int id;
    private String username;
    private String userPassword;
    private String fullname;
    private String email;
    private String contact;
    private String statusEmployee;

    public Employee(int id, String username, String userPassword, String fullname, String email, String contact, String statusEmployee) {
        this.id = id;
        this.username = username;
        this.userPassword = userPassword;
        this.fullname = fullname;
        this.email = email;
        this.contact = contact;
        this.statusEmployee = statusEmployee;
    }

    // Getters
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getUserPassword() { return userPassword; }
    public String getFullname() { return fullname; }
    public String getEmail() { return email; }
    public String getContact() { return contact; }
    public String getStatusEmployee() { return statusEmployee; }
}