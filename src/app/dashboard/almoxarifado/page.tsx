// src/app/dashboard/almoxarifado/page.tsx
'use client';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { cn } from "@/lib/utils";
import { ArrowDown, ArrowUp, Syringe, Warehouse, FlaskConical, Boxes, PackagePlus, Truck, AlertTriangle, Package, Route, ArrowRightLeft, PlusCircle, CheckCircle, Clock } from "lucide-react";
import { StatCard } from '@/components/dashboard/StatCard';
import type { StatCard as StatCardType } from '@/lib/types';
import { Alert, AlertTitle, AlertDescription } from "@/components/ui/alert";


const almoxarifadoStats: StatCardType[] = [
    { title: "Itens em Estoque", value: "358", icon: Boxes, change: "+20 hoje", iconColor: "text-blue-500" },
    { title: "Itens com Estoque Baixo", value: "12", icon: AlertTriangle, change: "+2 hoje", iconColor: "dark:text-[#FFD700] text-[#FFDE21]" },
    { title: "Pedidos de Compra", value: "5", icon: PackagePlus, change: "2 novos", iconColor: "dark:text-[#32CD32] text-[#7CFC00]" },
    { title: "Entregas Pendentes", value: "3", icon: Truck, change: "1 atrasada", iconColor: "dark:text-[#FF4040] text-[#FF2C2C]" },
];

const inventory = [
  { id: 1, name: "Luvas Descartáveis (Caixa)", quantity: 25, min: 20, status: "Em Estoque" },
  { id: 2, name: "Máscaras Cirúrgicas (Caixa)", quantity: 10, min: 15, status: "Estoque Baixo" },
  { id: 3, name: "Anestésico Lidocaína", quantity: 5, min: 10, status: "Crítico" },
  { id: 4, name: "Agulhas Gengivais", quantity: 50, min: 50, status: "Em Estoque" },
  { id: 5, name: "Gaze Estéril (Pacote)", quantity: 80, min: 40, status: "Em Estoque" },
];

const kits = [
    { id: 'K001', name: "Kit de Exame Clínico #1", status: "Disponível" },
    { id: 'K002', name: "Kit de Restauração #3", status: "Em uso" },
    { id: 'K003', name: "Kit de Cirurgia #2", status: "Em esterilização" },
    { id: 'K004', name: "Kit de Exame Clínico #2", status: "Bloqueado" },
];

const sterilizationCycles = [
    { id: "C-001", operator: "João Silva", start: "29/07/2024 09:00", end: "29/07/2024 10:00", result: "Aprovado" },
    { id: "C-002", operator: "Maria Lima", start: "29/07/2024 10:15", end: "29/07/2024 11:15", result: "Aprovado" },
    { id: "C-003", operator: "João Silva", start: "29/07/2024 11:30", end: "29/07/2024 12:30", result: "Reprovado" },
];

const kitHistory = [
     { kit: 'K003', time: "29/07/2024 12:30", action: "Liberado da esterilização", by: "Maria Lima" },
     { kit: 'K003', time: "29/07/2024 11:15", action: "Início da esterilização (Ciclo C-002)", by: "Maria Lima" },
     { kit: 'K003', time: "29/07/2024 10:45", action: "Devolvido ao almoxarifado", by: "Dr. Ana Costa" },
     { kit: 'K003', time: "29/07/2024 09:30", action: "Utilizado no consultório 02", by: "Dr. Ana Costa" },
];

const highConsumptionItems = [
    { name: "Máscaras Cirúrgicas (Caixa)", consumed: 15, period: "últimos 7 dias" },
    { name: "Luvas Descartáveis (Caixa)", consumed: 12, period: "últimos 7 dias" },
];


// --- Sub-components for each section ---

const InventarioGeral = () => {
    const getStatusColor = (status: string) => {
        switch (status) {
            case 'Estoque Baixo': return 'bg-[#FFDE21]/20 dark:bg-[#FFD700]/20 text-[#FFDE21] dark:text-[#FFD700]';
            case 'Crítico': return 'bg-[#FF2C2C]/20 dark:bg-[#FF4040]/20 text-[#FF2C2C] dark:text-[#FF4040]';
            default: return 'bg-[#7CFC00]/20 dark:bg-[#32CD32]/20 text-[#7CFC00] dark:text-[#32CD32]';
        }
    };

    return (
        <section id="inventario" className='space-y-6 scroll-mt-20'>
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2"><Warehouse /> Inventário Geral</CardTitle>
                    <CardDescription>Visão completa de todos os itens e consumíveis.</CardDescription>
                </CardHeader>
                <CardContent>
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead>Material</TableHead>
                                <TableHead>Quantidade</TableHead>
                                <TableHead>Nível Mínimo</TableHead>
                                <TableHead>Status</TableHead>
                                <TableHead className="text-right">Ações Rápidas</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {inventory.map(item => (
                                <TableRow key={item.id}>
                                    <TableCell>{item.name}</TableCell>
                                    <TableCell>{item.quantity}</TableCell>
                                    <TableCell>{item.min}</TableCell>
                                    <TableCell>
                                        <Badge variant="secondary" className={cn("font-semibold", getStatusColor(item.status))}>
                                            {item.status}
                                        </Badge>
                                    </TableCell>
                                    <TableCell className="text-right space-x-2">
                                        <Button size="icon" variant="outline"><ArrowUp className="size-4 text-[#7CFC00] dark:text-[#32CD32]" /></Button>
                                        <Button size="icon" variant="outline"><ArrowDown className="size-4 text-[#FF2C2C] dark:text-[#FF4040]" /></Button>
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

const MovimentacaoMateriais = () => (
    <section id="movimentacao" className='space-y-6 scroll-mt-20'>
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><ArrowRightLeft /> Entrada e Saída de Materiais</CardTitle>
                <CardDescription>Registre novas movimentações no estoque.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <div className="space-y-2 md:col-span-2">
                        <Label htmlFor="item-select">Item</Label>
                        <Select>
                            <SelectTrigger id="item-select">
                                <SelectValue placeholder="Selecionar item" />
                            </SelectTrigger>
                            <SelectContent>
                                {inventory.map(item => <SelectItem key={item.id} value={String(item.id)}>{item.name}</SelectItem>)}
                            </SelectContent>
                        </Select>
                    </div>
                    <div className="space-y-2">
                        <Label htmlFor="quantity">Quantidade</Label>
                        <Input id="quantity" type="number" placeholder="0"/>
                    </div>
                </div>
                <div className="space-y-2">
                    <Label htmlFor="movement-type">Tipo de Movimentação</Label>
                    <Select>
                        <SelectTrigger id="movement-type">
                            <SelectValue placeholder="Selecione o tipo" />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value="entrada">Entrada</SelectItem>
                            <SelectItem value="saida">Saída</SelectItem>
                        </SelectContent>
                    </Select>
                </div>
                <Button className="w-full md:w-auto">
                    Confirmar Registro
                </Button>
            </CardContent>
        </Card>
    </section>
);

const ControleKits = () => {
    const getKitStatusColor = (status: string) => {
        switch (status) {
            case 'Disponível': return 'bg-[#7CFC00]/20 dark:bg-[#32CD32]/20 text-[#7CFC00] dark:text-[#32CD32]';
            case 'Em uso': return 'bg-blue-500/20 text-blue-600';
            case 'Em esterilização': return 'bg-[#FFDE21]/20 dark:bg-[#FFD700]/20 text-[#FFDE21] dark:text-[#FFD700]';
            case 'Bloqueado': return 'bg-[#FF2C2C]/20 dark:bg-[#FF4040]/20 text-[#FF2C2C] dark:text-[#FF4040]';
            default: return 'secondary';
        }
    };
    return (
        <section id="kits" className="scroll-mt-20">
            <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><Package /> Controle de Kits e Instrumentais</CardTitle>
                <CardDescription>Rastreabilidade de kits reutilizáveis e seus status.</CardDescription>
            </CardHeader>
            <CardContent>
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>Kit</TableHead>
                            <TableHead>Status Atual</TableHead>
                            <TableHead className="text-right">Alterar Status</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {kits.map(kit => (
                            <TableRow key={kit.id}>
                                <TableCell className="font-medium flex items-center gap-2"><Syringe className="text-muted-foreground" /> {kit.name}</TableCell>
                                <TableCell>
                                    <Badge variant="secondary" className={cn('font-semibold', getKitStatusColor(kit.status))}>{kit.status}</Badge>
                                </TableCell>
                                <TableCell className="text-right">
                                    <Select defaultValue={kit.status}>
                                        <SelectTrigger className="w-[180px]">
                                            <SelectValue placeholder="Mudar status..." />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="Disponível">Disponível</SelectItem>
                                            <SelectItem value="Em uso">Em Uso</SelectItem>
                                            <SelectItem value="Em esterilização">Em Esterilização</SelectItem>
                                            <SelectItem value="Bloqueado">Bloqueado</SelectItem>
                                        </SelectContent>
                                    </Select>
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

const RegistroEsterilizacao = () => (
     <section id="esterilizacao" className="scroll-mt-20">
        <Card>
            <CardHeader className="flex-row items-center justify-between">
                <div>
                    <CardTitle className="flex items-center gap-2"><FlaskConical /> Registros de Esterilização</CardTitle>
                    <CardDescription>Histórico dos ciclos de esterilização.</CardDescription>
                </div>
                <Button>
                    <PlusCircle className="mr-2" />
                    Novo Ciclo
                </Button>
            </CardHeader>
            <CardContent>
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>Ciclo ID</TableHead>
                            <TableHead>Operador</TableHead>
                            <TableHead>Início</TableHead>
                            <TableHead>Término</TableHead>
                            <TableHead className="text-right">Resultado</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                    {sterilizationCycles.map(cycle => (
                        <TableRow key={cycle.id}>
                            <TableCell>{cycle.id}</TableCell>
                            <TableCell>{cycle.operator}</TableCell>
                            <TableCell>{cycle.start}</TableCell>
                            <TableCell>{cycle.end}</TableCell>
                            <TableCell className="text-right">
                                <Badge variant={cycle.result === "Aprovado" ? "secondary" : "destructive"}
                                    className={cn(
                                        "font-semibold",
                                        cycle.result === "Aprovado" && "bg-[#7CFC00]/20 dark:bg-[#32CD32]/20 text-[#7CFC00] dark:text-[#32CD32]",
                                        cycle.result === "Reprovado" && "bg-[#FF2C2C]/20 dark:bg-[#FF4040]/20 text-[#FF2C2C] dark:text-[#FF4040]"
                                    )}>
                                    {cycle.result}
                                </Badge>
                            </TableCell>
                        </TableRow>
                    ))}
                    </TableBody>
                </Table>
            </CardContent>
        </Card>
     </section>
);

const Rastreabilidade = () => (
    <section id="rastreabilidade" className="scroll-mt-20">
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><Route /> Rastreabilidade</CardTitle>
                <CardDescription>Acompanhe o histórico de movimentação de um kit.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
                 <div className="flex gap-4">
                    <Select>
                        <SelectTrigger>
                            <SelectValue placeholder="Selecione um kit para rastrear..." />
                        </SelectTrigger>
                        <SelectContent>
                            {kits.map(kit => <SelectItem key={kit.id} value={kit.id}>{kit.name}</SelectItem>)}
                        </SelectContent>
                    </Select>
                    <Button>Buscar Histórico</Button>
                </div>
                <div className="border rounded-lg p-4 space-y-4">
                    <h4 className="font-semibold">Histórico simulado para: Kit de Cirurgia #2</h4>
                     {kitHistory.map((entry, index) => (
                        <div key={index} className="flex items-start gap-4">
                           <div className="flex flex-col items-center">
                                <div className="rounded-full bg-primary/20 p-2 text-primary">
                                    {entry.action.includes('Liberado') ? <CheckCircle className="size-5" /> : <Clock className="size-5" />}
                                </div>
                               {index < kitHistory.length - 1 && <div className="w-px flex-grow bg-border my-1"></div>}
                           </div>
                           <div>
                                <p className="font-medium">{entry.action}</p>
                                <p className="text-sm text-muted-foreground">{entry.time} • Por: {entry.by}</p>
                           </div>
                        </div>
                     ))}
                </div>
            </CardContent>
        </Card>
    </section>
);

const AlertasConsumo = () => (
    <section id="alertas-consumo" className="scroll-mt-20">
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><AlertTriangle /> Alertas e Consumos</CardTitle>
                <CardDescription>Itens críticos e tendências de consumo.</CardDescription>
            </CardHeader>
            <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <Alert variant="destructive" className="border-[#FF2C2C]/50 dark:border-[#FF4040]/50 bg-[#FF2C2C]/10 dark:bg-[#FF4040]/10 text-[#FF2C2C] dark:text-[#FF4040]">
                    <AlertTriangle className="h-4 w-4 !text-[#FF2C2C] dark:!text-[#FF4040]" />
                    <AlertTitle>Itens com Estoque Crítico!</AlertTitle>
                    <AlertDescription>
                        <ul>
                            {inventory.filter(i => i.status === 'Crítico').map(item => (
                                <li key={item.id}>• {item.name} ({item.quantity} un.)</li>
                            ))}
                        </ul>
                    </AlertDescription>
                </Alert>
                 <Alert className="border-[#FFDE21]/50 dark:border-[#FFD700]/50 bg-[#FFDE21]/10 dark:bg-[#FFD700]/10 text-[#FFDE21] dark:text-[#FFD700]">
                    <AlertTriangle className="h-4 w-4 !text-[#FFDE21] dark:!text-[#FFD700]" />
                    <AlertTitle>Itens com Estoque Baixo</AlertTitle>
                    <AlertDescription>
                         <ul>
                            {inventory.filter(i => i.status === 'Estoque Baixo').map(item => (
                                <li key={item.id}>• {item.name} ({item.quantity} un.)</li>
                            ))}
                        </ul>
                    </AlertDescription>
                </Alert>
                <Card className="md:col-span-2">
                    <CardHeader>
                        <CardTitle className="text-lg">Materiais de Alta Rotatividade</CardTitle>
                    </CardHeader>
                    <CardContent>
                        {highConsumptionItems.map(item => (
                            <div key={item.name} className="flex justify-between items-center mb-2">
                                <p>{item.name}</p>
                                <p className="font-semibold">{item.consumed} un. <span className="text-xs text-muted-foreground">{item.period}</span></p>
                            </div>
                        ))}
                    </CardContent>
                </Card>
            </CardContent>
        </Card>
    </section>
);

const PainelPrincipal = () => (
     <header id="painel-principal" className="scroll-mt-20">
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
            {almoxarifadoStats.map(stat => <StatCard key={stat.title} {...stat} />)}
        </div>
    </header>
);

export default function AlmoxarifadoPage() {
    return (
        <div className="space-y-8">
            <PainelPrincipal />
            <InventarioGeral />
            <MovimentacaoMateriais />
            <ControleKits />
            <RegistroEsterilizacao />
            <Rastreabilidade />
            <AlertasConsumo />
        </div>
    );
}
