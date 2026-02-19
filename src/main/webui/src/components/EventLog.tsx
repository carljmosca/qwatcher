import { AlertTriangle, CheckCircle, Info, Terminal } from 'lucide-react';
import { MonitorEvent } from '../types';

interface EventLogProps {
    events: MonitorEvent[];
}

const formatTime = (ts: number) => {
    return new Date(ts).toLocaleTimeString();
};

export const EventLog = ({ events }: EventLogProps) => (
    <div className="lg:col-span-2 p-6 bg-slate-800/50 backdrop-blur-sm rounded-xl border border-slate-700/50 flex flex-col max-h-[400px]">
        <div className="flex items-center gap-3 mb-4">
            <Terminal className="text-green-400" />
            <h2 className="text-xl font-semibold text-slate-200">Event Log</h2>
        </div>

        <div className="flex-1 overflow-y-auto space-y-2 pr-2 font-mono text-sm">
            {events && events.length > 0 ? (
                events.map((evt, i) => (
                    <div key={i} className="flex gap-3 text-slate-300 border-b border-slate-700/30 pb-2 last:border-0 last:pb-0">
                        <span className="text-slate-500 whitespace-nowrap text-xs py-0.5">{formatTime(evt.timestamp)}</span>
                        <div className="flex-1">
                            <div className="flex items-center gap-2 mb-0.5">
                                {evt.type === 'ERROR' && <AlertTriangle size={12} className="text-red-400" />}
                                {evt.type === 'WARNING' && <AlertTriangle size={12} className="text-orange-400" />}
                                {evt.type === 'SUCCESS' && <CheckCircle size={12} className="text-green-400" />}
                                {evt.type === 'INFO' && <Info size={12} className="text-blue-400" />}
                                <span className={`text-xs font-bold ${evt.type === 'ERROR' ? 'text-red-400' :
                                        evt.type === 'WARNING' ? 'text-orange-400' :
                                            evt.type === 'SUCCESS' ? 'text-green-400' : 'text-blue-400'
                                    }`}>{evt.type}</span>
                            </div>
                            <p className="text-slate-300 break-all">{evt.message}</p>
                        </div>
                    </div>
                ))
            ) : (
                <div className="text-center text-slate-500 py-8">No events logged yet.</div>
            )}
        </div>
    </div>
);
