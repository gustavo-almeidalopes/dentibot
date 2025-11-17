'use client';

import { useFormState, useFormStatus } from 'react-dom';
import { generateChartAction } from '@/app/actions';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Wand2, Loader2, ServerCrash } from 'lucide-react';
import { Bar, BarChart, CartesianGrid, Legend, Line, LineChart, Pie, PieChart, Tooltip, XAxis, YAxis } from "recharts";
import { ChartContainer, ChartTooltip, ChartTooltipContent } from '@/components/ui/chart';
import { useEffect, useState } from 'react';
import { useToast } from '@/hooks/use-toast';

function SubmitButton() {
    const { pending } = useFormStatus();
    return (
        <Button type="submit" disabled={pending} className="w-full">
            {pending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Wand2 className="mr-2 h-4 w-4" />}
            Gerar Gráfico
        </Button>
    );
}

const initialState = {
    data: null,
    error: null,
};

export function ChartGenerator() {
    const [state, formAction] = useFormState(generateChartAction, initialState);
    const [chartType, setChartType] = useState('bar');
    const { toast } = useToast();

    useEffect(() => {
        if (state.error) {
            toast({
                variant: 'destructive',
                title: 'Falha na Geração do Gráfico',
                description: state.error,
            });
        }
    }, [state.error, toast]);

    const renderChart = () => {
        if (!state.data) return null;

        const chartConfig = Object.keys(state.data[0] || {}).reduce((acc, key) => {
            if (key !== 'label' && typeof state.data[0][key] === 'number') {
                acc[key] = { label: key.charAt(0).toUpperCase() + key.slice(1), color: `hsl(var(--chart-${Object.keys(acc).length + 1}))` };
            }
            return acc;
        }, {} as any);

        const keys = Object.keys(chartConfig);

        switch (chartType) {
            case 'pie':
                return (
                    <ChartContainer config={chartConfig} className="min-h-[200px] w-full aspect-square">
                        <PieChart>
                            <Tooltip content={<ChartTooltipContent nameKey="label" />} />
                            <Pie data={state.data} dataKey={keys[0]} nameKey="label" label />
                        </PieChart>
                    </ChartContainer>
                );
            case 'line':
                return (
                    <ChartContainer config={chartConfig} className="min-h-[200px] w-full">
                        <LineChart data={state.data}>
                            <CartesianGrid vertical={false} />
                            <XAxis dataKey="label" tickLine={false} axisLine={false} tickMargin={8} />
                            <Tooltip content={<ChartTooltipContent />} />
                            <Legend />
                            {keys.map(key => <Line key={key} dataKey={key} type="monotone" stroke={`var(--color-${key})`} strokeWidth={2} dot={false} />)}
                        </LineChart>
                    </ChartContainer>
                );
            case 'bar':
            default:
                return (
                    <ChartContainer config={chartConfig} className="min-h-[200px] w-full">
                        <BarChart data={state.data}>
                            <CartesianGrid vertical={false} />
                            <XAxis dataKey="label" tickLine={false} axisLine={false} tickMargin={8} />
                            <Tooltip content={<ChartTooltipContent />} />
                            <Legend />
                            {keys.map(key => <Bar key={key} dataKey={key} fill={`var(--color-${key})`} radius={4} />)}
                        </BarChart>
                    </ChartContainer>
                );
        }
    }


    return (
        <div className="grid lg:grid-cols-3 gap-6">
            <Card className="lg:col-span-1 h-fit">
                <form action={formAction}>
                    <CardHeader>
                        <CardTitle className="flex items-center gap-2">
                            <Wand2 className="text-primary" />
                            Gerador de Gráficos com IA
                        </CardTitle>
                        <CardDescription>Crie gráficos de simulação realistas com IA. Descreva o que você quer visualizar.</CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <div className="space-y-2">
                            <Label htmlFor="chartType">Tipo de Gráfico</Label>
                            <Select name="chartType" value={chartType} onValueChange={setChartType}>
                                <SelectTrigger id="chartType">
                                    <SelectValue placeholder="Selecione o tipo" />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="bar">Gráfico de Barras</SelectItem>
                                    <SelectItem value="line">Gráfico de Linha</SelectItem>
                                    <SelectItem value="pie">Gráfico de Pizza</SelectItem>
                                </SelectContent>
                            </Select>
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="dataType">Tipo de Dados</Label>
                            <Input id="dataType" name="dataType" placeholder="Ex: Visitas mensais de pacientes" required />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="timePeriod">Período de Tempo</Label>
                            <Input id="timePeriod" name="timePeriod" placeholder="Ex: Últimos 6 meses" required />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="numDataPoints">Pontos de Dados</Label>
                            <Input id="numDataPoints" name="numDataPoints" type="number" defaultValue="6" min="2" max="20" required />
                        </div>
                    </CardContent>
                    <CardFooter>
                        <SubmitButton />
                    </CardFooter>
                </form>
            </Card>

            <Card className="lg:col-span-2 min-h-[480px]">
                 <CardHeader>
                    <CardTitle>Gráfico Gerado</CardTitle>
                    <CardDescription>Este é o gráfico gerado pela IA com base em suas entradas.</CardDescription>
                </CardHeader>
                <CardContent className="flex items-center justify-center h-full">
                    {state.data ? (
                        renderChart()
                    ) : (
                        <div className="text-center text-muted-foreground">
                            <Wand2 className="mx-auto h-12 w-12 mb-4" />
                            <p>Seu gráfico gerado aparecerá aqui.</p>
                        </div>
                    )}
                    {state.error && !state.data && (
                         <div className="text-center text-destructive">
                            <ServerCrash className="mx-auto h-12 w-12 mb-4" />
                            <p>Algo deu errado. Por favor, tente novamente.</p>
                        </div>
                    )}
                </CardContent>
            </Card>
        </div>
    );
}
