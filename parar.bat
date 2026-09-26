@echo off
chcp 65001 >nul
setlocal
title DENTIBOT - PARAR
color 0F
cd /d "%~dp0"

rem down sem -v: os volumes ficam. Para apagar tudo: iniciar.bat -Recriar
docker compose -f infrastructure\docker-compose.yml down
if errorlevel 1 goto :falha

echo.
echo INFRA PARADA. VOLUMES PRESERVADOS.
echo API e web param ao fechar as janelas deles.
pause
exit /b 0

:falha
color 0C
echo.
echo O DOCKER NÃO RESPONDEU. O Docker Desktop está aberto?
pause
exit /b 1
