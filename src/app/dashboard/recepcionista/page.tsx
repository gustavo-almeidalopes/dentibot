// src/app/dashboard/recepcionista/page.tsx
'use client';

import { Calendar, CheckCheck, History, Wallet, Eye, AlertCircle, UserPlus, Phone, CalendarPlus } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Textarea } from '@/components/ui/textarea';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { StatCard } from '@/components/dashboard/StatCard';
import type { StatCard as StatCardType } from '@/lib/types';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { cn } from '@/lib/utils';


const receptionistStats: StatCardType[] = [
    { title: "Consultas Agendadas (Hoje)", value: "34", icon: Calendar, change: "+5 reagendamentos", iconColor: "text-blue-500" },
    { title: "Confirmações Pendentes", value: "7", icon: Phone, change: "2 são prioritárias", iconColor: "dark:text-[#FFD700] text-[#FFDE21]" },
    { title: "Novos Pacientes (Hoje)", value: "3", icon: UserPlus, change: "+1 da manhã", iconColor: "dark:text-[#32CD32] text-[#7CFC00]" },
];

const confirmations = [
    { patient: "Ana Silva", time: "2024-08-15 10:00", status: "Confirmado" },
    { patient: "Carlos Souza", time: "2024-08-15 11:30", status: "Aguardando" },
    { patient: "Mariana Costa", time: "2024-08-15 14:00", status: "Confirmado" },
    { patient: "João Pereira", time: "2024-08-15 15:30", status: "Aguardando" },
];

const PainelPrincipal = () => (
    <header id="painel-principal" className="space-y-6 scroll-mt-20">
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {receptionistStats.map(stat => <StatCard key={stat.title} {...stat} />)}
        </div>
    </header>
);

// --- Sub-components for each section ---

const AgendamentoInteligente = () => (
    <section id="agendamento" className="scroll-mt-20">
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><CalendarPlus /> Agendamento Inteligente</CardTitle>
                <CardDescription>Preencha para adicionar uma nova consulta.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    <div className="space-y-2 md:col-span-2 lg:col-span-1">
                        <Label htmlFor="patient-name">Nome Completo do Paciente</Label>
                        <Input id="patient-name" placeholder="Nome completo" />
                    </div>
                     <div className="space-y-2">
                        <Label htmlFor="dentist-select">Dentista Responsável</Label>
                        <Select>
                            <SelectTrigger id="dentist-select">
                                <SelectValue placeholder="Selecione o dentista" />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value="ana-costa">Dr. Ana Costa</SelectItem>
                                <SelectItem value="joao-silva">Dr. João Silva</SelectItem>
                                <SelectItem value="maria-lima">Dra. Maria Lima</SelectItem>
                            </SelectContent>
                        </Select>
                    </div>
                     <div className="space-y-2">
                        <Label htmlFor="procedure">Tipo de Procedimento</Label>
                        <Input id="procedure" placeholder="Ex: Limpeza, Avaliação" />
                    </div>
                    <div className="space-y-2">
                        <Label htmlFor="date">Data da Consulta</Label>
                        <Input id="date" type="date" />
                    </div>
                    <div className="space-y-2">
                        <Label htmlFor="time">Horário da Consulta</Label>
                        <Input id="time" type="time" />
                    </div>
                </div>
                <div className="flex flex-col md:flex-row gap-4 items-center pt-4">
                    <Button className="w-full md:w-auto">Agendar Consulta</Button>
                    <Alert className="border-primary/30 bg-primary/10">
                    <AlertCircle className="h-4 w-4 !text-primary" />
                    <AlertTitle className="text-primary">Fluxo Automático</AlertTitle>
                    <AlertDescription className="text-primary/80">
                        O sistema iniciará o fluxo de confirmação com o paciente automaticamente.
                    </AlertDescription>
                    </Alert>
                </div>
            </CardContent>
        </Card>
    </section>
);

const Confirmacoes = () => {
     const getStatusVariant = (status: string) => {
        switch (status) {
            case 'Confirmado': return 'bg-[#7CFC00]/20 dark:bg-[#32CD32]/20 text-[#7CFC00] dark:text-[#32CD32]';
            case 'Aguardando': return 'bg-[#FFDE21]/20 dark:bg-[#FFD700]/20 text-[#FFDE21] dark:text-[#FFD700]';
            default: return 'secondary';
        }
    };
    return (
        <section id="confirmacoes" className="scroll-mt-20">
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2"><CheckCheck /> Confirmações</CardTitle>
                    <CardDescription>Status das próximas consultas.</CardDescription>
                </CardHeader>
                <CardContent>
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead>Paciente</TableHead>
                                <TableHead>Data e Hora</TableHead>
                                <TableHead>Status</TableHead>
                                <TableHead className="text-right">Ação</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {confirmations.map(c => (
                                <TableRow key={c.patient}>
                                    <TableCell>{c.patient}</TableCell>
                                    <TableCell>{c.time}</TableCell>
                                    <TableCell>
                                        <Badge variant="secondary" className={cn('font-semibold', getStatusVariant(c.status))}>
                                            {c.status}
                                        </Badge>
                                    </TableCell>
                                    <TableCell className="text-right">
                                        <Button variant="outline" size="sm">
                                            <Eye className="mr-2 h-4 w-4" />
                                            Visualizar
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
};

const Reagendamento = () => (
    <section id="reagendamento" className="scroll-mt-20">
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><History /> Reagendamento</CardTitle>
                <CardDescription>Selecione uma consulta para reagendar.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
                 <div className="space-y-2">
                    <Label>Selecionar Consulta</Label>
                    <Select>
                        <SelectTrigger>
                            <SelectValue placeholder="Buscar paciente para reagendar..." />
                        </SelectTrigger>
                        <SelectContent>
                             {confirmations.map(c => (
                                <SelectItem key={c.patient} value={c.patient}>{c.patient} - {c.time}</SelectItem>
                             ))}
                        </SelectContent>
                    </Select>
                </div>
                <div className="space-y-2">
                    <Label>Nova Data e Horário</Label>
                    <div className="grid grid-cols-2 gap-4">
                        <Input type="date" />
                        <Input type="time" />
                    </div>
                </div>
                <Button className="w-full">Confirmar Reagendamento</Button>
            </CardContent>
        </Card>
    </section>
);

const ObservacoesEspeciais = () => (
    <section id="observacoes" className="scroll-mt-20">
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><AlertCircle /> Observações Especiais</CardTitle>
                <CardDescription>Registre informações importantes sobre os pacientes.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
                <div className="space-y-2">
                    <Label>Selecionar Paciente</Label>
                     <Select>
                        <SelectTrigger>
                            <SelectValue placeholder="Selecione um paciente..." />
                        </SelectTrigger>
                        <SelectContent>
                             <SelectItem value="ana-silva">Ana Silva</SelectItem>
                             <SelectItem value="carlos-souza">Carlos Souza</SelectItem>
                             <SelectItem value="mariana-costa">Mariana Costa</SelectItem>
                        </SelectContent>
                    </Select>
                </div>
                <Textarea placeholder="Adicionar observação (ex: alergias, necessidades especiais, limitações físicas)..." rows={4} />
                <Button>Salvar Observação</Button>
            </CardContent>
        </Card>
    </section>
);

const Convenios = () => (
    <section id="convenios" className="scroll-mt-20">
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><Wallet /> Integração com Convênios</CardTitle>
                <CardDescription>Solicite autorizações e verifique status.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
                 <div className="space-y-2">
                    <Label>Selecionar Paciente</Label>
                     <Select>
                        <SelectTrigger>
                            <SelectValue placeholder="Selecione um paciente..." />
                        </SelectTrigger>
                        <SelectContent>
                             <SelectItem value="ana-silva">Ana Silva</SelectItem>
                             <SelectItem value="carlos-souza">Carlos Souza</SelectItem>
                             <SelectItem value="mariana-costa">Mariana Costa</SelectItem>
                        </SelectContent>
                    </Select>
                </div>
                <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                         <Label>Nº da Carteirinha</Label>
                         <Input placeholder="000.000.000-00" />
                    </div>
                     <div className="space-y-2">
                         <Label>Procedimento</Label>
                        <Input placeholder="Código TUSS ou descrição" />
                    </div>
                </div>
                <div className="flex gap-2">
                    <Button className="w-full">Solicitar Autorização</Button>
                    <Button variant="secondary" className="w-full">Verificar Status</Button>
                </div>
                <Alert variant="default" className="bg-muted/50">
                    <AlertCircle className="h-4 w-4" />
                    <AlertDescription>
                        O sistema retornará "Aprovação", "Pendência" ou "Glosa" com base nos códigos TISS/TUSS.
                    </AlertDescription>
                </Alert>
            </CardContent>
        </Card>
    </section>
);

export default function ReceptionistPage() {
    return (
        <div className="space-y-8">
            <PainelPrincipal />
            <AgendamentoInteligente />
            <Confirmacoes />
            <Reagendamento />
            <ObservacoesEspeciais />
            <Convenios />
        </div>
    );
}
