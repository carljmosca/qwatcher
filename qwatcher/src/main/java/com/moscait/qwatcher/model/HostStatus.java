package com.moscait.qwatcher.model;

import java.util.List;

public class HostStatus {
    public long uptimeSeconds;
    public boolean internetAvailable;
    public List<Device> devices;

    public HostStatus() {
    }

    public HostStatus(long uptimeSeconds, boolean internetAvailable, List<Device> devices) {
        this.uptimeSeconds = uptimeSeconds;
        this.internetAvailable = internetAvailable;
        this.devices = devices;
    }
}
