import { Plus, Bluetooth } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { Device } from '../types';
import { DeviceCard } from './DeviceCard';

interface DeviceListProps {
    devices: Device[];
    onRefresh: () => void;
    onAddClick: () => void;
}

export const DeviceList = ({ devices, onRefresh, onAddClick }: DeviceListProps) => (
    <motion.div
        layout
        className="col-span-1 md:col-span-2 lg:col-span-3 p-6 bg-slate-800/50 backdrop-blur-sm rounded-xl border border-slate-700/50"
    >
        <div className="flex items-center justify-between mb-6">
            <div className="flex items-center gap-4">
                <h2 className="text-xl font-semibold text-slate-200 flex items-center gap-2">
                    <Bluetooth className="text-blue-400" />
                    Bluetooth Devices
                </h2>
                <span className="bg-slate-700/50 px-3 py-1 rounded-full text-xs text-slate-300 font-mono">
                    {devices?.length || 0} DETECTED
                </span>
            </div>

            <button
                onClick={onAddClick}
                className="flex items-center gap-2 px-3 py-1.5 bg-blue-600/20 text-blue-400 hover:bg-blue-600/30 rounded-lg text-sm font-medium transition-colors"
            >
                <Plus size={16} />
                Add Device
            </button>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-4">
            <AnimatePresence>
                {devices && devices.length > 0 ? (
                    devices.map((device) => (
                        <DeviceCard
                            key={device.id || device.name}
                            device={device}
                            onRefresh={onRefresh}
                        />
                    ))
                ) : (
                    <div className="col-span-full flex flex-col items-center justify-center py-12 text-slate-500 border-2 border-dashed border-slate-700/50 rounded-xl">
                        <Bluetooth className="w-12 h-12 mb-3 opacity-20" />
                        <p>No devices detected nearby</p>
                    </div>
                )}
            </AnimatePresence>
        </div>
    </motion.div>
);
