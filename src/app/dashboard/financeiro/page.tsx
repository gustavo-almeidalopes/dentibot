// src/app/dashboard/financeiro/page.tsx
'use client';

import { DollarSign, FilePieChart, CreditCard, TrendingUp, BookCheck, History, Landmark, Settings, FileText, Download, PlusCircle } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Switch } from '@/components/ui/switch';
import { cn } from '@/lib/utils';
import { StatCard } from '@/components/dashboard/StatCard';
import type { StatCard as StatCardType } from '@/lib/types';


const financeiroStats: StatCardType[] = [
    { title: "Recebido no Mês", value: "R$85.7k", icon: DollarSign, change: "+8.2%", iconColor: "dark:text-[#32CD32] text-[#7CFC00]" },
    { title: "Pendente (Particular)", value: "R$15.2k", icon: TrendingUp, change: "-3.1%", iconColor: "dark:text-[#FFD700] text-[#FFDE21]" },
    { title: "Faturado (Convênios)", value: "R$29.8k", icon: Landmark, change: "+12%", iconColor: "text-blue-500" },
    { title: "Pagamentos (Particular)", value: "R$55.9k", icon: CreditCard, change: "+5 hoje", iconColor: "text-purple-500" },
];

const payments = [
  { patient: "Ana Silva", date: "2024-07-29", procedure: "Clareamento", value: "R$ 1.200,00", method: "Cartão de Crédito", status: "Pago", transactionId: "txn_123abc" },
  { patient: "Carlos Souza", date: "2024-07-28", procedure: "Limpeza", value: "R$ 250,00", method: "PIX", status: "Pago", transactionId: "txn_456def" },
  { patient: "Mariana Costa", date: "2024-07-28", procedure: "Implante", value: "R$ 4.800,00", method: "Boleto", status: "Pendente", transactionId: "-" },
  { patient: "João Pereira", date: "2024-07-27", procedure: "Restauração", value: "R$ 350,00", method: "Cartão de Crédito", status: "Em análise", transactionId: "txn_789ghi" },
];

const conveniosBilling = [
    { convenios: "Amil Dental", patient: "José Santos", tuss: "85400150", value: "R$ 200,00", status: "Enviado", lastUpdate: "28/07/2024" },
    { convenios: "Bradesco Saúde", patient: "Fernanda Lima", tuss: "82000388", value: "R$ 450,00", status: "Aprovado", lastUpdate: "27/07/2024" },
    { convenios: "SulAmérica Odonto", patient: "Ricardo Alves", tuss: "86000415", value: "R$ 180,00", status: "Glosado", lastUpdate: "26/07/2024" },
    { convenios: "OdontoPrev", patient: "Beatriz Oliveira", tuss: "85100100", value: "R$ 90,00", status: "Aguardando Envio", lastUpdate: "29/07/2024" },
];

const financialHistory = [
    { date: "29/07/2024", type: "Recebimento (PIX)", value: "R$ 250,00", user: "recepcionista" },
    { date: "28/07/2024", type: "Repasse (Convênio)", value: "R$ 4.500,00", user: "sistema" },
    { date: "28/07/2024", type: "Estorno (Cartão)", value: "- R$ 150,00", user: "financeiro" },
    { date: "27/07/2024", type: "Ajuste de Caixa", value: "R$ 50,00", user: "coordenador" },
];

const receipts = [
    { patient: "Ana Silva", date: "29/07/2024", value: "R$ 1.200,00", type: "Recibo de Pagamento" },
    { patient: "Carlos Souza", date: "28/07/2024", value: "R$ 250,00", type: "Nota Fiscal de Serviço" },
];


// --- Sub-components for each section ---

const ResumoFinanceiro = () => (
    <header id="painel-principal" className="scroll-mt-20">
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
            {financeiroStats.map(stat => <StatCard key={stat.title} {...stat} />)}
        </div>
    </header>
);

const PagamentosRecebimentos = () => {
    const getStatusVariant = (status: string) => {
        switch (status) {
            case 'Pago': return 'bg-[#7CFC00]/20 dark:bg-[#32CD32]/20 text-[#7CFC00] dark:text-[#32CD32]';
            case 'Pendente': return 'bg-[#FFDE21]/20 dark:bg-[#FFD700]/20 text-[#FFDE21] dark:text-[#FFD700]';
            case 'Em análise': return 'bg-blue-500/20 text-blue-500';
            default: return 'secondary';
        }
    };
    return (
        <section id="movimentacoes" className="scroll-mt-20">
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2"><DollarSign/> Pagamentos e Recebimentos</CardTitle>
                    <CardDescription>Lista de todas as transações financeiras recentes.</CardDescription>
                </CardHeader>
                <CardContent>
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead>Paciente</TableHead>
                                <TableHead>Data</TableHead>
                                <TableHead>Procedimento</TableHead>
                                <TableHead>Valor</TableHead>
                                <TableHead>Forma de Pagamento</TableHead>
                                <TableHead>Status</TableHead>
                                <TableHead>ID da Transação</TableHead>
                                <TableHead className="text-right">Ação</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {payments.map((payment, index) => (
                                <TableRow key={index}>
                                    <TableCell className="font-medium">{payment.patient}</TableCell>
                                    <TableCell>{payment.date}</TableCell>
                                    <TableCell>{payment.procedure}</TableCell>
                                    <TableCell>{payment.value}</TableCell>
                                    <TableCell>{payment.method}</TableCell>
                                    <TableCell>
                                        <Badge variant="secondary" className={cn('font-semibold', getStatusVariant(payment.status))}>
                                            {payment.status}
                                        </Badge>
                                    </TableCell>
                                    <TableCell className="font-mono text-xs">{payment.transactionId}</TableCell>
                                    <TableCell className="text-right">
                                        <Button variant="outline" size="sm">Ver Detalhes</Button>
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </CardContent>
            </Card>
        </section>
    );
};


const FaturamentoConvenios = () => {
    const getStatusVariant = (status: string) => {
        switch (status) {
            case 'Aprovado': return 'bg-[#7CFC00]/20 dark:bg-[#32CD32]/20 text-[#7CFC00] dark:text-[#32CD32]';
            case 'Aguardando Envio': return 'bg-[#FFDE21]/20 dark:bg-[#FFD700]/20 text-[#FFDE21] dark:text-[#FFD700]';
            case 'Glosado': return 'bg-[#FF2C2C]/20 dark:bg-[#FF4040]/20 text-[#FF2C2C] dark:text-[#FF4040]';
            case 'Enviado': return 'bg-blue-500/20 text-blue-500';
            default: return 'secondary';
        }
    };

    return (
        <section id="faturamento-convenios" className="scroll-mt-20">
            <Card>
                <CardHeader className="flex-row items-center justify-between">
                    <div>
                        <CardTitle className="flex items-center gap-2"><Landmark/> Faturamento por Convênios</CardTitle>
                        <CardDescription>Acompanhe o status das cobranças para operadoras de saúde.</CardDescription>
                    </div>
                    <Button><PlusCircle className="mr-2"/> Registrar Nova Cobrança</Button>
                </CardHeader>
                <CardContent>
                    <Table>
                         <TableHeader>
                            <TableRow>
                                <TableHead>Convênio</TableHead>
                                <TableHead>Paciente</TableHead>
                                <TableHead>Código TUSS</TableHead>
                                <TableHead>Valor</TableHead>
                                <TableHead>Status</TableHead>
                                <TableHead>Última Atualização</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {conveniosBilling.map((billing, index) => (
                                <TableRow key={index}>
                                    <TableCell>{billing.convenios}</TableCell>
                                    <TableCell>{billing.patient}</TableCell>
                                    <TableCell className="font-mono text-xs">{billing.tuss}</TableCell>
                                    <TableCell>{billing.value}</TableCell>
                                    <TableCell>
                                        <Badge variant="secondary" className={cn('font-semibold', getStatusVariant(billing.status))}>
                                            {billing.status}
                                        </Badge>
                                    </TableCell>
                                    <TableCell>{billing.lastUpdate}</TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </CardContent>
            </Card>
        </section>
    );
};

const HistoricoFinanceiro = () => (
    <section id="historico-financeiro" className="scroll-mt-20">
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><History/> Histórico Financeiro Geral</CardTitle>
                <CardDescription>Registro de todas as movimentações financeiras anteriores.</CardDescription>
            </CardHeader>
            <CardContent>
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>Data</TableHead>
                            <TableHead>Tipo de Operação</TableHead>
                            <TableHead>Valor</TableHead>
                            <TableHead>Usuário Responsável</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {financialHistory.map((entry, index) => (
                             <TableRow key={index}>
                                <TableCell>{entry.date}</TableCell>
                                <TableCell>
                                    <Badge variant={entry.value.startsWith('-') ? "destructive" : 'outline'} className={cn('font-semibold')}>
                                        {entry.type}
                                    </Badge>
                                </TableCell>
                                <TableCell className={cn(entry.value.startsWith('-') ? 'text-[#FF2C2C] dark:text-[#FF4040]' : 'text-[#7CFC00] dark:text-[#32CD32]')}>
                                    {entry.value}
                                </TableCell>
                                <TableCell>{entry.user}</TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </CardContent>
        </Card>
    </section>
);


const RelatoriosIndicadores = () => (
    <section id="relatorios" className="grid grid-cols-1 lg:grid-cols-2 gap-8 scroll-mt-20">
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><FilePieChart/> Formas de Pagamento (Mês)</CardTitle>
            </CardHeader>
            <CardContent className="h-[250px] flex flex-col justify-center gap-4">
                <div className="space-y-2">
                    <div className="flex justify-between text-sm"><span>PIX</span><span>45%</span></div>
                    <div className="w-full bg-muted rounded-full h-2.5"><div className="bg-primary h-2.5 rounded-full" style={{width: "45%"}}></div></div>
                </div>
                 <div className="space-y-2">
                    <div className="flex justify-between text-sm"><span>Cartão de Crédito</span><span>35%</span></div>
                    <div className="w-full bg-muted rounded-full h-2.5"><div className="bg-accent h-2.5 rounded-full" style={{width: "35%"}}></div></div>
                </div>
                 <div className="space-y-2">
                    <div className="flex justify-between text-sm"><span>Boleto</span><span>15%</span></div>
                    <div className="w-full bg-muted rounded-full h-2.5"><div className="bg-secondary h-2.5 rounded-full" style={{width: "15%"}}></div></div>
                </div>
                 <div className="space-y-2">
                    <div className="flex justify-between text-sm"><span>Outros</span><span>5%</span></div>
                    <div className="w-full bg-muted rounded-full h-2.5"><div className="bg-muted-foreground h-2.5 rounded-full" style={{width: "5%"}}></div></div>
                </div>
            </CardContent>
        </Card>
        <Card>
            <CardHeader>
                <CardTitle>Receita Mensal (Últimos 6 Meses)</CardTitle>
            </CardHeader>
            <CardContent className="h-[250px] flex items-end gap-2 p-4 bg-muted/50 rounded-lg">
                {/* Simulação de gráfico com divs */}
                <div className="flex-1 flex flex-col justify-end items-center gap-1">
                    <div className="bg-primary w-full rounded-t-sm" style={{height: '60%'}}></div><span className="text-xs text-muted-foreground">Jan</span>
                </div>
                <div className="flex-1 flex flex-col justify-end items-center gap-1">
                    <div className="bg-primary w-full rounded-t-sm" style={{height: '80%'}}></div><span className="text-xs text-muted-foreground">Fev</span>
                </div>
                <div className="flex-1 flex flex-col justify-end items-center gap-1">
                   <div className="bg-primary w-full rounded-t-sm" style={{height: '75%'}}></div><span className="text-xs text-muted-foreground">Mar</span>
                </div>
                 <div className="flex-1 flex flex-col justify-end items-center gap-1">
                   <div className="bg-primary w-full rounded-t-sm" style={{height: '90%'}}></div><span className="text-xs text-muted-foreground">Abr</span>
                </div>
                <div className="flex-1 flex flex-col justify-end items-center gap-1">
                   <div className="bg-primary w-full rounded-t-sm" style={{height: '70%'}}></div><span className="text-xs text-muted-foreground">Mai</span>
                </div>
                <div className="flex-1 flex flex-col justify-end items-center gap-1">
                   <div className="bg-primary w-full rounded-t-sm" style={{height: '85%'}}></div><span className="text-xs text-muted-foreground">Jun</span>
                </div>
            </CardContent>
        </Card>
    </section>
);


const ConfiguracoesFinanceiras = () => (
    <section id="configuracoes-financeiras" className="scroll-mt-20">
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><Settings/> Configurações Financeiras</CardTitle>
                <CardDescription>Defina parâmetros, políticas e informações fiscais da clínica.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
                 <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div className="space-y-4 p-4 border rounded-lg">
                        <h3 className="font-semibold">Políticas e Repasses</h3>
                         <div className="flex items-center justify-between">
                            <Label htmlFor="cancel-fee" className="flex flex-col gap-1"><span>Cobrar taxa por falta (não comparecimento)</span><span className="text-xs text-muted-foreground">Aplica uma taxa se o paciente não cancelar a tempo.</span></Label>
                            <Switch id="cancel-fee" />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="cancel-hours">Prazo para Cancelamento (horas)</Label>
                            <Input id="cancel-hours" type="number" defaultValue="24" />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="dentist-commission">Repasse padrão dos Dentistas (%)</Label>
                            <Input id="dentist-commission" type="number" defaultValue="40" />
                        </div>
                    </div>
                     <div className="space-y-4 p-4 border rounded-lg">
                        <h3 className="font-semibold">Informações Fiscais</h3>
                         <div className="space-y-2">
                            <Label htmlFor="company-name">Razão Social</Label>
                            <Input id="company-name" defaultValue="DentiBot Clínica Odontológica LTDA" />
                        </div>
                         <div className="space-y-2">
                            <Label htmlFor="company-cnpj">CNPJ</Label>
                            <Input id="company-cnpj" defaultValue="00.000.000/0001-00" />
                        </div>
                         <div className="space-y-2">
                            <Label htmlFor="company-address">Endereço Fiscal</Label>
                            <Input id="company-address" defaultValue="Rua Fictícia, 123 - Centro" />
                        </div>
                    </div>
                </div>
                 <div className="flex justify-end gap-2">
                    <Button variant="ghost">Restaurar Padrões</Button>
                    <Button>Salvar Configurações</Button>
                </div>
            </CardContent>
        </Card>
    </section>
);


const ComprovantesRecibos = () => (
    <section id="comprovantes" className="scroll-mt-20">
        <Card>
            <CardHeader className="flex-row items-center justify-between">
                 <div>
                    <CardTitle className="flex items-center gap-2"><FileText/> Comprovantes e Recibos</CardTitle>
                    <CardDescription>Visualize documentos emitidos e gere novos recibos.</CardDescription>
                </div>
                <Button><PlusCircle className="mr-2"/> Gerar Recibo Manual</Button>
            </CardHeader>
            <CardContent>
                 <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>Paciente</TableHead>
                            <TableHead>Data</TableHead>
                            <TableHead>Valor</TableHead>
                            <TableHead>Tipo de Documento</TableHead>
                            <TableHead className="text-right">Ação</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {receipts.map((receipt, index) => (
                            <TableRow key={index}>
                                <TableCell className="font-medium">{receipt.patient}</TableCell>
                                <TableCell>{receipt.date}</TableCell>
                                <TableCell>{receipt.value}</TableCell>
                                <TableCell>
                                    <Badge variant="secondary">{receipt.type}</Badge>
                                </TableCell>
                                <TableCell className="text-right">
                                    <Button variant="outline" size="icon">
                                        <Download className="h-4 w-4" />
                                    </Button>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </CardContent>
        </Card>
    </section>
);


export default function FinanceiroPage() {
    return (
        <div className='space-y-8'>
            <ResumoFinanceiro />
            <PagamentosRecebimentos />
            <FaturamentoConvenios />
            <HistoricoFinanceiro />
            <RelatoriosIndicadores />
            <ConfiguracoesFinanceiras />
            <ComprovantesRecibos />
        </div>
    );
}
