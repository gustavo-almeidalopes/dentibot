import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import type { StatCard as StatCardType } from "@/lib/types";


export function StatCard({ title, value, icon: Icon, change, iconColor }: StatCardType) {
  const isPositive = change && change.startsWith('+');
  const isNegative = change && change.startsWith('-');
  
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">{title}</CardTitle>
        <Icon className={cn("h-5 w-5 text-muted-foreground", iconColor)} />
      </CardHeader>
      <CardContent>
        <div className="text-3xl font-bold">{value}</div>
        {change && (
            <p className={cn("text-xs text-muted-foreground", 
                isPositive && "text-[#7CFC00] dark:text-[#32CD32]", 
                isNegative && "text-[#FF2C2C] dark:text-[#FF4040]")}>
                {change}
            </p>
        )}
      </CardContent>
    </Card>
  );
}
