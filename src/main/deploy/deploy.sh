#!/bin/bash

# Ensure we are root
if [ "$EUID" -ne 0 ]; then
  echo "Please run as root (sudo)"
  exit 1
fi

# Check for Quadlet generator
GENERATOR_PATHS=(
    "/usr/lib/systemd/system-generators/podman-system-generator"
    "/lib/systemd/system-generators/podman-system-generator"
)
QUADLET_FOUND=0
for path in "${GENERATOR_PATHS[@]}"; do
    if [ -f "$path" ]; then
        QUADLET_FOUND=1
        echo "Found Podman Quadlet generator at $path"
        break
    fi
done

if [ $QUADLET_FOUND -eq 0 ]; then
    echo "WARNING: Podman systemd generator not found."
    echo "Your Podman installation may be too old (< 4.4) or missing integration."
    echo "Quadlet service generation will likely fail."
fi

if [ $QUADLET_FOUND -eq 1 ]; then
    # --- QUADLET DEPLOYMENT (Podman 4.4+) ---
    echo "Deploying via Quadlet..."
    
    # Create directory if missing
    if [ ! -d "/etc/containers/systemd/" ]; then
        echo "Creating /etc/containers/systemd/ ..."
        mkdir -p /etc/containers/systemd/
    fi

    # Copy container definition
    echo "Copying src/main/deploy/qwatcher.container -> /etc/containers/systemd/qwatcher.container"
    cp src/main/deploy/qwatcher.container /etc/containers/systemd/

    # Restore SELinux context if applicable
    if command -v restorecon &> /dev/null; then
        echo "Restoring SELinux context..."
        restorecon -v /etc/containers/systemd/qwatcher.container
    fi

    echo "Reloading systemd..."
    systemctl daemon-reload

else
    # --- LEGACY DEPLOYMENT (Podman < 4.4) ---
    echo "WARNING: Podman < 4.4 detected. Falling back to 'podman generate systemd'."
    
    IMAGE="ghcr.io/carljmosca/qwatcher:1.0.0"
    CONTAINER_NAME="qwatcher"
    SERVICE_FILE="/etc/systemd/system/${CONTAINER_NAME}.service"

    echo "Stopping existing container..."
    podman rm -f $CONTAINER_NAME || true

    echo "Creating container..."
    # Match config from qwatcher.container
    podman create \
      --name $CONTAINER_NAME \
      --volume /var/run/dbus/system_bus_socket:/var/run/dbus/system_bus_socket \
      --env DBUS_SYSTEM_BUS_ADDRESS=unix:path=/var/run/dbus/system_bus_socket \
      --env QUARKUS_HTTP_PORT=8080 \
      --env QWATCHER_BLUETOOTH_SHELLY_TX_UUID=5f6d4f53-5f52-5043-5f74-785f63746c5f \
      --env QWATCHER_BLUETOOTH_SHELLY_DATA_UUID=5f6d4f53-5f52-5043-5f64-6174615f5f5f \
      --env "QWATCHER_MONITOR_INTERNET_CHECK_CRON=0 */5 * * * ?" \
      --env QWATCHER_MONITOR_INTERNET_OFFLINE_THRESHOLD_MINUTES=5 \
      --env QWATCHER_MONITOR_INTERNET_POWER_CYCLE_DELAY_MINUTES=2 \
      --env QWATCHER_MONITOR_INTERNET_TARGET_DEVICE_ID=change-me \
      --network host \
      --user root \
      $IMAGE

    echo "Generating systemd unit..."
    podman generate systemd --new --name $CONTAINER_NAME --files --restart-policy=always
    
    echo "Installing service to $SERVICE_FILE..."
    mv container-${CONTAINER_NAME}.service $SERVICE_FILE
    
    echo "Reloading systemd..."
    systemctl daemon-reload
    systemctl enable $CONTAINER_NAME
fi

echo "Step 4: Starting QWatcher service..."

# Verify service generation
if ! systemctl list-units --all | grep -q "qwatcher.service"; then
    echo "WARNING: qwatcher.service was not generated."
    echo "Check if Podman Quadlet is supported (Podman v4.4+)."
    echo "You may need to inspect generator logs: journalctl -u systemd-gpt-generator" 
    # Attempt dry-run if generator exists
    GENERATOR=/usr/lib/systemd/system-generators/podman-system-generator
    if [ -f "$GENERATOR" ]; then
        echo "Running generator dry-run:"
        $GENERATOR --dry-run
    fi
fi

echo "Step 4: Starting QWatcher service..."
systemctl start qwatcher

echo "----------------------------------------"
echo "Service Status:"
systemctl status qwatcher --no-pager
echo "----------------------------------------"
