@echo off
chcp 65001 >nul
setlocal
title DENTIBOT
color 0F
cd /d "%~dp0"

rem Infra, migrations e o invariante de RLS. Argumentos seguem adiante:
rem   iniciar.bat -Recriar   apaga os volumes e começa do zero
powershell -NoProfile -ExecutionPolicy Bypass -File "infrastructure\init.ps1" %*
if errorlevel 1 goto :falha

rem Cada peça na sua janela: fechar a janela para a peça.
start "DENTIBOT - API" cmd /k call "%~dp0api.bat"
start "DENTIBOT - WEB" cmd /k call "%~dp0web.bat"

echo.
echo API  http://localhost:8080
echo WEB  http://localhost:5173
echo APP  app.bat, à parte: o Expo pede a tecla do aparelho
echo.
pause
exit /b 0

:falha
color 0C
echo.
echo A INFRAESTRUTURA NÃO SUBIU. NADA FOI INICIADO.
pause
exit /b 1
