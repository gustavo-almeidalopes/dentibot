import type { LucideIcon } from "lucide-react";

export type UserRole = 'dentist' | 'receptionist' | 'patient' | 'almoxarifado' | 'coordenador' | 'financeiro';

export const ROLES: { id: UserRole; name: string }[] = [
    { id: 'dentist', name: 'Dentista' },
    { id: 'receptionist', name: 'Recepcionista' },
    { id: 'almoxarifado', name: 'Almoxarifado' },
    { id: 'coordenador', name: 'Coordenador' },
    { id: 'financeiro', name: 'Financeiro' },
    { id: 'patient', name: 'Paciente' },
];

export type StatCard = {
  title: string;
  value: string;
  icon: LucideIcon;
  change?: string;
  iconColor?: string;
};
