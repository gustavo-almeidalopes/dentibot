@echo off
chcp 65001 >nul
setlocal
title DENTIBOT - APP
color 0F
cd /d "%~dp0app"

if not exist node_modules (
  call npm ci
  if errorlevel 1 goto :falha
)
echo DENTIBOT APP  depois do QR: a = Android, i = iOS
echo.
call npm start
if errorlevel 1 goto :falha
exit /b 0

:falha
color 0C
echo.
echo O APP PAROU COM ERRO.
pause
exit /b 1
