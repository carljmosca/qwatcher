#!/bin/bash

MAC=$1
CMD=$2

if [ -z "$MAC" ] || [ -z "$CMD" ]; then
    echo "Usage: ./test_shelly.sh <MAC_ADDRESS> <on|off>"
    exit 1
fi

STATE="true"
if [ "$CMD" == "off" ]; then
    STATE="false"
fi

# 1. Construct JSON
# {"id":1,"src":"shell","method":"Switch.Set","params":{"id":0,"on":true}}
JSON="{\"id\":1,\"src\":\"shell\",\"method\":\"Switch.Set\",\"params\":{\"id\":0,\"on\":$STATE}}"
LEN=${#JSON}

echo "Payload: $JSON"
echo "Length: $LEN"

# 2. Setup UUIDs (Shelly Plus defaults)
UUID_TX="5f6d4f53-5f52-5043-5f74-785f63746c5f"
UUID_DATA="5f6d4f53-5f52-5043-5f64-6174615f5f5f"

# 3. Find Handles using gatttool (more reliable)
echo "Looking for handles..."
# get handle for TX
HANDLE_TX=$(gatttool -b $MAC --char-desc | grep $UUID_TX | awk -F 'handle: ' '{print $2}' | awk '{print $1}')
# get handle for DATA
HANDLE_DATA=$(gatttool -b $MAC --char-desc | grep $UUID_DATA | awk -F 'handle: ' '{print $2}' | awk '{print $1}')

if [ -z "$HANDLE_TX" ] || [ -z "$HANDLE_DATA" ]; then
    echo "Could not find handles. Trying fixed handles from bluetoothctl output logic (often +1 from char handle)"
    # Fallback or manual intervention needed if finding fails.
    # But usually gatttool --char-desc works.
    echo "Please ensure gatttool is installed and device is reachable."
    exit 1
fi

echo "TX Handle: $HANDLE_TX"
echo "DATA Handle: $HANDLE_DATA"

# 4. Write Length (Big Endian 4 bytes)
# Convert len to 4 bytes hex
HEX_LEN=$(printf "%08x" $LEN | sed 's/../&/g') 
# Format: 0000003a (example)

echo "Writing Length: $HEX_LEN to $HANDLE_TX"
gatttool -b $MAC --char-write-req --handle=$HANDLE_TX --value=$HEX_LEN

# 5. Write Data (JSON)
# Convert JSON string to hex
HEX_JSON=$(echo -n "$JSON" | xxd -p | tr -d '\n')

echo "Writing Data: $HEX_JSON to $HANDLE_DATA"
gatttool -b $MAC --char-write-req --handle=$HANDLE_DATA --value=$HEX_JSON

echo "Done."
