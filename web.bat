@echo off
chcp 65001 >nul
setlocal
title DENTIBOT - WEB
color 0F
cd /d "%~dp0web"

if not exist node_modules (
  call npm ci
  if errorlevel 1 goto :falha
)
echo DENTIBOT WEB  http://localhost:5173
echo.
call npm run dev
if errorlevel 1 goto :falha
exit /b 0

:falha
color 0C
echo.
echo O WEB PAROU COM ERRO.
pause
exit /b 1
