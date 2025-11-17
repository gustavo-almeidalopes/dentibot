import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"

const inventory = [
  { name: "Espelhos Dentários", stock: 15, status: "Estoque Baixo" },
  { name: "Sondas", stock: 48, status: "Em Estoque" },
  { name: "Anestésico", stock: 8, status: "Estoque Baixo" },
  { name: "Gaze", stock: 150, status: "Em Estoque" },
  { name: "Luvas (Caixa)", stock: 3, status: "Crítico" },
  { name: "Cureta", stock: 32, status: "Em Estoque" },
]

export function InventoryWidget() {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Status do Estoque</CardTitle>
        <CardDescription>Visão em tempo real dos seus níveis de suprimentos.</CardDescription>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Item</TableHead>
              <TableHead className="text-right">Estoque</TableHead>
              <TableHead className="text-right">Status</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {inventory.map((item) => (
              <TableRow key={item.name}>
                <TableCell className="font-medium">{item.name}</TableCell>
                <TableCell className="text-right">{item.stock}</TableCell>
                <TableCell className="text-right">
                    <Badge variant={
                        item.status === 'Em Estoque' ? 'secondary' : item.status === 'Estoque Baixo' ? 'default' : 'destructive'
                    } className={cn(
                        item.status === 'Em Estoque' && 'bg-[#7CFC00]/20 dark:bg-[#32CD32]/20 text-[#7CFC00] dark:text-[#32CD32]',
                        item.status === 'Estoque Baixo' && 'bg-[#FFDE21]/20 dark:bg-[#FFD700]/20 text-[#FFDE21] dark:text-[#FFD700]',
                        item.status === 'Crítico' && 'bg-[#FF2C2C]/20 dark:bg-[#FF4040]/20 text-[#FF2C2C] dark:text-[#FF4040]',
                    )}>
                        {item.status}
                    </Badge>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  )
}
