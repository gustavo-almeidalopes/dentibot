'use server';
/**
 * @fileOverview AI-powered mock chart data generator.
 *
 * - generateRealisticChartData - A function that generates realistic-looking mock chart data.
 * - GenerateRealisticChartDataInput - The input type for the generateRealisticChartData function.
 * - GenerateRealisticChartDataOutput - The return type for the generateRealisticChartData function.
 */

import {ai} from '@/ai/genkit';
import {z} from 'genkit';

const GenerateRealisticChartDataInputSchema = z.object({
  chartType: z.string().describe('The type of chart to generate data for (e.g., line, bar, pie).'),
  dataType: z.string().describe('The type of data the chart represents (e.g., sales, expenses, website traffic).'),
  timePeriod: z.string().describe('The time period the chart covers (e.g., monthly, quarterly, yearly).'),
  numDataPoints: z.number().describe('The number of data points to generate for the chart.'),
});
export type GenerateRealisticChartDataInput = z.infer<typeof GenerateRealisticChartDataInputSchema>;

const GenerateRealisticChartDataOutputSchema = z.object({
  chartData: z.string().describe('A JSON string representing the chart data, with realistic-looking values.'),
});
export type GenerateRealisticChartDataOutput = z.infer<typeof GenerateRealisticChartDataOutputSchema>;

export async function generateRealisticChartData(input: GenerateRealisticChartDataInput): Promise<GenerateRealisticChartDataOutput> {
  return generateRealisticChartDataFlow(input);
}

const prompt = ai.definePrompt({
  name: 'generateRealisticChartDataPrompt',
  input: {schema: GenerateRealisticChartDataInputSchema},
  output: {schema: GenerateRealisticChartDataOutputSchema},
  prompt: `You are an expert at generating realistic-looking mock data for charts and dashboards.

  I need you to generate mock data for a chart with the following characteristics:

  Chart Type: {{{chartType}}}
  Data Type: {{{dataType}}}
  Time Period: {{{timePeriod}}}
  Number of Data Points: {{{numDataPoints}}}

  The data should be returned as a JSON string that can be directly used to render the chart.

  Ensure the data looks realistic and reflects the typical patterns for the specified data type and time period.
  For example, if the data type is sales and the time period is monthly, the data should show some seasonality.
  If the chart type is a pie chart, return data appropriate for a pie chart.
  If the chart type is a line or bar chart, provide both labels and values.
`,
});

const generateRealisticChartDataFlow = ai.defineFlow(
  {
    name: 'generateRealisticChartDataFlow',
    inputSchema: GenerateRealisticChartDataInputSchema,
    outputSchema: GenerateRealisticChartDataOutputSchema,
  },
  async input => {
    const {output} = await prompt(input);
    return output!;
  }
);
