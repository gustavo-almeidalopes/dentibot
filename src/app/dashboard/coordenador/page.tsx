// src/app/dashboard/coordenador/page.tsx
'use client';
import { Users, CheckCheck, History, Shield, BarChart3, Activity, BarChart2, UserPlus, FileText, Server, KeyRound, Settings2, ShieldCheck, FileCog, FileClock, Edit, Trash2 } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Switch } from '@/components/ui/switch';
import { Label } from '@/components/ui/label';
import { StatCard } from '@/components/dashboard/StatCard';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import type { StatCard as StatCardType } from '@/lib/types';
import { cn } from '@/lib/utils';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';


const coordenadorStats: StatCardType[] = [
    { title: "Usuários Ativos", value: "27", icon: Users, change: "+2 hoje", iconColor: "text-blue-500" },
    { title: "Logs (24h)", value: "1,254", icon: History, change: "+8%", iconColor: "text-purple-500" },
    { title: "Rotinas Pendentes", value: "3", icon: FileClock, change: "1 nova", iconColor: "dark:text-[#FFD700] text-[#FFDE21]" },
    { title: "Status do Sistema", value: "Operacional", icon: ShieldCheck, change: "", iconColor: "dark:text-[#32CD32] text-[#7CFC00]" },
];

const users = [
    { name: "Dr. Ana Costa", role: "Dentista", status: "Ativo" },
    { name: "Carlos Andrade", role: "Recepcionista", status: "Ativo" },
    { name: "Fernanda Lima", role: "Financeiro", status: "Inativo" },
    { name: "Beatriz Silva", role: "Almoxarifado", status: "Ativo" },
    { name: "José Pereira", role: "Paciente", status: "Ativo" },
];

const routines = [
    { name: "Protocolo de Anamnese v2.1", description: "Adiciona campo para alergias a medicamentos específicos.", sender: "Dr. Ana Costa", status: "Pendente" },
    { name: "Macro de Confirmação via WhatsApp", description: "Atualiza o texto da mensagem com link para reagendamento.", sender: "Carlos Andrade", status: "Pendente" },
];

const auditLogs = [
    { time: "2024-07-29 10:05:12", user: "Dr. Ana Costa", action: "Prescrição criada", details: "Paciente: João S." },
    { time: "2024-07-29 10:02:45", user: "Carlos Andrade", action: "Agendamento confirmado", details: "Paciente: Maria L." },
    { time: "2024-07-29 09:58:30", user: "sistema", action: "Backup automático", details: "Concluído com sucesso" },
    { time: "2024-07-29 09:45:10", user: "Beatriz Silva", action: "Saída de material", details: "Item: Luvas Descartáveis (Caixa), Qtd: 2" },
    { time: "2024-07-29 09:30:00", user: "Fernanda Lima", action: "Login falhou", details: "Tentativa de acesso com senha incorreta" },
    { time: "2024-07-29 09:25:50", user: "coordenador@dentibot.com", action: "Login bem-sucedido", details: "Acesso ao painel de coordenação" },
];


// --- Sub-components for each section ---

const DashboardAdministrativo = () => (
    <header id="painel-principal" className="scroll-mt-20 space-y-6">
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
            {coordenadorStats.map(stat => <StatCard key={stat.title} {...stat} />)}
        </div>
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><BarChart3/> Métricas Administrativas</CardTitle>
                <CardDescription>Visão geral do desempenho recente do sistema.</CardDescription>
            </CardHeader>
            <CardContent>
                <div className="h-64 bg-muted/50 rounded-lg flex items-center justify-center p-4">
                    <p className="text-center text-muted-foreground">
                        <BarChart2 className="w-12 h-12 mx-auto mb-2" />
                        Gráfico estático simulando cadastros, volume de consultas e taxa de confirmações.
                    </p>
                </div>
            </CardContent>
        </Card>
    </header>
);

const GestaoUsuarios = () => (
    <section id="gestao-usuarios" className="scroll-mt-20">
        <Card>
            <CardHeader className="flex-row items-center justify-between">
                <div>
                    <CardTitle className="flex items-center gap-2"><Users/> Gestão de Usuários e Permissões</CardTitle>
                    <CardDescription>Adicione, edite ou remova usuários do sistema.</CardDescription>
                </div>
                <Button> <UserPlus className="mr-2"/> Adicionar Usuário</Button>
            </CardHeader>
            <CardContent className="space-y-6">
                 <div className="space-y-4 p-6 border rounded-lg bg-muted/50 hidden"> {/* Formulário escondido por padrão */}
                    <h3 className="font-semibold text-lg">Novo Usuário</h3>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        <div className="space-y-2">
                            <Label htmlFor="new-user-name">Nome Completo</Label>
                            <Input id="new-user-name" placeholder="Nome do usuário" />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="new-user-email">E-mail</Label>
                            <Input id="new-user-email" type="email" placeholder="email@exemplo.com" />
                        </div>
                         <div className="space-y-2">
                            <Label htmlFor="new-user-role">Função</Label>
                            <Select>
                                <SelectTrigger id="new-user-role"><SelectValue placeholder="Selecione a função" /></SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="dentista">Dentista</SelectItem>
                                    <SelectItem value="recepcionista">Recepcionista</SelectItem>
                                    <SelectItem value="almoxarifado">Almoxarifado</SelectItem>
                                    <SelectItem value="financeiro">Financeiro</SelectItem>
                                    <SelectItem value="paciente">Paciente</SelectItem>
                                </SelectContent>
                            </Select>
                        </div>
                    </div>
                     <div className="flex items-center gap-4">
                        <Button>Salvar Usuário</Button>
                        <Button variant="ghost">Cancelar</Button>
                    </div>
                </div>
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>Nome</TableHead>
                            <TableHead>Função</TableHead>
                            <TableHead>Status</TableHead>
                            <TableHead className="text-right">Ações</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {users.map(user => (
                            <TableRow key={user.name}>
                                <TableCell className="font-medium">{user.name}</TableCell>
                                <TableCell>{user.role}</TableCell>
                                <TableCell>
                                    <Badge variant={user.status === 'Ativo' ? 'secondary' : 'outline'} className={cn('font-semibold', user.status === 'Ativo' ? 'bg-[#7CFC00]/20 dark:bg-[#32CD32]/20 text-[#7CFC00] dark:text-[#32CD32]' : 'bg-muted text-muted-foreground')}>
                                    {user.status}
                                    </Badge>
                                </TableCell>
                                <TableCell className="text-right space-x-2">
                                    <Button variant="outline" size="icon"><Edit className="size-4" /></Button>
                                    <Button variant="destructive" size="icon"><Trash2 className="size-4" /></Button>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </CardContent>
        </Card>
    </section>
);

const HomologacaoRotinas = () => (
    <section id="homologacao-rotinas" className="scroll-mt-20">
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><FileCog /> Homologação de Rotinas e Macros</CardTitle>
                <CardDescription>Aprove ou reprove protocolos e macros enviados pela equipe.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
                {routines.map(routine => (
                    <Card key={routine.name} className="bg-muted/50">
                        <CardHeader>
                            <CardTitle className="text-lg">{routine.name}</CardTitle>
                             <CardDescription>Enviado por: {routine.sender}</CardDescription>
                        </CardHeader>
                        <CardContent className="space-y-4">
                            <p className="text-sm">{routine.description}</p>
                            <Textarea placeholder="Adicionar comentário opcional..." />
                            <div className="flex gap-2">
                                <Button variant="secondary" size="sm" className="bg-[#7CFC00]/80 dark:bg-[#32CD32]/80 hover:bg-[#7CFC00] dark:hover:bg-[#32CD32] text-white"><CheckCheck className="mr-2"/>Aprovar</Button>
                                <Button variant="destructive" size="sm">Reprovar</Button>
                            </div>
                        </CardContent>
                    </Card>
                ))}
            </CardContent>
        </Card>
    </section>
);

const AuditoriaLogs = () => (
    <section id="auditoria" className="scroll-mt-20">
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><History/> Auditoria e Logs</CardTitle>
                <CardDescription>Ações recentes realizadas no sistema.</CardDescription>
            </CardHeader>
            <CardContent>
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>Data/Hora</TableHead>
                            <TableHead>Usuário</TableHead>
                            <TableHead>Ação</TableHead>
                            <TableHead>Detalhes</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {auditLogs.map(log => (
                            <TableRow key={log.time}>
                                <TableCell className="font-mono text-xs">{log.time}</TableCell>
                                <TableCell>{log.user}</TableCell>
                                <TableCell><Badge variant="outline">{log.action}</Badge></TableCell>
                                <TableCell>{log.details}</TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </CardContent>
        </Card>
    </section>
);

const ManutencaoIntegridade = () => (
    <section id="manutencao" className="scroll-mt-20">
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><Server/> Manutenção e Integridade do Sistema</CardTitle>
                <CardDescription>Informações sobre backups, integridade e rotinas operacionais.</CardDescription>
            </CardHeader>
            <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <Card>
                    <CardHeader className="pb-2">
                        <CardDescription>Último backup</CardDescription>
                        <CardTitle className="text-2xl">Hoje às 02:00</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <p className="text-xs text-muted-foreground">Próximo backup automático em 24h.</p>
                    </CardContent>
                </Card>
                 <Card>
                    <CardHeader className="pb-2">
                        <CardDescription>Integridade dos Dados</CardDescription>
                        <CardTitle className="text-2xl text-[#7CFC00] dark:text-[#32CD32]">Verificada</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <p className="text-xs text-muted-foreground">Nenhuma inconsistência encontrada.</p>
                    </CardContent>
                </Card>
                <div className="md:col-span-2 space-y-4">
                    <Button className="w-full">Executar Backup Manual</Button>
                    <Alert>
                        <FileText className="h-4 w-4" />
                        <AlertTitle>Ciclo de Manutenção</AlertTitle>
                        <AlertDescription>
                        A manutenção periódica ocorre todos os domingos às 03:00 para otimização do banco de dados e aplicação de patches de segurança.
                        </AlertDescription>
                    </Alert>
                </div>
            </CardContent>
        </Card>
    </section>
);


const SegurancaConformidade = () => (
    <section id="seguranca" className="scroll-mt-20">
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><Shield/> Segurança e Conformidade (LGPD)</CardTitle>
                <CardDescription>Gerencie as configurações de privacidade e segurança dos dados.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
                <div className="space-y-4 p-4 border rounded-lg">
                    <div className="flex items-center justify-between">
                        <Label htmlFor="encryption-switch" className="flex flex-col gap-1">
                            <span>Criptografia de Dados Ativa</span>
                            <span className="text-xs text-muted-foreground">Protege todos os dados de pacientes em repouso.</span>
                        </Label>
                        <Switch id="encryption-switch" defaultChecked disabled />
                    </div>
                    <div className="flex items-center justify-between">
                        <Label htmlFor="consent-switch" className="flex flex-col gap-1">
                             <span>Consentimento Obrigatório para Pacientes</span>
                             <span className="text-xs text-muted-foreground">Exige aceite dos termos para novos cadastros.</span>
                        </Label>
                        <Switch id="consent-switch" defaultChecked />
                    </div>
                    <div className="flex items-center justify-between">
                        <Label htmlFor="anonymization-switch" className="flex flex-col gap-1">
                            <span>Anonimização de Logs Antigos</span>
                            <span className="text-xs text-muted-foreground">Anonimiza dados de logs com mais de 2 anos.</span>
                        </Label>
                        <Switch id="anonymization-switch" />
                    </div>
                </div>
                <Alert variant="default" className="bg-primary/10 border-primary/30 text-primary">
                    <KeyRound className="h-4 w-4 !text-primary" />
                    <AlertTitle>Boas Práticas de Proteção de Dados</AlertTitle>
                    <AlertDescription className="text-primary/90">
                       Lembre-se de revisar as permissões de usuário regularmente e seguir o princípio do menor privilégio. A segurança é uma responsabilidade compartilhada.
                    </AlertDescription>
                </Alert>
            </CardContent>
        </Card>
    </section>
);

const ConfiguracoesGerais = () => (
    <section id="configuracoes" className="scroll-mt-20">
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><Settings2 /> Configurações Gerais do Sistema</CardTitle>
                <CardDescription>Defina parâmetros administrativos de alto nível para a clínica.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div className="space-y-4 p-4 border rounded-lg">
                        <h3 className="font-semibold">Identificação da Clínica</h3>
                        <div className="space-y-2">
                            <Label htmlFor="clinic-name">Nome da Clínica</Label>
                            <Input id="clinic-name" defaultValue="DentiBot Clínica Odontológica" />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="clinic-address">Endereço</Label>
                            <Input id="clinic-address" defaultValue="Rua Fictícia, 123 - Centro" />
                        </div>
                         <div className="space-y-2">
                            <Label htmlFor="clinic-phone">Telefone</Label>
                            <Input id="clinic-phone" defaultValue="(11) 5555-1234" />
                        </div>
                        <div className="space-y-2">
                            <Label>Logotipo</Label>
                            <div className="flex items-center gap-4">
                                <Button variant="outline" size="sm">Alterar Logo</Button>
                            </div>
                        </div>
                    </div>
                     <div className="space-y-4 p-4 border rounded-lg">
                        <h3 className="font-semibold">Políticas e Operação</h3>
                        <div className="grid grid-cols-2 gap-4">
                             <div className="space-y-2">
                                <Label htmlFor="clinic-open-time">Abertura</Label>
                                <Input id="clinic-open-time" type="time" defaultValue="08:00" />
                            </div>
                             <div className="space-y-2">
                                <Label htmlFor="clinic-close-time">Fechamento</Label>
                                <Input id="clinic-close-time" type="time" defaultValue="18:00" />
                            </div>
                        </div>
                        <div className="space-y-2">
                             <Label htmlFor="cancel-policy">Política de Cancelamento (horas)</Label>
                             <Input id="cancel-policy" type="number" defaultValue="24" />
                        </div>
                         <div className="flex items-center justify-between">
                            <Label htmlFor="no-show-fee" className="flex flex-col gap-1">
                                <span>Cobrar taxa por falta</span>
                            </Label>
                            <Switch id="no-show-fee" />
                        </div>
                    </div>
                </div>
                 <div className="flex justify-end">
                    <Button>Salvar Configurações Gerais</Button>
                </div>
            </CardContent>
        </Card>
    </section>
);


export default function CoordenadorPage() {
    return (
        <div className="space-y-8">
            <DashboardAdministrativo />
            <GestaoUsuarios />
            <HomologacaoRotinas />
            <AuditoriaLogs />
            <ManutencaoIntegridade />
            <SegurancaConformidade />
            <ConfiguracoesGerais />
        </div>
    );
}
