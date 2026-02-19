import { useState, useEffect } from 'react';
import { Zap, Clock, Wifi } from 'lucide-react';
import { motion } from 'framer-motion';
import { HostStatus, MonitorStatus } from './types';
import { AddDeviceModal } from './components/AddDeviceModal';
import { StatusCard } from './components/StatusCard';
import { DeviceList } from './components/DeviceList';
import { MonitorSettings } from './components/MonitorSettings';
import { EventLog } from './components/EventLog';

function App() {
    const [status, setStatus] = useState<HostStatus | null>(null);
    const [monitorStatus, setMonitorStatus] = useState<MonitorStatus | null>(null);
    const [showAddModal, setShowAddModal] = useState(false);

    const fetchStatus = async () => {
        try {
            const response = await fetch('/api/host');
            if (response.ok) {
                const data = await response.json();
                setStatus(data);
            }

            const monitorResponse = await fetch('/api/host/monitor');
            if (monitorResponse.ok) {
                const data = await monitorResponse.json();
                setMonitorStatus(data);
            }
        } catch (error) {
            console.error('Error fetching status:', error);
        }
    };

    useEffect(() => {
        fetchStatus();
        const interval = setInterval(fetchStatus, 5000);
        return () => clearInterval(interval);
    }, []);

    const formatUptime = (seconds: number) => {
        const d = Math.floor(seconds / (3600 * 24));
        const h = Math.floor((seconds % (3600 * 24)) / 3600);
        const m = Math.floor((seconds % 3600) / 60);

        if (d > 0) return `${d}d ${h}h ${m}m`;
        return `${h}h ${m}m`;
    };

    return (
        <div className="min-h-screen bg-slate-900 text-white font-sans p-4 md:p-8">
            <AddDeviceModal
                isOpen={showAddModal}
                onClose={() => setShowAddModal(false)}
                onSuccess={() => { setShowAddModal(false); fetchStatus(); }}
            />

            <header className="mb-8 md:mb-12 flex items-center justify-between">
                <div className="flex items-center gap-4">
                    <div className="p-3 bg-blue-600 rounded-lg shadow-lg shadow-blue-500/20">
                        <Zap className="w-6 h-6 md:w-8 md:h-8 text-white" />
                    </div>
                    <div>
                        <h1 className="text-2xl md:text-3xl font-bold bg-gradient-to-r from-blue-400 to-purple-400 bg-clip-text text-transparent">QWatcher</h1>
                        <p className="text-slate-400 text-xs md:text-sm">System Monitor & Control</p>
                    </div>
                </div>
                <div className="text-right hidden md:block">
                    <div className="text-xs text-slate-500 uppercase tracking-wider font-semibold">Active Monitoring</div>
                    <div className="flex items-center gap-2 justify-end text-slate-300 text-sm">
                        <div className="w-2 h-2 rounded-full bg-green-500 animate-pulse"></div>
                        Running
                    </div>
                </div>
            </header>

            <main className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {/* Uptime Card */}
                <StatusCard title="System Uptime" icon={Clock}>
                    <div className="text-3xl md:text-4xl font-mono text-white tracking-tight">
                        {status ? formatUptime(status.uptimeSeconds) : <span className="animate-pulse">...</span>}
                    </div>
                </StatusCard>

                {/* Internet Card */}
                <StatusCard
                    title="Internet"
                    icon={Wifi}
                    colorClass={status?.internetAvailable ? 'text-green-400' : 'text-red-400'}
                    hoverColorClass={status?.internetAvailable ? 'group-hover:text-green-400' : 'group-hover:text-red-400'}
                >
                    <div className="flex items-center gap-3">
                        <div className={`w-3 h-3 rounded-full ${status?.internetAvailable ? 'bg-green-500 shadow-[0_0_8px_rgba(74,222,128,0.6)]' : 'bg-red-500'}`}></div>
                        <span className="text-lg font-medium">
                            {status ? (status.internetAvailable ? 'Connected' : 'Offline') : 'Checking...'}
                        </span>
                    </div>
                </StatusCard>

                {/* Device List */}
                <DeviceList
                    devices={status?.devices || []}
                    onRefresh={fetchStatus}
                    onAddClick={() => setShowAddModal(true)}
                />

                {/* Monitor Controls & Logs */}
                <motion.div
                    layout
                    className="col-span-1 md:col-span-2 lg:col-span-3 grid grid-cols-1 lg:grid-cols-3 gap-6"
                >
                    <MonitorSettings status={monitorStatus} refresh={fetchStatus} />
                    <EventLog events={monitorStatus?.events || []} />
                </motion.div>
            </main>
        </div>
    );
}

export default App;
