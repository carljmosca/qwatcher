import { useState } from 'react';
import { Power, PowerOff, Activity, Bluetooth } from 'lucide-react';
import { motion } from 'framer-motion';
import { Device } from '../types';

interface DeviceCardProps {
    device: Device;
    onRefresh: () => void;
}

export const DeviceCard = ({ device, onRefresh }: DeviceCardProps) => {
    const [loading, setLoading] = useState(false);

    const handleAction = async (action: 'connect' | 'disconnect') => {
        setLoading(true);
        try {
            await fetch(`/api/devices/${device.id}/${action}`, { method: 'POST' });
            onRefresh();
        } catch (error) {
            console.error('Error performing action:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleControl = async (command: string) => {
        setLoading(true);
        try {
            await fetch(`/api/devices/${device.id}/control`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ command })
            });
            onRefresh();
        } catch (error) {
            console.error('Error sending command:', error);
        } finally {
            setLoading(false);
        }
    };

    return (
        <motion.div
            layout
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.95 }}
            className="p-4 bg-slate-700/30 rounded-lg border border-slate-600/50 hover:bg-slate-700/50 transition-all group relative overflow-hidden flex flex-col justify-between"
        >
            <div className="flex items-center gap-4 mb-4">
                <div className={`p-3 rounded-lg flex-shrink-0 ${device.status === 'Connected' ? 'bg-blue-500/10 text-blue-400' : 'bg-slate-600/20 text-slate-500'}`}>
                    {device.type.includes('Mouse') || device.type.includes('Keyboard') ? <Activity size={20} /> : <Bluetooth size={20} />}
                </div>
                <div className="min-w-0">
                    <h3 className="font-medium text-slate-200 truncate pr-8" title={device.name}>{device.name}</h3>
                    <div className="flex items-center gap-2 mt-1">
                        <span className={`inline-block w-1.5 h-1.5 rounded-full ${device.status === 'Connected' ? 'bg-green-400' : 'bg-slate-500'}`}></span>
                        <p className="text-xs text-slate-400">
                            {device.status}
                            {device.status === 'Connected' && device.state && device.state !== 'UNKNOWN' && (
                                <span className={`ml-2 px-1.5 py-0.5 rounded text-[10px] font-bold ${device.state === 'ON' ? 'bg-green-500/20 text-green-400 border border-green-500/30' : 'bg-red-500/20 text-red-400 border border-red-500/30'}`}>
                                    {device.state}
                                </span>
                            )}
                        </p>
                    </div>
                    <p className="text-[10px] text-slate-500 font-mono mt-1">{device.id}</p>
                </div>
            </div>

            <div className="mt-2 pt-3 border-t border-slate-600/30">
                {device.status === 'Connected' ? (
                    <div className="flex gap-2">
                        <button
                            onClick={() => handleControl('on')}
                            disabled={loading}
                            className="flex-1 px-2 py-1.5 bg-green-500/20 text-green-400 rounded hover:bg-green-500/30 text-xs font-semibold"
                        >
                            ON
                        </button>
                        <button
                            onClick={() => handleControl('off')}
                            disabled={loading}
                            className="flex-1 px-2 py-1.5 bg-red-500/20 text-red-400 rounded hover:bg-red-500/30 text-xs font-semibold"
                        >
                            OFF
                        </button>
                        <button
                            onClick={() => handleAction('disconnect')}
                            disabled={loading}
                            className="px-3 py-1.5 bg-slate-600/50 text-slate-300 rounded hover:bg-slate-600 text-xs"
                            title="Disconnect"
                        >
                            <PowerOff size={14} />
                        </button>
                    </div>
                ) : (
                    <button
                        onClick={() => handleAction('connect')}
                        disabled={loading}
                        className="w-full px-3 py-1.5 bg-blue-600/20 text-blue-400 rounded hover:bg-blue-600/30 text-xs font-semibold flex items-center justify-center gap-2"
                    >
                        <Power size={14} />
                        Connect
                    </button>
                )}
            </div>
        </motion.div>
    );
};
