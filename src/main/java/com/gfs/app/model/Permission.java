package com.gfs.app.model;

public class Permission {
    private long id;
    private String code;
    private String description;
    public Permission() {}
    public Permission(long id, String code, String description) { this.id = id; this.code = code; this.description = description; }
    // getters and setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}