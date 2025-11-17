// src/app/dashboard/paciente/page.tsx
'use client';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Download, Edit, MapPin, CheckCircle, User, History, ClipboardList, Calendar, Bell, Wallet, MessageSquare, Send, Paperclip, CalendarPlus } from "lucide-react";
import { Switch } from "@/components/ui/switch";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { cn } from "@/lib/utils";

const historyData = [
    { date: "2024-05-15", procedure: "Limpeza e Aplicação de Flúor", professional: "Dr. Ana Costa", status: "Realizada" },
    { date: "2024-01-20", procedure: "Restauração (Dente 26)", professional: "Dr. Ana Costa", status: "Realizada" },
    { date: "2023-11-10", procedure: "Avaliação Inicial", professional: "Dr. João Silva", status: "Realizada" },
    { date: "2023-09-02", procedure: "Consulta de Acompanhamento", professional: "Dr. João Silva", status: "Cancelada" },
];

const documents = [
    { name: "Orçamento_Tratamento_XYZ.pdf", date: "2024-05-10" },
    { name: "Raio_X_Panoramico_2023.jpg", date: "2023-11-10" },
];

const financialData = [
    { date: "2024-05-15", procedure: "Limpeza", value: "R$ 250,00", status: "Pago" },
    { date: "2024-01-20", procedure: "Restauração", value: "R$ 400,00", status: "Pago" },
    { date: "2024-07-30", procedure: "Clareamento (1ª sessão)", value: "R$ 600,00", status: "Pendente" },
];

// --- Sub-components for each section ---
const PageHeader = () => (
     <header id="painel-principal" className="flex items-center gap-4 scroll-mt-20">
        <div>
            <h1 className="text-2xl font-bold">Olá, Paciente!</h1>
            <p className="text-muted-foreground">Bem-vindo(a) ao seu portal.</p>
        </div>
    </header>
);

const ProximaConsulta = () => (
    <section id="proxima-consulta" className="scroll-mt-20">
        <Card className="bg-primary/10 border-primary/40">
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><Calendar /> Sua Próxima Consulta</CardTitle>
            </CardHeader>
            <CardContent className="flex flex-col md:flex-row items-center justify-between gap-6">
                <div className="flex items-center gap-4">
                    <div className="text-center">
                        <p className="text-sm text-muted-foreground">AGO</p>
                        <p className="text-5xl font-bold">15</p>
                    </div>
                    <div>
                        <p className="font-semibold text-lg">Reavaliação e Limpeza</p>
                        <p className="text-muted-foreground">com Dr(a). Ana Costa</p>
                        <p className="text-lg font-bold mt-1">14:30</p>
                    </div>
                </div>
                <div className="flex flex-col items-center gap-2">
                    <Badge className="bg-[#7CFC00]/20 dark:bg-[#32CD32]/20 text-[#7CFC00] dark:text-[#32CD32] text-base px-4 py-1 font-semibold">
                        <CheckCircle className="mr-2 h-4 w-4" />
                        Confirmada
                    </Badge>
                     <div className="flex gap-2 mt-2">
                         <Button variant="outline" size="sm">
                            <CalendarPlus className="mr-2 h-4 w-4" />
                            Adicionar
                        </Button>
                        <Button variant="link" className="text-primary">
                            <MapPin className="mr-2 h-4 w-4" />
                            Ver no Mapa
                        </Button>
                    </div>
                </div>
            </CardContent>
        </Card>
    </section>
);

const HistoricoConsultas = () => (
    <section id="historico" className="scroll-mt-20">
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><History /> Histórico de Consultas</CardTitle>
                 <CardDescription>Todas as suas consultas realizadas anteriormente.</CardDescription>
            </CardHeader>
            <CardContent>
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>Data</TableHead>
                            <TableHead>Procedimento</TableHead>
                            <TableHead>Profissional</TableHead>
                            <TableHead className="text-right">Status</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {historyData.map(item => (
                            <TableRow key={item.date}>
                                <TableCell>{item.date}</TableCell>
                                <TableCell>{item.procedure}</TableCell>
                                <TableCell>{item.professional}</TableCell>
                                <TableCell className="text-right">
                                    <Badge variant={item.status === "Realizada" ? "secondary" : "outline"} className={cn('font-semibold', item.status === 'Realizada' ? 'bg-[#7CFC00]/20 dark:bg-[#32CD32]/20 text-[#7CFC00] dark:text-[#32CD32]' : 'bg-muted text-muted-foreground')}>
                                        {item.status}
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

const Documentos = () => (
    <section id="documentos" className="scroll-mt-20">
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><ClipboardList/> Documentos</CardTitle>
                <CardDescription>Acesse seus exames, orçamentos e outros documentos.</CardDescription>
            </CardHeader>
            <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {documents.map(doc => (
                <div key={doc.name} className="flex items-center justify-between p-4 border rounded-lg">
                    <div>
                        <p className="font-semibold">{doc.name}</p>
                        <p className="text-sm text-muted-foreground">Emitido em: {doc.date}</p>
                    </div>
                    <Button variant="ghost" size="icon">
                        <Download className="h-5 w-5" />
                    </Button>
                </div>
            ))}
            </CardContent>
        </Card>
    </section>
);

const MeuPerfil = () => (
    <section id="perfil" className="scroll-mt-20">
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><User /> Seu Perfil</CardTitle>
                 <CardDescription>Suas informações cadastrais.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
                <form className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div className="space-y-2">
                        <Label htmlFor="patient-name">Nome Completo</Label>
                        <Input id="patient-name" defaultValue="Nome Completo do Paciente" />
                    </div>
                     <div className="space-y-2">
                        <Label htmlFor="patient-dob">Data de Nascimento</Label>
                        <Input id="patient-dob" type="date" defaultValue="1990-01-01" />
                    </div>
                     <div className="space-y-2">
                        <Label htmlFor="patient-email">E-mail</Label>
                        <Input id="patient-email" type="email" defaultValue="paciente@email.com" />
                    </div>
                     <div className="space-y-2">
                        <Label htmlFor="patient-phone">Telefone</Label>
                        <Input id="patient-phone" type="tel" defaultValue="(11) 98765-4321" />
                    </div>
                     <div className="space-y-2 md:col-span-2">
                        <Label htmlFor="patient-address">Endereço</Label>
                        <Input id="patient-address" defaultValue="Rua das Flores, 123, Bairro Jardim, Cidade-UF" />
                    </div>
                     <div className="space-y-2">
                        <Label htmlFor="patient-insurance">Convênio</Label>
                        <Input id="patient-insurance" defaultValue="Amil Dental" />
                    </div>
                     <div className="space-y-2">
                        <Label htmlFor="patient-insurance-id">Nº da Carteirinha</Label>
                        <Input id="patient-insurance-id" defaultValue="123.456.789-00" />
                    </div>
                </form>
                <Button className="w-full md:w-auto">
                    <Edit className="mr-2" />
                    Salvar Alterações
                </Button>
            </CardContent>
        </Card>
    </section>
);


const PreferenciasNotificacao = () => (
    <section id="notificacoes" className="scroll-mt-20">
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><Bell /> Preferências de Notificação</CardTitle>
                <CardDescription>Defina como deseja receber lembretes e confirmações.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
                <p className="text-sm text-muted-foreground">Essas configurações determinam como a clínica enviará lembretes e outras comunicações importantes.</p>
                <div className="flex items-center justify-between p-4 border rounded-lg">
                    <Label htmlFor="whatsapp-switch" className="font-medium">WhatsApp</Label>
                    <Switch id="whatsapp-switch" defaultChecked />
                </div>
                 <div className="flex items-center justify-between p-4 border rounded-lg">
                    <Label htmlFor="sms-switch" className="font-medium">SMS</Label>
                    <Switch id="sms-switch" />
                </div>
                 <div className="flex items-center justify-between p-4 border rounded-lg">
                    <Label htmlFor="email-switch" className="font-medium">E-mail</Label>
                    <Switch id="email-switch" defaultChecked />
                </div>
            </CardContent>
        </Card>
    </section>
);


const FinanceiroPaciente = () => {
    const getStatusVariant = (status: string) => {
        switch (status) {
            case 'Pago': return 'bg-[#7CFC00]/20 dark:bg-[#32CD32]/20 text-[#7CFC00] dark:text-[#32CD32]';
            case 'Pendente': return 'bg-[#FFDE21]/20 dark:bg-[#FFD700]/20 text-[#FFDE21] dark:text-[#FFD700]';
            default: return 'secondary';
        }
    };
    return (
        <section id="financeiro" className="scroll-mt-20">
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2"><Wallet /> Financeiro</CardTitle>
                    <CardDescription>Seu histórico de pagamentos e pendências.</CardDescription>
                </CardHeader>
                <CardContent>
                     <div className="grid grid-cols-2 gap-4 mb-6 text-center">
                        <div className="p-4 bg-muted/50 rounded-lg">
                            <p className="text-sm text-muted-foreground">Total Pago</p>
                            <p className="text-2xl font-bold text-[#7CFC00] dark:text-[#32CD32]">R$ 650,00</p>
                        </div>
                        <div className="p-4 bg-muted/50 rounded-lg">
                            <p className="text-sm text-muted-foreground">Total Pendente</p>
                            <p className="text-2xl font-bold text-[#FFDE21] dark:text-[#FFD700]">R$ 600,00</p>
                        </div>
                    </div>
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead>Data</TableHead>
                                <TableHead>Procedimento</TableHead>
                                <TableHead>Valor</TableHead>
                                <TableHead>Status</TableHead>
                                <TableHead className="text-right">Comprovante</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {financialData.map((item, index) => (
                                <TableRow key={index}>
                                    <TableCell>{item.date}</TableCell>
                                    <TableCell>{item.procedure}</TableCell>
                                    <TableCell>{item.value}</TableCell>
                                    <TableCell>
                                        <Badge variant="secondary" className={cn('font-semibold', getStatusVariant(item.status))}>{item.status}</Badge>
                                    </TableCell>
                                    <TableCell className="text-right">
                                        <Button variant="outline" size="sm" disabled={item.status === 'Pendente'}>
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


const MensagensContato = () => (
    <section id="mensagens" className="scroll-mt-20">
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><MessageSquare /> Mensagens e Contato</CardTitle>
                <CardDescription>Envie uma mensagem diretamente para a recepção.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
                <Textarea placeholder="Escreva sua mensagem aqui..." rows={5} />
                <div className="flex justify-between items-center">
                    <Button variant="outline">
                        <Paperclip className="mr-2" />
                        Anexar Arquivo
                    </Button>
                    <Button>
                        <Send className="mr-2" />
                        Enviar Mensagem
                    </Button>
                </div>
            </CardContent>
        </Card>
    </section>
);


export default function PatientPage() {
    return (
        <div className="space-y-8">
            <PageHeader />
            <ProximaConsulta />
            <HistoricoConsultas />
            <Documentos />
            <MeuPerfil />
            <PreferenciasNotificacao />
            <FinanceiroPaciente />
            <MensagensContato />
        </div>
    );
}
