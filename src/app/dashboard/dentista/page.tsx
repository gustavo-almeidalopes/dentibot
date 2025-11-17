// src/app/dashboard/dentista/page.tsx
'use client';

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Checkbox } from "@/components/ui/checkbox";
import { FileUp, Send, Calendar, FilePlus, History, Phone, BookUser, ClipboardList, ShieldCheck, FileClock } from "lucide-react";

// --- Sub-components for each section ---

const AgendaConsultas = () => (
    <section id="agenda" className="scroll-mt-20">
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><Calendar /> Agenda de Consultas</CardTitle>
                <CardDescription>Visualize e gerencie seus próximos agendamentos.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
                <div className="p-4 bg-muted/50 rounded-lg text-center">
                    <p className="text-muted-foreground">Aqui será exibida a agenda diária/semanal.</p>
                </div>
            </CardContent>
        </Card>
    </section>
);

const PrescricoesDigitais = () => (
    <section id="prescricoes" className="scroll-mt-20">
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><FilePlus /> Prescrições Digitais</CardTitle>
                <CardDescription>Crie e envie prescrições com segurança.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
                <div className="space-y-2">
                    <Label htmlFor="prescription-patient">Paciente</Label>
                    <Input id="prescription-patient" placeholder="Selecionar Paciente" />
                </div>
                <div className="space-y-2">
                    <Label htmlFor="prescription-content">Conteúdo da Prescrição</Label>
                    <Textarea id="prescription-content" placeholder="Digite a prescrição ou instruções clínicas aqui..." rows={5} />
                </div>
                <Button>
                    <Send className="mr-2" />
                    Gerar e Enviar Prescrição
                </Button>
            </CardContent>
        </Card>
    </section>
);

const HistoricoClinico = () => (
    <section id="historico" className="scroll-mt-20">
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><History /> Histórico Clínico Detalhado</CardTitle>
                <CardDescription>Acesse o histórico completo dos seus pacientes.</CardDescription>
            </CardHeader>
            <CardContent>
                 <div className="space-y-2 mb-4">
                    <Label htmlFor="history-patient">Buscar Paciente</Label>
                    <Input id="history-patient" placeholder="Digite o nome do paciente para ver o histórico" />
                </div>
                <Textarea placeholder="O histórico clínico, incluindo evolução e procedimentos, aparecerá aqui..." rows={8} readOnly className="bg-muted/50" />
            </CardContent>
        </Card>
    </section>
);

const ComunicacaoPacientes = () => (
    <section id="comunicacao" className="scroll-mt-20">
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><Phone /> Comunicação com Pacientes</CardTitle>
                <CardDescription>Envie mensagens e lembretes importantes.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
                <div className="space-y-2">
                    <Label htmlFor="message-recipient">Paciente Destinatário</Label>
                    <Input id="message-recipient" placeholder="E-mail ou ID do Paciente" />
                </div>
                <div className="space-y-2">
                    <Label htmlFor="message-content">Mensagem</Label>
                    <Textarea id="message-content" placeholder="Escreva sua mensagem, lembrete ou instrução pós-atendimento..." />
                </div>
                <Button className="w-full md:w-auto">Enviar Mensagem</Button>
            </CardContent>
        </Card>
    </section>
);

const CadastroPacientes = () => (
    <section id="cadastro" className="scroll-mt-20">
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><BookUser /> Cadastro e Gerenciamento de Pacientes</CardTitle>
                <CardDescription>Adicione ou atualize informações dos pacientes.</CardDescription>
            </CardHeader>
            <CardContent className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="space-y-2">
                    <Label htmlFor="new-patient-name">Nome Completo</Label>
                    <Input id="new-patient-name" placeholder="Nome do Paciente" />
                </div>
                <div className="space-y-2">
                    <Label htmlFor="new-patient-email">E-mail</Label>
                    <Input id="new-patient-email" type="email" placeholder="email@exemplo.com" />
                </div>
                <div className="space-y-2">
                    <Label htmlFor="new-patient-phone">Telefone</Label>
                    <Input id="new-patient-phone" type="tel" placeholder="(00) 99999-9999" />
                </div>
                <div className="md:col-span-3">
                    <Button>Salvar Paciente</Button>
                </div>
            </CardContent>
        </Card>
    </section>
);

const ProntuarioEletronico = () => (
    <section id="prontuario" className="scroll-mt-20">
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><ClipboardList /> Prontuário Eletrônico</CardTitle>
                <CardDescription>Registre os detalhes da consulta atual.</CardDescription>
            </CardHeader>
            <CardContent>
                <Textarea placeholder="Registrar procedimentos realizados, evolução clínica, observações sobre dor, exames solicitados, medicações e outras anotações da consulta..." rows={10} />
            </CardContent>
        </Card>
    </section>
);

const ChecklistBiosseguranca = () => (
    <section id="biosseguranca" className="scroll-mt-20">
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><ShieldCheck /> Checklist de Biossegurança</CardTitle>
                <CardDescription>Confirme os protocolos antes de cada atendimento.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
                <div className="flex items-center space-x-2">
                    <Checkbox id="check-instruments" />
                    <Label htmlFor="check-instruments">Instrumentos esterilizados e embalados</Label>
                </div>
                <div className="flex items-center space-x-2">
                    <Checkbox id="check-epi" />
                    <Label htmlFor="check-epi">Uso completo de EPI (gorro, óculos, máscara, luvas)</Label>
                </div>
                <div className="flex items-center space-x-2">
                    <Checkbox id="check-office" />
                    <Label htmlFor="check-office">Preparo e desinfecção do consultório</Label>
                </div>
            </CardContent>
        </Card>
    </section>
);

const UploadDocumentos = () => (
    <section id="documentos" className="scroll-mt-20">
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><FileUp /> Upload de Documentos</CardTitle>
                 <CardDescription>Anexe exames e imagens ao prontuário do paciente.</CardDescription>
            </CardHeader>
            <CardContent>
                <Label htmlFor="file-upload" className="w-full cursor-pointer">
                    <div className="flex flex-col items-center justify-center w-full p-6 border-2 border-dashed rounded-lg hover:bg-muted/50">
                        <FileUp className="mx-auto h-10 w-10 text-muted-foreground" />
                        <p className="mt-2 text-sm text-muted-foreground">Clique para anexar exames, radiografias, etc.</p>
                        <p className="text-xs text-muted-foreground">PDF, JPG, PNG</p>
                    </div>
                </Label>
                <Input id="file-upload" type="file" multiple className="hidden" />
            </CardContent>
        </Card>
    </section>
);

const RegistroClinicoRapido = () => (
    <section id="registro-rapido" className="scroll-mt-20">
        <Card>
            <CardHeader>
                <CardTitle className="flex items-center gap-2"><FileClock /> Registro Clínico Rápido</CardTitle>
                <CardDescription>Anote informações rápidas durante o atendimento.</CardDescription>
            </CardHeader>
            <CardContent>
                <Textarea placeholder="Notas curtas sobre o atendimento..." rows={3}/>
            </CardContent>
        </Card>
    </section>
);

export default function DentistPage() {
    return (
        <div className="space-y-8">
            <header id="painel-principal" className="scroll-mt-20">
                <h2 className="text-2xl font-bold tracking-tight text-foreground">Painel do Dentista</h2>
                <p className="text-muted-foreground">Suas ferramentas diárias para um atendimento de excelência.</p>
            </header>
            <AgendaConsultas />
            <ProntuarioEletronico />
            <PrescricoesDigitais />
            <HistoricoClinico />
            <CadastroPacientes />
            <ComunicacaoPacientes />
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                <ChecklistBiosseguranca />
                <UploadDocumentos />
            </div>
            <RegistroClinicoRapido />
        </div>
    );
}
