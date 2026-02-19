import { useState, useEffect } from 'react';
import { Settings } from 'lucide-react';
import { MonitorStatus } from '../types';

interface MonitorSettingsProps {
    status: MonitorStatus | null;
    refresh: () => void;
}

export const MonitorSettings = ({ status, refresh }: MonitorSettingsProps) => {
    const [settings, setSettings] = useState({
        targetDeviceId: '',
        offlineThresholdMinutes: 5,
        powerCycleDelayMinutes: 2
    });
    const [dirty, setDirty] = useState(false);

    // Sync with status if user hasn't edited
    useEffect(() => {
        if (status && !dirty) {
            setSettings({
                targetDeviceId: status.targetDeviceId,
                offlineThresholdMinutes: status.offlineThresholdMinutes,
                powerCycleDelayMinutes: status.powerCycleDelayMinutes
            });
        }
    }, [status, dirty]);

    const handleSave = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const response = await fetch('/api/host/monitor', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(settings)
            });

            if (response.ok) {
                setDirty(false);
                refresh();
                alert('Settings saved successfully');
            } else {
                alert('Failed to save settings');
            }
        } catch (error) {
            alert('Error saving settings');
        }
    };

    return (
        <div className="lg:col-span-1 p-6 bg-slate-800/50 backdrop-blur-sm rounded-xl border border-slate-700/50">
            <div className="flex items-center gap-3 mb-6">
                <Settings className="text-blue-400" />
                <h2 className="text-xl font-semibold text-slate-200">Auto Power Cycle</h2>
            </div>

            <form onSubmit={handleSave} className="space-y-4">
                <div>
                    <label className="block text-xs uppercase text-slate-500 font-semibold mb-1">Target Device ID</label>
                    <input
                        type="text"
                        value={settings.targetDeviceId}
                        onChange={e => { setSettings({ ...settings, targetDeviceId: e.target.value }); setDirty(true); }}
                        className="w-full bg-slate-900 border border-slate-700 rounded p-2 text-sm font-mono text-white focus:border-blue-500 outline-none"
                        placeholder="88:13:BF:..."
                    />
                </div>
                <div className="grid grid-cols-2 gap-4">
                    <div>
                        <label className="block text-xs uppercase text-slate-500 font-semibold mb-1">Threshold (mins)</label>
                        <input
                            type="number"
                            min="1"
                            value={settings.offlineThresholdMinutes}
                            onChange={e => { setSettings({ ...settings, offlineThresholdMinutes: parseInt(e.target.value) || 0 }); setDirty(true); }}
                            className="w-full bg-slate-900 border border-slate-700 rounded p-2 text-sm text-white focus:border-blue-500 outline-none"
                        />
                    </div>
                    <div>
                        <label className="block text-xs uppercase text-slate-500 font-semibold mb-1">Cycle Delay (mins)</label>
                        <input
                            type="number"
                            min="1"
                            value={settings.powerCycleDelayMinutes}
                            onChange={e => { setSettings({ ...settings, powerCycleDelayMinutes: parseInt(e.target.value) || 0 }); setDirty(true); }}
                            className="w-full bg-slate-900 border border-slate-700 rounded p-2 text-sm text-white focus:border-blue-500 outline-none"
                        />
                    </div>
                </div>

                <button
                    type="submit"
                    disabled={!dirty}
                    className={`w-full py-2 rounded-lg font-medium transition-colors ${dirty ? 'bg-blue-600 hover:bg-blue-500 text-white' : 'bg-slate-700 text-slate-400 cursor-not-allowed'}`}
                >
                    {dirty ? 'Save Changes' : 'Saved'}
                </button>
            </form>
        </div>
    );
};
