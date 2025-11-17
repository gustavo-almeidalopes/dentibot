'use client'

import { Sidebar, SidebarHeader, SidebarContent, SidebarFooter, SidebarMenu, SidebarMenuItem, SidebarMenuButton, SidebarTrigger, useSidebar } from "@/components/ui/sidebar"
import {
  Users,
  ClipboardList,
  DollarSign,
  Home,
  LogOut,
  Settings,
  FilePlus,
  BookUser,
  ShieldCheck,
  FileClock,
  FlaskConical,
  User,
  Calendar,
  CheckCheck,
  History,
  Phone,
  Wallet,
  Warehouse,
  FilePieChart,
  CalendarPlus,
  FileUp,
  Package,
  ArrowRightLeft,
  Route,
  AlertTriangleIcon,
  Server,
  FileCog,
  Shield,
  Settings2,
  Landmark,
  FileText,
  MessageSquare,
  Bell,
} from "lucide-react"
import type { UserRole } from "@/lib/types";
import { ROLES } from "@/lib/types";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { ToothIcon } from "../ui/icons";

const SidebarUser = () => {
    const searchParams = useSearchParams();
    const role = (searchParams.get("role") || 'paciente') as UserRole;
    const roleName = ROLES.find(r => r.id === role)?.name || 'Usuário';

    return (
        <div className="flex items-center gap-3 rounded-md bg-sidebar-accent p-2">
            <div className="overflow-hidden">
                <p className="truncate font-medium text-sidebar-accent-foreground">{roleName}</p>
                <p className="text-xs text-muted-foreground truncate">{role}@dentibot.com</p>
            </div>
        </div>
    );
};


export function AppSidebar() {
    const searchParams = useSearchParams();
    const role = (searchParams.get("role") || 'paciente') as UserRole;
    const [activeItem, setActiveItem] = useState('painel-principal');

    useEffect(() => {
        const handleScroll = () => {
            const sections = document.querySelectorAll('section[id], header[id]');
            let currentSection = 'painel-principal';

            sections.forEach(section => {
                const sectionTop = section.getBoundingClientRect().top;
                if (sectionTop <= 100) { // 100px offset from top
                    currentSection = section.id;
                }
            });
            setActiveItem(currentSection);
        };

        window.addEventListener('scroll', handleScroll);
        handleScroll(); // Set initial active item

        return () => {
            window.removeEventListener('scroll', handleScroll);
        };
    }, []);

    const roleConfig = {
        dentist: {
            routes: [
                { href: 'agenda', icon: Calendar, label: 'Agenda' },
                { href: 'prescricoes', icon: FilePlus, label: 'Prescrições' },
                { href: 'historico', icon: History, label: 'Histórico' },
                { href: 'comunicacao', icon: Phone, label: 'Comunicação' },
                { href: 'cadastro', icon: BookUser, label: 'Pacientes' },
                { href: 'prontuario', icon: ClipboardList, label: 'Prontuário' },
                { href: 'biosseguranca', icon: ShieldCheck, label: 'Biossegurança' },
                { href: 'documentos', icon: FileUp, label: 'Documentos' },
                { href: 'registro-rapido', icon: FileClock, label: 'Registro Rápido' }
            ],
            settingsHref: null // No settings section for dentist
        },
        receptionist: {
            routes: [
                { href: 'agendamento', icon: CalendarPlus, label: 'Agendamento' },
                { href: 'confirmacoes', icon: CheckCheck, label: 'Confirmações' },
                { href: 'reagendamento', icon: History, label: 'Reagendamento' },
                { href: 'observacoes', icon: FileClock, label: 'Observações' },
                { href: 'convenios', icon: Wallet, label: 'Convênios' }
            ],
            settingsHref: null // No settings section for receptionist
        },
        almoxarifado: {
            routes: [
                { href: 'inventario', icon: Warehouse, label: 'Inventário Geral' },
                { href: 'movimentacao', icon: ArrowRightLeft, label: 'Entrada e Saída' },
                { href: 'kits', icon: Package, label: 'Controle de Kits' },
                { href: 'esterilizacao', icon: FlaskConical, label: 'Esterilização' },
                { href: 'rastreabilidade', icon: Route, label: 'Rastreabilidade' },
                { href: 'alertas-consumo', icon: AlertTriangleIcon, label: 'Alertas' }
            ],
             settingsHref: null // No settings section for almoxarifado
        },
        coordenador: {
             routes: [
                { href: 'gestao-usuarios', icon: Users, label: 'Usuários' },
                { href: 'homologacao-rotinas', icon: FileCog, label: 'Homologação' },
                { href: 'auditoria', icon: History, label: 'Auditoria e Logs' },
                { href: 'manutencao', icon: Server, label: 'Manutenção' },
                { href: 'seguranca', icon: Shield, label: 'Segurança' },
                { href: 'configuracoes', icon: Settings2, label: 'Configurações' },
            ],
            settingsHref: 'configuracoes'
        },
        financeiro: {
            routes: [
                { href: 'movimentacoes', icon: DollarSign, label: 'Pagamentos' },
                { href: 'faturamento-convenios', icon: Landmark, label: 'Faturamento Convênios' },
                { href: 'historico-financeiro', icon: History, label: 'Histórico' },
                { href: 'relatorios', icon: FilePieChart, label: 'Relatórios' },
                { href: 'configuracoes-financeiras', icon: Settings, label: 'Configurações' },
                { href: 'comprovantes', icon: FileText, label: 'Comprovantes' },
            ],
            settingsHref: 'configuracoes-financeiras'
        },
        patient: {
             routes: [
                { href: 'proxima-consulta', icon: Calendar, label: 'Próxima Consulta' },
                { href: 'historico', icon: History, label: 'Histórico' },
                { href: 'documentos', icon: ClipboardList, label: 'Documentos' },
                { href: 'perfil', icon: User, label: 'Meu Perfil' },
                { href: 'notificacoes', icon: Bell, label: 'Notificações' },
                { href: 'financeiro', icon: Wallet, label: 'Financeiro' },
                { href: 'mensagens', icon: MessageSquare, label: 'Mensagens' },
            ],
            settingsHref: 'notificacoes' // Patient settings are notifications
        }
    };

    const currentRoleConfig = roleConfig[role] || roleConfig.patient;
    const menuItems = currentRoleConfig.routes;

    return (
        <Sidebar>
            <SidebarHeader>
                <div className="flex items-center gap-2">
                    <ButtonOrDiv>
                        <ToothIcon className="size-6 text-primary" />
                    </ButtonOrDiv>
                    <span className="font-headline text-lg font-semibold">DentiBot</span>
                </div>
            </SidebarHeader>
            <SidebarContent>
                <SidebarMenu>
                     <SidebarMenuItem>
                        <SidebarMenuButton asChild isActive={activeItem === 'painel-principal'}>
                            <Link href="#painel-principal">
                                <Home />
                                Painel Principal
                            </Link>
                        </SidebarMenuButton>
                    </SidebarMenuItem>
                    {menuItems.map(item => (
                        <SidebarMenuItem key={item.label}>
                            <SidebarMenuButton asChild isActive={activeItem === item.href}>
                                <Link href={`#${item.href}`}>
                                    <item.icon />
                                    {item.label}
                                </Link>
                            </SidebarMenuButton>
                        </SidebarMenuItem>
                    ))}
                </SidebarMenu>
            </SidebarContent>
            <SidebarFooter>
                 <Suspense>
                    <SidebarUser />
                </Suspense>
                <SidebarMenu>
                    {currentRoleConfig.settingsHref && (
                        <SidebarMenuItem>
                            <SidebarMenuButton asChild isActive={activeItem === currentRoleConfig.settingsHref}>
                                <Link href={`#${currentRoleConfig.settingsHref}`}>
                                    <Settings />
                                    Configurações
                                </Link>
                            </SidebarMenuButton>
                        </SidebarMenuItem>
                    )}
                    <SidebarMenuItem>
                        <SidebarMenuButton asChild variant="outline">
                            <Link href="/">
                                <LogOut />
                                Sair
                            </Link>
                        </SidebarMenuButton>
                    </SidebarMenuItem>
                </SidebarMenu>
            </SidebarFooter>
        </Sidebar>
    );
}

function ButtonOrDiv({ children }: { children: React.ReactNode }) {
    const { isMobile, toggleSidebar } = useSidebar()
    if (isMobile) {
        return (
            <button onClick={toggleSidebar} className="md:hidden">
                {children}
            </button>
        )
    }
    return (
        <SidebarTrigger className="md:hidden">
            {children}
        </SidebarTrigger>
    )
}
