'use client';
import { useSearchParams } from "next/navigation";
import { ROLES, type UserRole } from "@/lib/types";
import { useState, useEffect } from "react";

export function WelcomeHeader() {
    const searchParams = useSearchParams();
    const role = (searchParams.get("role") || 'paciente') as UserRole;
    const roleName = ROLES.find(r => r.id === role)?.name || "Usuário";
    const [greeting, setGreeting] = useState<string | null>(null);

    useEffect(() => {
        const getGreeting = () => {
            const hour = new Date().getHours();
            if (hour < 12) return "Bom dia";
            if (hour < 18) return "Boa tarde";
            return "Boa noite";
        }
        setGreeting(`${getGreeting()}, ${roleName}!`);
    }, [roleName]);

    // Render only on the client-side after the component has mounted
    if (!greeting) {
        return null;
    }

    return (
        <div className="mb-6">
            <h1 className="font-headline text-3xl font-bold tracking-tight">
                {greeting}
            </h1>
            <p className="text-muted-foreground">Aqui está um resumo do que está acontecendo hoje.</p>
        </div>
    );
}
