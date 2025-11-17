'use server';

import { generateRealisticChartData, type GenerateRealisticChartDataInput } from "@/ai/flows/generate-realistic-chart-data";

export type ChartGeneratorState = {
    data: any | null;
    error: string | null;
};

export async function generateChartAction(
    _prevState: ChartGeneratorState,
    formData: FormData
): Promise<ChartGeneratorState> {
    const input: GenerateRealisticChartDataInput = {
        chartType: formData.get('chartType') as string,
        dataType: formData.get('dataType') as string,
        timePeriod: formData.get('timePeriod') as string,
        numDataPoints: Number(formData.get('numDataPoints')),
    };

    try {
        const result = await generateRealisticChartData(input);
        const parsedData = JSON.parse(result.chartData);
        return { data: parsedData, error: null };
    } catch (e: any) {
        console.error(e);
        return { data: null, error: e.message || "Failed to generate chart data." };
    }
}
