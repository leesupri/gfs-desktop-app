package com.gfs.app.model;

import java.time.LocalDateTime;

public class ActivityLog {

    private final long          id;
    private final long          userId;
    private final String        username;
    private final String        action;
    private final String        description;
    private final String        ip;
    private final LocalDateTime createdAt;

    public ActivityLog(long id, long userId, String username,
                       String action, String description, String ip,
                       LocalDateTime createdAt) {
        this.id          = id;
        this.userId      = userId;
        this.username    = username;
        this.action      = action;
        this.description = description;
        this.ip          = ip;
        this.createdAt   = createdAt;
    }

    public long          getId()          { return id; }
    public long          getUserId()      { return userId; }
    public String        getUsername()    { return username; }
    public String        getAction()      { return action; }
    public String        getDescription() { return description != null ? description : ""; }
    public String        getIp()          { return ip          != null ? ip          : ""; }
    public LocalDateTime getCreatedAt()   { return createdAt; }

    /** Formatted timestamp for display in the table. */
    public String getCreatedAtFormatted() {
        return createdAt != null
            ? createdAt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            : "";
    }
}