'use client';

import { Suspense, useEffect } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import type { UserRole } from '@/lib/types';
import { Skeleton } from '@/components/ui/skeleton';

function DashboardRedirect() {
    const searchParams = useSearchParams();
    const router = useRouter();
    const role: UserRole = (searchParams.get('role') as UserRole) || 'paciente';

    useEffect(() => {
        let path = '/dashboard/paciente';
        switch (role) {
            case 'dentist':
                path = '/dashboard/dentista';
                break;
            case 'receptionist':
                path = '/dashboard/recepcionista';
                break;
            case 'almoxarifado':
                path = '/dashboard/almoxarifado';
                break;
            case 'coordenador':
                path = '/dashboard/coordenador';
                break;
            case 'financeiro':
                path = '/dashboard/financeiro';
                break;
            case 'patient':
                path = '/dashboard/paciente';
                break;
        }
        // Preserve other query params if necessary
        const newSearchParams = new URLSearchParams(searchParams);
        // We can remove role as it's now part of the URL path
        // newSearchParams.delete('role'); 
        router.replace(`${path}?${newSearchParams.toString()}`);

    }, [role, searchParams, router]);

    return <DashboardSkeleton />;
}


function DashboardSkeleton() {
    return (
        <div>
            <Skeleton className="h-12 w-1/2 mb-6" />
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                <Skeleton className="h-32" />
                <Skeleton className="h-32" />
                <Skeleton className="h-32" />
                <Skeleton className="h-32" />
            </div>
            <Skeleton className="h-96 mt-6" />
        </div>
    )
}

export default function DashboardPage() {
    return (
        <Suspense fallback={<DashboardSkeleton />}>
            <DashboardRedirect />
        </Suspense>
    );
}
