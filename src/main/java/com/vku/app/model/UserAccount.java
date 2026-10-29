package com.vku.app.model;

public class UserAccount {
    private String username;
    private String email;
    private String password;

    public UserAccount() {
    }

    public UserAccount(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFolderPrefix() {
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf("@"));
        }
        return username;
    }
}

