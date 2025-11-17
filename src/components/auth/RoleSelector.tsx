'use client';

import { useState } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { ROLES, type UserRole } from '@/lib/types';
import { LogIn } from 'lucide-react';

export function RoleSelector() {
  const [selectedRole, setSelectedRole] = useState<UserRole>('dentist');

  return (
    <Card className="w-full max-w-sm border-2 border-primary/20 shadow-lg bg-card/80 backdrop-blur-md">
      <CardHeader className="text-center">
        <CardTitle className="font-headline text-2xl">Bem-vindo(a) de volta!</CardTitle>
        <CardDescription>Selecione seu perfil para acessar o painel</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <Select value={selectedRole} onValueChange={(value: UserRole) => setSelectedRole(value)}>
          <SelectTrigger className="w-full h-12 text-base">
            <SelectValue placeholder="Selecione um perfil..." />
          </SelectTrigger>
          <SelectContent>
            {ROLES.map((role) => (
              <SelectItem key={role.id} value={role.id} className="text-base">
                {role.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Button asChild size="lg" className="w-full h-12 text-base">
          <Link href={`/dashboard?role=${selectedRole}`}>
            <LogIn className="mr-2 h-5 w-5" />
            Entrar no Painel
          </Link>
        </Button>
      </CardContent>
    </Card>
  );
}
