import { UserCheck, CalendarPlus, UserPlus, PhoneIncoming } from 'lucide-react';
import { StatCard } from './StatCard';
import type { StatCard as StatCardType } from '@/lib/types';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../ui/card';

const receptionistStats: StatCardType[] = [
    { title: "Check-ins de Pacientes", value: "8", icon: UserCheck, change: "+3 nesta hora", iconColor: "text-green-500" },
    { title: "Próximas Consultas", value: "22", icon: CalendarPlus, change: "nas próximas 4 horas", iconColor: "text-blue-500" },
    { title: "Novos Agendamentos Hoje", value: "12", icon: UserPlus, change: "+5.1%", iconColor: "text-purple-500" },
    { title: "Chamadas Recebidas", value: "3", icon: PhoneIncoming, change: "atualmente na fila", iconColor: "text-yellow-500" },
];

export function ReceptionistDashboard() {
    return (
        <div className="space-y-6">
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                {receptionistStats.map(stat => <StatCard key={stat.title} {...stat} />)}
            </div>
            <div className="grid gap-6">
                 <Card>
                    <CardHeader>
                        <CardTitle>Próximas Consultas</CardTitle>
                        <CardDescription>Uma lista dinâmica de consultas apareceria aqui.</CardDescription>
                    </CardHeader>
                    <CardContent>
                        <p className="text-muted-foreground">O sistema está pronto para exibir os próximos agendamentos, atualizados em tempo real.</p>
                    </CardContent>
                 </Card>
            </div>
        </div>
    );
}
