# Deployment with Podman Quadlet (Recommended)

This project includes a Podman Quadlet service definition for automatic startup and management.

## Prerequisites

- **Target OS**: Linux (Raspberry Pi OS, Fedora, CentOS, etc.)
- **Podman**: Version 4.4+ (Supports `.container` files)
- **BlueZ**: Ensure `bluetoothd` is running on the host.

## Building with GitHub Actions (Recommended)

This project is configured to automatically build and publish a multi-arch container image when you push a new version tag.

1.  **Tag the release**:
    ```bash
    git tag v1.0.0
    git push origin v1.0.0
    ```
2.  **Wait for Build**: The GitHub Action will build and push `ghcr.io/carljmosca/qwatcher:v1.0.0`.
3.  **Deploy**:
    Update `src/main/deploy/qwatcher.container` to match the tag:
    ```ini
    Image=ghcr.io/carljmosca/qwatcher:v1.0.0
    ```
    Then run the local deploy script.

## Manual Build (Local)

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/moscait/qwatcher.git
    cd qwatcher
    ```

2.  **Run the deployment script**:
    This script builds the container image locally and installs the systemd unit.
    ```bash
    sudo ./src/main/deploy/deploy.sh
    ```

3.  **Check Status**:
    ```bash
    systemctl status qwatcher
    podman logs -f qwatcher
    ```

## Manual Installation (without script)

1.  **Build image**:
    ```bash
    podman build -f src/main/docker/Dockerfile.jvm -t localhost/qwatcher:jvm .
    ```

2.  **Copy unit file**:
    ```bash
    sudo cp src/main/deploy/qwatcher.container /etc/containers/systemd/
    sudo systemctl daemon-reload
    sudo systemctl start qwatcher
    ```

## Configuration

The applicWhat name would you like for the new subdirectory where the application will be moved?iables.
You can edit this file **before** running the deploy script to customize:

- **Target Device ID** (`QWATCHER_MONITOR_INTERNET_TARGET_DEVICE_ID`)
- **Check Interval** (`QWATCHER_MONITOR_INTERNET_CHECK_CRON`)
- **Offline Thresholds**

If you need to change configuration *after* deployment:
1.  Edit `/etc/containers/systemd/qwatcher.container`
2.  Run `systemctl daemon-reload`
3.  Run `systemctl restart qwatcher`

## Verify Access

The service runs on port `8080` by default. Access the dashboard at:
## Testing (Simulating Failure)

To verify the auto-power-cycling feature, you need to simulate an internet outage.

**Option 1: Unplug WAN Cable (Recommended)**
Simply unplug the WAN/Internet cable from your router. This is the most realistic test. The Pi (on LAN) will lose internet access, triggering the monitor.

**Option 2: Firewall Rule (Advanced)**
If you have SSH access and want to test without physical access, you can block outgoing traffic to the check target (e.g., `google.com` or `8.8.8.8` if configured).

1.  **Find the check target**: By default, the app checks `google.com`.
2.  **Block traffic (on Host)**:
    ```bash
    # Reject packets to Google's IP (find it first via `ping google.com`)
    sudo iptables -I OUTPUT -d 142.250.0.0/16 -j REJECT
    ```
    *Warning: This might block other services.*
3.  **Restore**:
    ```bash
    sudo iptables -D OUTPUT -d 142.250.0.0/16 -j REJECT
    ```

**Option 3: Disable Interface (Console Access Only)**
If you are connected via HDMI/Keyboard (not SSH):
```bash
sudo ifconfig wlan0 down
# ... wait for cycle ...
sudo ifconfig wlan0 up
```
