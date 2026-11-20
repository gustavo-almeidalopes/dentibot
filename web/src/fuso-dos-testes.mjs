/**
 * Carregado antes dos testes (`node --import`). As horas esperadas são as de
 * Brasília; o runner do CI roda em UTC. Precisa vir antes dos módulos, porque
 * os `Intl.DateTimeFormat` guardam o fuso quando são criados.
 */
import process from 'node:process';

process.env.TZ = 'America/Sao_Paulo';
