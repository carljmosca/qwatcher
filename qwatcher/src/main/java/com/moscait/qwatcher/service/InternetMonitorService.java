package com.moscait.qwatcher.service;

import com.moscait.qwatcher.model.MonitorEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class InternetMonitorService {

    private static final Logger LOG = Logger.getLogger(InternetMonitorService.class);

    @Inject
    HostService hostService;

    @Inject
    BluetoothService bluetoothService;

    @ConfigProperty(name = "qwatcher.monitor.internet.offline-threshold-minutes", defaultValue = "5")
    int configOfflineThresholdMinutes;

    @ConfigProperty(name = "qwatcher.monitor.internet.power-cycle-delay-minutes", defaultValue = "2")
    int configPowerCycleDelayMinutes;

    @ConfigProperty(name = "qwatcher.monitor.internet.target-device-id", defaultValue = "")
    String configTargetDeviceId;

    // Runtime configurable settings
    private int currentOfflineThreshold;
    private int currentPowerCycleDelay;
    private String currentTargetDeviceId;

    private Instant lastOnlineTime = Instant.now();
    private Instant deviceTurnedOffAt = null;
    private boolean deviceCurrentlyOff = false;

    // Circular buffer for events, synchronized for thread safety
    private final List<MonitorEvent> eventLog = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_LOG_SIZE = 100;

    @PostConstruct
    void init() {
        this.currentOfflineThreshold = configOfflineThresholdMinutes;
        this.currentPowerCycleDelay = configPowerCycleDelayMinutes;
        this.currentTargetDeviceId = configTargetDeviceId;
        logEvent(MonitorEvent.TYPE_INFO, "Monitor initialized. Threshold: " + currentOfflineThreshold + "m, Delay: "
                + currentPowerCycleDelay + "m");
    }

    private void logEvent(String type, String message) {
        synchronized (eventLog) {
            MonitorEvent event = new MonitorEvent(System.currentTimeMillis(), type, message);
            eventLog.add(0, event); // Add to head
            if (eventLog.size() > MAX_LOG_SIZE) {
                eventLog.remove(eventLog.size() - 1);
            }
        }

        // Log to system log as well
        if (MonitorEvent.TYPE_ERROR.equals(type)) {
            LOG.error(message);
        } else if (MonitorEvent.TYPE_WARNING.equals(type)) {
            LOG.warn(message);
        } else {
            LOG.info(message);
        }
    }

    @Scheduled(cron = "{qwatcher.monitor.internet.check-cron:0 */5 * * * ?}")
    void checkInternetAndManageDevice() {
        try {
            boolean internetAvailable = hostService.isInternetAvailable();
            Instant now = Instant.now();

            if (internetAvailable) {
                lastOnlineTime = now;
            }

            if (deviceCurrentlyOff && deviceTurnedOffAt != null) {
                // Device is OFF. Check if it's time to turn it ON.
                // This happens regardless of internet status (since router is off, internet is
                // likely off)
                long minutesOff = (now.getEpochSecond() - deviceTurnedOffAt.getEpochSecond()) / 60;

                if (minutesOff >= currentPowerCycleDelay) {
                    logEvent(MonitorEvent.TYPE_INFO,
                            "Power cycle delay elapsed (" + minutesOff + "m). Turning device ON.");
                    turnDeviceOn();

                    // Reset lastOnlineTime to NOW to give a grace period for the router/modem to
                    // boot up.
                    // Otherwise, the very next check might find it "offline for > threshold" and
                    // turn it off again.
                    lastOnlineTime = now;
                }
            } else {
                // Device is ON. Monitoring internet.
                if (!internetAvailable) {
                    long minutesOffline = (now.getEpochSecond() - lastOnlineTime.getEpochSecond()) / 60;

                    if (minutesOffline >= currentOfflineThreshold) {
                        logEvent(MonitorEvent.TYPE_WARNING, "Internet offline for " + minutesOffline + "m (threshold: "
                                + currentOfflineThreshold + "m). Turning device OFF.");
                        turnDeviceOff();
                    }
                }
            }

        } catch (Exception e) {
            logEvent(MonitorEvent.TYPE_ERROR, "Error in monitor task: " + e.getMessage());
        }
    }

    private void turnDeviceOff() {
        if (currentTargetDeviceId == null || currentTargetDeviceId.isBlank()) {
            logEvent(MonitorEvent.TYPE_WARNING, "No target device configured for power cycle.");
            return;
        }

        try {
            bluetoothService.controlDevice(currentTargetDeviceId, "off");
            deviceCurrentlyOff = true;
            deviceTurnedOffAt = Instant.now();
            logEvent(MonitorEvent.TYPE_SUCCESS, "Device turned OFF: " + currentTargetDeviceId);
        } catch (Exception e) {
            logEvent(MonitorEvent.TYPE_ERROR, "Failed to turn device OFF: " + e.getMessage());
        }
    }

    private void turnDeviceOn() {
        if (currentTargetDeviceId == null || currentTargetDeviceId.isBlank()) {
            logEvent(MonitorEvent.TYPE_WARNING, "No target device configured.");
            return;
        }

        try {
            bluetoothService.controlDevice(currentTargetDeviceId, "on");
            deviceCurrentlyOff = false;
            deviceTurnedOffAt = null;
            logEvent(MonitorEvent.TYPE_SUCCESS, "Device turned ON: " + currentTargetDeviceId);
        } catch (Exception e) {
            logEvent(MonitorEvent.TYPE_ERROR, "Failed to turn device ON: " + e.getMessage());
        }
    }

    public Map<String, Object> getMonitorStatus() {
        Map<String, Object> status = new ConcurrentHashMap<>();
        status.put("targetDeviceId", currentTargetDeviceId != null ? currentTargetDeviceId : "");
        status.put("offlineThresholdMinutes", currentOfflineThreshold);
        status.put("powerCycleDelayMinutes", currentPowerCycleDelay);
        status.put("lastOnlineTime", lastOnlineTime.toEpochMilli());
        status.put("deviceCurrentlyOff", deviceCurrentlyOff);

        synchronized (eventLog) {
            status.put("events", new ArrayList<>(eventLog));
        }

        if (deviceTurnedOffAt != null) {
            status.put("deviceTurnedOffAt", deviceTurnedOffAt.toEpochMilli());
        }
        return status;
    }

    public void updateSettings(String targetDeviceId, int offlineThreshold, int powerCycleDelay) {
        this.currentTargetDeviceId = targetDeviceId;
        this.currentOfflineThreshold = offlineThreshold;
        this.currentPowerCycleDelay = powerCycleDelay;
        logEvent(MonitorEvent.TYPE_INFO, "Settings updated. Target: " + targetDeviceId + ", Threshold: "
                + offlineThreshold + "m, Delay: " + powerCycleDelay + "m");
    }
}
