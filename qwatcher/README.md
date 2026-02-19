# QWatcher (Quarkus Watcher)

**QWatcher** is a robust, containerized solution for **Bluetooth-based Internet Monitoring and Auto-Recovery**.

Designed for single-board computers like the **Raspberry Pi**, it continuously monitors your internet connection. If an outage is detected, it automatically communicates with a paired **Shelly Bluetooth Device** (e.g., a smart plug controlling your modem/router) to power-cycle the equipment, attempting to restore connectivity without human intervention.

### Key Features
*   **Automatic Internet Monitoring**: Periodically checks connectivity (default: every 5 mins).
*   **Smart Power Cycling**: Automatically toggles a Bluetooth smart plug (Shelly) when offline for too long.
*   **Modern Web Dashboard**: View status, event logs, and configure settings manually via a responsive UI.
*   **Containerized**: Runs as a Podman/Docker service for easy deployment and isolation.
*   **Bluetooth Low Energy (BLE)**: Uses lightweight BLE RPC commands to control Shelly devices without WiFi dependency.

---

## Technical Stack
This project uses **Quarkus**, the Supersonic Subatomic Java Framework, along with **React** for the frontend (via Quinoa).
To learn more about Quarkus, please visit <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Native Image Compatibility
This application is **not currently compatible** with GraalVM Native Image compilation.
The underlying `bluez-dbus` and `dbus-java` libraries rely heavily on runtime reflection and JNI (native socket access) which require complex, manual configuration for native compilation.

**Recommended Deployment:**
Use the provided **Container (JVM)** deployment method. It offers:
- Fast startup (<1s)
- Reliable DBus integration
- Easy updates via Docker/Podman

## Related Guides

- Scheduler ([guide](https://quarkus.io/guides/scheduler)): Schedule jobs and tasks
- SmallRye OpenAPI ([guide](https://quarkus.io/guides/openapi-swaggerui)): Document your REST APIs with OpenAPI - comes with Swagger UI
- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it
- Quinoa ([guide](https://quarkiverse.github.io/quarkiverse-docs/quarkus-quinoa/dev/index.html)): Develop, build, and serve your npm-compatible web applications such as React, Angular, Vue, Lit, Svelte, Astro, SolidJS, and others alongside Quarkus.

## Provided Code

### Quinoa

Quinoa codestart added a tiny Vite app in src/main/webui. The page is configured to be visible on <a href="/quinoa">/quinoa</a>.

[Related guide section...](https://quarkiverse.github.io/quarkiverse-docs/quarkus-quinoa/dev/index.html)


### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)

## Shelly Bluetooth Integration

This application supports controlling Shelly Plus devices via Bluetooth.

### Prerequisites (Linux/Raspberry Pi)

Ensure `bluez` is installed and the `bluetooth` service is running. This application uses `bluetoothctl` (standard in BlueZ) to communicate with the device.

### Bluetooth Setup (Linux/Pi)

The application communicates with Shelly devices using BlueZ DBus.
**Critical:** You must PAIR and TRUST the Shelly device manually *once* before the application can control it reliably.

1.  Open the terminal on your Raspberry Pi:
    ```bash
    bluetoothctl
    ```
2.  Enable scanning:
    ```bash
    scan on
    ```
3.  Wait for your Shelly device to appear (e.g. `88:13:BF:D6:84:86`).
4.  Pair the device:
    ```bash
    pair <DEVICE_MAC_ADDRESS>
    ```
    *Note: If prompted for a PIN or confirmation, accept it on the terminal.*
5.  Trust the device (ensures it auto-connects):
    ```bash
    trust <DEVICE_MAC_ADDRESS>
    ```
6.  (Optional) Connect to verify:
    ```bash
    connect <DEVICE_MAC_ADDRESS>
    ```
7.  Exit:
    ```bash
    quit
    ```
    
### Troubleshooting Pairing

If you see `Failed to pair: org.bluez.Error.AuthenticationFailed`:

1.  Remove the device cache:
    ```bash
    remove <MAC_ADDRESS>
    ```
2.  Restart Bluetooth service:
    ```bash
    sudo systemctl restart bluetooth
    ```
3.  Ensure the default agent is active:
    ```bash
    bluetoothctl
    agent on
    default-agent
    scan on
    ```
4.  Try pairing again. Watch for a confirmation prompt (type `yes`) or a PIN code request.

If pairing persists to fail:
*   Ensure no other device (e.g. Phone App) is connected to the Shelly device.
*   **Factory Reset** the Shelly device (hold the physical button for 10 seconds until it flashes rapidly, or use the web interface/app to reset).
*   **Alternative:** If you can `connect` but not `pair`, try running the application anyway. The built-in "Write Command" fallback mechanism might work without formal bonding.

## Internet Monitoring & Auto Power Cycling

The application can automatically monitor internet connectivity and power-cycle a Bluetooth device when the connection is lost. This is useful for remotely resetting routers or modems.

### Configuration

Edit `src/main/resources/application.properties`:

```properties
# Cron expression for internet check (default: every 5 minutes)
qwatcher.monitor.internet.check-cron=0 */5 * * * ?

# Minutes of no internet before turning device OFF
qwatcher.monitor.internet.offline-threshold-minutes=5

# Minutes to wait before turning device back ON (power cycle delay)
qwatcher.monitor.internet.power-cycle-delay-minutes=2

# Target device MAC address to control
qwatcher.monitor.internet.target-device-id=88:13:BF:D6:84:86
```

### How It Works

1. **Monitor**: Checks internet connectivity on the configured cron schedule
2. **Detect Outage**: If internet is unavailable for `offline-threshold-minutes`, turns device OFF
3. **Power Cycle**: Waits `power-cycle-delay-minutes`, then turns device back ON
4. **Resume**: Once internet returns, monitoring continues normally

### View Monitor Status

```bash
curl http://localhost:8080/api/host/monitor
```

Returns current monitoring state including last online time and device status.

### Setup

1. **Find your Shelly Device**
   Run `bluetoothctl scan on` to discover devices. Look for "ShellyPlus..."
   Note the MAC address (e.g., `88:13:BF:D6:84:86`).

2. **Connect and Trust**
   Manually pair/trust the device on the Pi once to ensure stable connection:
   ```bash
   bluetoothctl
   [bluetooth]# scan on
   [bluetooth]# trust 88:13:BF:D6:84:86
   [bluetooth]# connect 88:13:BF:D6:84:86
   ```

3. **Identify UUIDs**
   If you need to customize the control UUIDs (default are for standard Shelly Plus RPC), find them via:
   ```bash
   [ShellyPlus...]# menu gatt
   [ShellyPlus...]# list-attributes
   ```
   Look for:
   - **TX Control**: Characteristic with `write` property (Write Length)
   - **Data**: Characteristic with `read, write` properies (RPC Data)

4. **Configuration**
   If different from default, add these to `src/main/resources/application.properties`:
   ```properties
   qwatcher.bluetooth.shelly.tx-uuid=5f6d4f53-5f52-5043-5f74-785f63746c5f
   qwatcher.bluetooth.shelly.data-uuid=5f6d4f53-5f52-5043-5f64-6174615f5f5f
   ```

### Usage in App
1. Go to the dashboard.
2. Click "Add Device" and enter the MAC address.
3. Click "Connect".
4. Once connected, use "ON"/"OFF" buttons to control the relay.

## Deployment

For instructions on deploying this application as a **Systemd Service** using **Podman/Docker** on a Raspberry Pi or Linux server, see:

[**DEPLOYMENT GUIDE (README-DEPLOY.md)**](README-DEPLOY.md)

Supported features:
- **Automatic Startup**: Runs as a systemd service.
- **Containerized**: Clean isolation with necessary Bluetooth/DBus access.
- **Configuration**: Easy environment variable configuration via `qwatcher.container` file.
