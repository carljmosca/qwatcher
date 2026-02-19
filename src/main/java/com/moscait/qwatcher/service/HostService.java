package com.moscait.qwatcher.service;

import com.moscait.qwatcher.model.Device;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

@ApplicationScoped
public class HostService {

    @Inject
    BluetoothService bluetoothService;

    public long getUptime() {
        return ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
    }

    public boolean isInternetAvailable() {
        try {
            InetAddress.getByName("google.com");
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    public List<Device> getDevices() {
        return bluetoothService.getDevices();
    }
}
