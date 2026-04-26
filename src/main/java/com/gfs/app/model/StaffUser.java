package com.gfs.app.model;

public class StaffUser {
    private final long id;
    private final String username;
    private final String name;
    private final String title;
    private final String password;
    private final boolean active;

    

    public StaffUser(long id, String username, String name, String title, String password, boolean active) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.title = title;
        this.password = password;
        this.active = active;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getPassword() {
        return password;
    }

    public boolean isActive() {
        return active;
    }
}