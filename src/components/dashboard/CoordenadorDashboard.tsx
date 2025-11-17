import { Users, Calendar, Activity, BarChart2 } from 'lucide-react';
import { StatCard } from './StatCard';
import type { StatCard as StatCardType } from '@/lib/types';
import { PaymentsWidget } from './PaymentsWidget';
import { InventoryWidget } from './InventoryWidget';

const coordenadorStats: StatCardType[] = [
    { title: "Pacientes Atendidos Hoje", value: "45", icon: Users, change: "+10.5%", iconColor: "text-blue-500" },
    { title: "Consultas Agendadas", value: "89", icon: Calendar, change: "para esta semana", iconColor: "text-purple-500" },
    { title: "Ocupação da Clínica", value: "75%", icon: Activity, change: "+5% vs semana passada", iconColor: "text-green-500" },
    { title: "Satisfação do Paciente", value: "9.2/10", icon: BarChart2, change: "estável", iconColor: "text-yellow-500" },
];

export function CoordenadorDashboard() {
    return (
        <div className="space-y-6">
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                {coordenadorStats.map(stat => <StatCard key={stat.title} {...stat} />)}
            </div>
            <div className="grid gap-6 grid-cols-1 lg:grid-cols-2">
                 <PaymentsWidget />
                 <InventoryWidget />
            </div>
        </div>
    );
}
