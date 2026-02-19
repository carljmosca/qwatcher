import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';

interface AddDeviceModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
}

export const AddDeviceModal = ({ isOpen, onClose, onSuccess }: AddDeviceModalProps) => {
    const [id, setId] = useState('');
    const [name, setName] = useState('');

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            if (!id) return;

            const response = await fetch('/api/devices', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ id, name })
            });

            if (response.ok) {
                setId('');
                setName('');
                onSuccess();
            } else {
                alert('Failed to add device');
            }
        } catch (error) {
            console.error('Error adding device:', error);
        }
    };

    return (
        <AnimatePresence>
            {isOpen && (
                <motion.div
                    initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                    className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4"
                    onClick={(e) => { if (e.target === e.currentTarget) onClose() }}
                >
                    <motion.div
                        initial={{ scale: 0.9, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} exit={{ scale: 0.9, opacity: 0 }}
                        className="bg-slate-800 p-6 rounded-xl border border-slate-700 w-full max-w-md shadow-2xl"
                    >
                        <h2 className="text-xl font-bold mb-4">Add Device Manually</h2>
                        <form onSubmit={handleSubmit} className="space-y-4">
                            <div>
                                <label className="block text-sm text-slate-400 mb-1">MAC Address</label>
                                <input
                                    type="text"
                                    placeholder="00:11:22:33:44:55"
                                    value={id}
                                    onChange={e => setId(e.target.value)}
                                    className="w-full bg-slate-900 border border-slate-700 rounded-lg p-2 text-white focus:outline-none focus:border-blue-500 font-mono"
                                    required
                                />
                            </div>
                            <div>
                                <label className="block text-sm text-slate-400 mb-1">Name</label>
                                <input
                                    type="text"
                                    placeholder="Shelly Switch"
                                    value={name}
                                    onChange={e => setName(e.target.value)}
                                    className="w-full bg-slate-900 border border-slate-700 rounded-lg p-2 text-white focus:outline-none focus:border-blue-500"
                                />
                            </div>
                            <div className="flex justify-end gap-3 mt-6">
                                <button
                                    type="button"
                                    onClick={onClose}
                                    className="px-4 py-2 rounded-lg text-slate-300 hover:bg-slate-700 transition-colors"
                                >
                                    Cancel
                                </button>
                                <button
                                    type="submit"
                                    className="px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white font-medium transition-colors"
                                >
                                    Add Device
                                </button>
                            </div>
                        </form>
                    </motion.div>
                </motion.div>
            )}
        </AnimatePresence>
    );
};
