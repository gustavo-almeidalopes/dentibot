import { DollarSign, UserPlus, Calendar, Activity } from 'lucide-react';
import { StatCard } from './StatCard';
import type { StatCard as StatCardType } from '@/lib/types';
import { PaymentsWidget } from './PaymentsWidget';
import { ChartGenerator } from './ChartGenerator';

const dentistStats: StatCardType[] = [
    { title: "Receita de Hoje", value: "R$4.250", icon: DollarSign, change: "+15.2%", iconColor: "text-green-500" },
    { title: "Novos Pacientes", value: "6", icon: UserPlus, change: "+2 de ontem", iconColor: "text-blue-500" },
    { title: "Consultas Hoje", value: "14", icon: Calendar, change: "-3 de ontem", iconColor: "text-purple-500" },
    { title: "Tempo Médio Proced.", value: "45 min", icon: Activity, change: "+5 min", iconColor: "text-yellow-500" },
];

export function DentistDashboard() {
    return (
        <div className="space-y-6">
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                {dentistStats.map(stat => <StatCard key={stat.title} {...stat} />)}
            </div>
            <div className="grid gap-6 grid-cols-1">
                 <PaymentsWidget />
            </div>
            <div>
                <ChartGenerator />
            </div>
        </div>
    );
}
