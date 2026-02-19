export interface Device {
    id: string;
    name: string;
    type: string;
    status: string;
    state?: string;
    lastSeen: number;
}

export interface MonitorEvent {
    timestamp: number;
    type: string;
    message: string;
}

export interface MonitorStatus {
    targetDeviceId: string;
    offlineThresholdMinutes: number;
    powerCycleDelayMinutes: number;
    lastOnlineTime: number;
    deviceCurrentlyOff: boolean;
    events: MonitorEvent[];
}

export interface HostStatus {
    uptimeSeconds: number;
    internetAvailable: boolean;
    devices: Device[];
}
