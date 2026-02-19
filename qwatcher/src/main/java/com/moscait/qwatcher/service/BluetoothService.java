package com.moscait.qwatcher.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.hypfvieh.bluetooth.DeviceManager;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothAdapter;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattCharacteristic;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattService;
import com.moscait.qwatcher.model.Device;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class BluetoothService {

    private static final Logger LOG = Logger.getLogger(BluetoothService.class);

    @ConfigProperty(name = "qwatcher.bluetooth.shelly.tx-uuid", defaultValue = "5f6d4f53-5f52-5043-5f74-785f63746c5f")
    String shellyTxUuid;

    @ConfigProperty(name = "qwatcher.bluetooth.shelly.data-uuid", defaultValue = "5f6d4f53-5f52-5043-5f64-6174615f5f5f")
    String shellyDataUuid;

    @Inject
    ObjectMapper objectMapper;

    private Set<Device> manualDevices = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private Map<String, String> deviceStates = new ConcurrentHashMap<>();
    private DeviceManager deviceManager;
    private boolean isLinux;

    @PostConstruct
    void init() {
        String os = System.getProperty("os.name").toLowerCase();
        isLinux = os.contains("nux");

        if (isLinux) {
            try {
                // false = System Bus
                deviceManager = DeviceManager.createInstance(false);
                LOG.info("Initialized BlueZ DBus Manager");
            } catch (Exception e) {
                LOG.error("Failed to initialize BlueZ DBus", e);
                isLinux = false;
            }
        }
    }

    @PreDestroy
    void tearDown() {
        // deviceManager doesn't have close() in older versions?
        // If it does, we can call it. But usually it manages its own connection.
    }

    public void addManualDevice(String id, String name) {
        manualDevices.removeIf(d -> d.id.equalsIgnoreCase(id));
        manualDevices.add(new Device(id, name, "Manual Device", "Unknown", Instant.now().getEpochSecond()));
    }

    public List<Device> getDevices() {
        List<Device> allDevices = new ArrayList<>();

        if (isLinux && deviceManager != null) {
            BluetoothAdapter adapter = deviceManager.getAdapter();
            if (adapter != null) {
                try {
                    if (!adapter.isDiscovering()) {
                        adapter.startDiscovery();
                    }

                    List<BluetoothDevice> bluezDevices = deviceManager.getDevices();
                    for (BluetoothDevice bd : bluezDevices) {
                        String addr = bd.getAddress();
                        String name = bd.getName();
                        // Filter out non-Shelly devices to avoid clutter
                        if (name != null
                                && (name.toLowerCase().startsWith("shelly") || name.toLowerCase().contains("shelly"))) {
                            Boolean connected = bd.isConnected();
                            String status = (connected != null && connected) ? "Connected" : "Disconnected";
                            String state = deviceStates.getOrDefault(addr, "UNKNOWN");

                            // Query actual state if connected
                            if (connected != null && connected) {
                                String queriedState = queryDeviceState(addr);
                                if (!queriedState.equals("UNKNOWN")) {
                                    state = queriedState;
                                    deviceStates.put(addr, state);
                                }
                            }

                            allDevices.add(new Device(addr, name, "BLE Device", status, Instant.now().getEpochSecond(),
                                    state));
                        }
                    }
                } catch (Exception e) {
                    LOG.error("Error scanning devices", e);
                }
            }
        }

        for (Device manual : manualDevices) {
            if (allDevices.stream().noneMatch(d -> d.id.equalsIgnoreCase(manual.id))) {
                String status = "Disconnected";
                if (isLinux && deviceManager != null) {
                    BluetoothDevice bd = findDevice(manual.id);
                    if (bd != null && bd.isConnected()) {
                        status = "Connected";
                    }
                }
                String state = deviceStates.getOrDefault(manual.id, "UNKNOWN");
                allDevices.add(
                        new Device(manual.id, manual.name, manual.type, status, Instant.now().getEpochSecond(), state));
            }
        }

        return allDevices;
    }

    private BluetoothDevice findDevice(String address) {
        if (deviceManager == null)
            return null;
        List<BluetoothDevice> devices = deviceManager.getDevices();
        for (BluetoothDevice d : devices) {
            String addr = d.getAddress();
            if (addr != null && addr.equalsIgnoreCase(address)) {
                return d;
            }
        }
        return null;
    }

    public void controlDevice(String address, String command) throws Exception {
        LOG.info("Sending command " + command + " to " + address);

        if (!isLinux || deviceManager == null) {
            throw new Exception("Cannot control device: BlueZ not initialized (Are you on Linux/Pi?)");
        }

        try {
            BluetoothDevice device = findDevice(address);
            if (device == null) {
                // Try to connect anyway? No, we need object wrapper.
                throw new Exception("Device " + address + " not found in BlueZ cache. Ensure it is scanned.");
            }

            if (!device.isConnected()) {
                LOG.info("Connecting to " + address);
                device.connect();
                // Check if services resolved logic is needed
                int retries = 0;
                while (!device.isServicesResolved() && retries < 50) {
                    Thread.sleep(100);
                    retries++;
                }
                if (!device.isServicesResolved()) {
                    LOG.warn("Services not resolved yet, proceeding anyway...");
                }
            }

            // Check Pairing (Logging only)
            Boolean paired = device.isPaired();
            LOG.info("Device Paired: " + paired);

            // Query current state before changing
            boolean isOn = command.equalsIgnoreCase("on");
            String currentState = queryDeviceState(address);
            LOG.info("Current state: " + currentState + ", Desired: " + (isOn ? "ON" : "OFF"));

            // Skip if already in desired state
            if ((isOn && "ON".equals(currentState)) || (!isOn && "OFF".equals(currentState))) {
                LOG.info("Device already in desired state, skipping command");
                return;
            }

            // Construct JSON
            ObjectNode json = objectMapper.createObjectNode();
            json.put("id", System.currentTimeMillis() % 10000); // Unique ID
            json.put("src", "shell");
            json.put("method", "Switch.Set");
            ObjectNode params = json.putObject("params");
            params.put("id", 0);
            params.put("on", isOn);

            byte[] jsonBytes = json.toString().getBytes(StandardCharsets.UTF_8);
            int length = jsonBytes.length;

            // Format Length (Big Endian)
            byte[] lenBytes = new byte[4];
            lenBytes[0] = (byte) ((length >> 24) & 0xFF);
            lenBytes[1] = (byte) ((length >> 16) & 0xFF);
            lenBytes[2] = (byte) ((length >> 8) & 0xFF);
            lenBytes[3] = (byte) (length & 0xFF);

            // Find Characteristics
            BluetoothGattCharacteristic txChar = findCharacteristic(device, shellyTxUuid);
            BluetoothGattCharacteristic dataChar = findCharacteristic(device, shellyDataUuid);

            if (txChar == null || dataChar == null) {
                throw new Exception("Could not find Shelly Characteristics (" + shellyTxUuid + " / " + shellyDataUuid
                        + "). Ensure device exposes them.");
            }

            // Write Length
            LOG.info("Writing Length to " + shellyTxUuid);
            Map<String, Object> options = new HashMap<>();
            // Determine write type based on flags
            String writeType = "request";
            try {
                List<String> flags = txChar.getFlags();
                if (flags != null) {
                    LOG.info("TX Char Flags: " + flags);
                    if (flags.contains("write-without-response") && !flags.contains("write")) {
                        writeType = "command";
                    }
                }
            } catch (Exception e) {
            }
            options.put("type", writeType);

            try {
                txChar.writeValue(lenBytes, options);
            } catch (Exception e) {
                // Fallback
                LOG.warn("Write failed (" + writeType + "), trying alternate mode...");
                options.put("type", writeType.equals("request") ? "command" : "request");
                txChar.writeValue(lenBytes, options);
            }

            Thread.sleep(100);

            // Write Data (Manually chunked 20 bytes for safety)
            LOG.info("Writing Data to " + shellyDataUuid);
            int chunkSize = 20;
            for (int i = 0; i < length; i += chunkSize) {
                int end = Math.min(length, i + chunkSize);
                byte[] chunk = Arrays.copyOfRange(jsonBytes, i, end);
                LOG.info("Writing Chunk " + (i / chunkSize + 1));
                dataChar.writeValue(chunk, options);
                Thread.sleep(50); // Small delay
            }

            LOG.info("Successfully sent Shelly command via BlueZ");
            deviceStates.put(address.toUpperCase(), isOn ? "ON" : "OFF");

        } catch (Exception e) {
            String msg = "Failed to control device: " + e.getMessage();
            LOG.error(msg, e);
            throw new Exception(msg, e);
        }
    }

    private String queryDeviceState(String address) {
        try {
            BluetoothDevice device = findDevice(address);
            if (device == null || !device.isConnected()) {
                return "UNKNOWN";
            }

            // Ensure services are resolved
            if (!device.isServicesResolved()) {
                int retries = 0;
                while (!device.isServicesResolved() && retries < 30) {
                    Thread.sleep(100);
                    retries++;
                }
            }

            // Find characteristics
            BluetoothGattCharacteristic txChar = findCharacteristic(device, shellyTxUuid);
            BluetoothGattCharacteristic dataChar = findCharacteristic(device, shellyDataUuid);

            if (txChar == null || dataChar == null) {
                return "UNKNOWN";
            }

            // Build Switch.GetStatus RPC
            ObjectNode json = objectMapper.createObjectNode();
            json.put("id", System.currentTimeMillis() % 10000);
            json.put("src", "shell");
            json.put("method", "Switch.GetStatus");
            ObjectNode params = json.putObject("params");
            params.put("id", 0);

            byte[] jsonBytes = json.toString().getBytes(StandardCharsets.UTF_8);
            int length = jsonBytes.length;

            // Format Length (Big Endian)
            byte[] lenBytes = new byte[4];
            lenBytes[0] = (byte) ((length >> 24) & 0xFF);
            lenBytes[1] = (byte) ((length >> 16) & 0xFF);
            lenBytes[2] = (byte) ((length >> 8) & 0xFF);
            lenBytes[3] = (byte) (length & 0xFF);

            // Write with fallback
            Map<String, Object> options = new HashMap<>();
            options.put("type", "command");

            txChar.writeValue(lenBytes, options);
            Thread.sleep(50);

            // Write data in chunks
            int chunkSize = 20;
            for (int i = 0; i < length; i += chunkSize) {
                int end = Math.min(length, i + chunkSize);
                byte[] chunk = Arrays.copyOfRange(jsonBytes, i, end);
                dataChar.writeValue(chunk, options);
                Thread.sleep(50);
            }

            // Read response - try to read the data characteristic
            // Note: bluez-dbus doesn't have easy notification reading in sync mode
            // We'll use a simple read attempt with timeout
            Thread.sleep(200); // Give device time to respond

            try {
                byte[] response = dataChar.readValue(new HashMap<>());
                if (response != null && response.length > 0) {
                    String responseStr = new String(response, StandardCharsets.UTF_8);
                    LOG.info("GetStatus response: " + responseStr);

                    // Parse JSON response:
                    // {"id":123,"src":"...","result":{"id":0,"output":true,...}}
                    if (responseStr.contains("\"output\":true") || responseStr.contains("\"output\": true")) {
                        return "ON";
                    } else if (responseStr.contains("\"output\":false") || responseStr.contains("\"output\": false")) {
                        return "OFF";
                    }
                }
            } catch (Exception e) {
                LOG.debug("Could not read response: " + e.getMessage());
            }

            return "UNKNOWN";

        } catch (Exception e) {
            LOG.warn("Failed to query device state for " + address + ": " + e.getMessage());
            return "UNKNOWN";
        }
    }

    private BluetoothGattCharacteristic findCharacteristic(BluetoothDevice device, String uuid) {
        List<BluetoothGattService> services = device.getGattServices();
        if (services == null)
            return null;

        for (BluetoothGattService s : services) {
            List<BluetoothGattCharacteristic> chars = s.getGattCharacteristics();
            if (chars == null)
                continue;
            for (BluetoothGattCharacteristic c : chars) {
                if (c.getUuid().equalsIgnoreCase(uuid)) {
                    return c;
                }
            }
        }
        return null;
    }

    public boolean connectDevice(String address) {
        if (!isLinux || deviceManager == null)
            return false;
        BluetoothDevice d = findDevice(address);
        if (d != null) {
            try {
                d.connect();
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public boolean disconnectDevice(String address) {
        if (!isLinux || deviceManager == null)
            return false;
        BluetoothDevice d = findDevice(address);
        if (d != null) {
            try {
                d.disconnect();
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
}
