package com.moscait.qwatcher.model;

public class Device {
    public String id;
    public String name;
    public String type;
    public String status;
    public String state;
    public long lastSeen;

    public Device() {
    }

    public Device(String id, String name, String type, String status, long lastSeen) {
        this(id, name, type, status, lastSeen, "UNKNOWN");
    }

    public Device(String id, String name, String type, String status, long lastSeen, String state) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.status = status;
        this.lastSeen = lastSeen;
        this.state = state;
    }
}
