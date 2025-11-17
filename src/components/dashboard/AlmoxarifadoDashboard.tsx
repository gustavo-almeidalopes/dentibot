import { Boxes, PackagePlus, Truck, AlertTriangle } from 'lucide-react';
import { StatCard } from './StatCard';
import type { StatCard as StatCardType } from '@/lib/types';
import { InventoryWidget } from './InventoryWidget';

const almoxarifadoStats: StatCardType[] = [
    { title: "Itens em Estoque", value: "358", icon: Boxes, change: "+20 hoje", iconColor: "text-blue-500" },
    { title: "Itens com Estoque Baixo", value: "12", icon: AlertTriangle, change: "+2 hoje", iconColor: "text-yellow-500" },
    { title: "Pedidos de Compra", value: "5", icon: PackagePlus, change: "2 novos", iconColor: "text-green-500" },
    { title: "Entregas Pendentes", value: "3", icon: Truck, change: "1 atrasada", iconColor: "text-red-500" },
];

export function AlmoxarifadoDashboard() {
    return (
        <div className="space-y-6">
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                {almoxarifadoStats.map(stat => <StatCard key={stat.title} {...stat} />)}
            </div>
            <div className="grid gap-6">
                 <InventoryWidget />
            </div>
        </div>
    );
}
