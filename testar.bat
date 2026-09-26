@echo off
chcp 65001 >nul
setlocal
title DENTIBOT - TESTES
color 0F
cd /d "%~dp0"

set "API=api"

rem O mesmo "Verificar" do README, parando na primeira falha.
echo === API  %API%  (Testcontainers: precisa do Docker) ===
pushd "%API%"
call .\mvnw.cmd -B verify
if errorlevel 1 goto :falha
popd

echo.
echo === WEB ===
pushd web
if not exist node_modules (
  call npm ci
  if errorlevel 1 goto :falha
)
call npm run lint
if errorlevel 1 goto :falha
call npm test
if errorlevel 1 goto :falha
call npm run build
if errorlevel 1 goto :falha
popd

echo.
echo === APP ===
pushd app
if not exist node_modules (
  call npm ci
  if errorlevel 1 goto :falha
)
call npm run typecheck
if errorlevel 1 goto :falha
call npm test
if errorlevel 1 goto :falha
popd

echo.
echo TUDO VERDE.
pause
exit /b 0

:falha
color 0C
echo.
echo FALHOU. A SAÍDA ACIMA DIZ ONDE.
pause
exit /b 1
