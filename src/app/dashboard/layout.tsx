import { SidebarProvider, SidebarInset } from "@/components/ui/sidebar";
import { AppSidebar } from "@/components/dashboard/AppSidebar";
import { Suspense } from "react";
import { WelcomeHeader } from "@/components/dashboard/WelcomeHeader";
import { ThemeToggle } from "@/components/theme-toggle";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <SidebarProvider>
      <Suspense>
        <AppSidebar />
      </Suspense>
      <SidebarInset>
        <main className="p-4 sm:p-6 lg:p-8">
            <div className="flex justify-between items-start mb-8">
              <WelcomeHeader />
              <ThemeToggle />
            </div>
            {children}
        </main>
      </SidebarInset>
    </SidebarProvider>
  )
}
