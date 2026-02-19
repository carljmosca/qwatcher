import { LucideIcon } from 'lucide-react';
import { motion } from 'framer-motion';

interface StatusCardProps {
    title: string;
    icon: LucideIcon;
    children: React.ReactNode;
    colorClass?: string;
    hoverColorClass?: string;
}

export const StatusCard = ({
    title,
    icon: Icon,
    children,
    colorClass = "text-blue-400",
    hoverColorClass = "group-hover:text-blue-400"
}: StatusCardProps) => (
    <motion.div
        layout
        className={`col-span-1 p-6 bg-slate-800/50 backdrop-blur-sm rounded-xl border border-slate-700/50 hover:border-blue-500/50 transition-colors group`}
    >
        <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-slate-200">{title}</h2>
            <Icon className={`${colorClass}/80 ${hoverColorClass} transition-colors`} />
        </div>
        <div>{children}</div>
    </motion.div>
);
