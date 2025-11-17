'use client'

import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from "recharts"
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card"
import { ChartContainer, ChartTooltip, ChartTooltipContent } from "@/components/ui/chart"
import type { ChartConfig } from "@/components/ui/chart"

const chartData = [
  { month: "Janeiro", revenue: 18600, expenses: 8000 },
  { month: "Fevereiro", revenue: 30500, expenses: 12000 },
  { month: "Março", revenue: 23700, expenses: 9800 },
  { month: "Abril", revenue: 27300, expenses: 11000 },
  { month: "Maio", revenue: 20900, expenses: 9500 },
  { month: "Junho", revenue: 21400, expenses: 10200 },
]

const chartConfig = {
  revenue: {
    label: "Receita",
    color: "hsl(var(--chart-1))",
  },
  expenses: {
    label: "Despesas",
    color: "hsl(var(--chart-2))",
  },
} satisfies ChartConfig

export function PaymentsWidget() {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Resumo Financeiro</CardTitle>
        <CardDescription>Receita vs. Despesas dos Últimos 6 Meses</CardDescription>
      </CardHeader>
      <CardContent>
        <ChartContainer config={chartConfig} className="min-h-[200px] w-full">
          <BarChart accessibilityLayer data={chartData}>
            <CartesianGrid vertical={false} />
            <XAxis
              dataKey="month"
              tickLine={false}
              tickMargin={10}
              axisLine={false}
              tickFormatter={(value) => value.slice(0, 3)}
            />
             <YAxis
              tickFormatter={(value) => `R$${Number(value) / 1000}k`}
            />
            <ChartTooltip content={<ChartTooltipContent />} />
            <Bar dataKey="revenue" fill="var(--color-revenue)" radius={4} />
            <Bar dataKey="expenses" fill="var(--color-expenses)" radius={4} />
          </BarChart>
        </ChartContainer>
      </CardContent>
    </Card>
  )
}
