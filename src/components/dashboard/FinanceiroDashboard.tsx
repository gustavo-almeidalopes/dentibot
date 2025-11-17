import { DollarSign, CreditCard, TrendingUp, TrendingDown } from 'lucide-react';
import { StatCard } from './StatCard';
import type { StatCard as StatCardType } from '@/lib/types';
import { PaymentsWidget } from './PaymentsWidget';
import { ChartGenerator } from './ChartGenerator';

const financeiroStats: StatCardType[] = [
    { title: "Faturamento do Mês", value: "R$85.7k", icon: DollarSign, change: "+8.2%", iconColor: "text-green-500" },
    { title: "Contas a Receber", value: "R$15.2k", icon: TrendingUp, change: "-3.1%", iconColor: "text-yellow-500" },
    { title: "Contas a Pagar", value: "R$9.8k", icon: TrendingDown, change: "+12%", iconColor: "text-red-500" },
    { title: "Pagamentos Online", value: "28", icon: CreditCard, change: "+5 hoje", iconColor: "text-blue-500" },
];

export function FinanceiroDashboard() {
    return (
        <div className="space-y-6">
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                {financeiroStats.map(stat => <StatCard key={stat.title} {...stat} />)}
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
